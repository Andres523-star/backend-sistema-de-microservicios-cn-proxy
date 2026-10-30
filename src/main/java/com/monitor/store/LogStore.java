package com.monitor.store;

import com.monitor.model.LogEntry;
import com.monitor.model.LogStatus;
import com.monitor.model.PagedResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Almacen de logs en memoria, seguro para hilos y acotado: al superar
 * {@code monitor.log.max-entries} se descartan los registros mas antiguos.
 * Internamente esta en orden cronologico (el mas nuevo al final).
 */
@Component
public class LogStore {

    private final int maxEntries;
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LogStore(@Value("${monitor.log.max-entries:5000}") int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    public void add(LogEntry entry) {
        lock.writeLock().lock();
        try {
            entries.addLast(entry);
            while (entries.size() > maxEntries) {
                entries.removeFirst();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Copia cronologica (antiguo -> nuevo) de todos los logs. */
    public List<LogEntry> snapshot() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(entries);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Logs filtrados, del mas nuevo al mas antiguo, con paginacion (page empieza en 0). */
    public PagedResponse<LogEntry> query(String serviceId, LogStatus status, Instant from, Instant to,
                                         int page, int size) {
        long skip = (long) page * size;
        long total = 0;
        List<LogEntry> content = new ArrayList<>();

        lock.readLock().lock();
        try {
            Iterator<LogEntry> it = entries.descendingIterator();
            while (it.hasNext()) {
                LogEntry e = it.next();
                if (!matches(e, serviceId, status, from, to)) {
                    continue;
                }
                if (total >= skip && content.size() < size) {
                    content.add(e);
                }
                total++;
            }
        } finally {
            lock.readLock().unlock();
        }

        int totalPages = (int) Math.ceil(total / (double) size);
        return new PagedResponse<>(content, page, size, total, totalPages);
    }

    /** Ultimas {@code limit} llamadas de un servicio, en orden cronologico (para graficas). */
    public List<LogEntry> recent(String serviceId, int limit) {
        List<LogEntry> result = new ArrayList<>();
        lock.readLock().lock();
        try {
            Iterator<LogEntry> it = entries.descendingIterator();
            while (it.hasNext() && result.size() < limit) {
                LogEntry e = it.next();
                if (serviceId.equals(e.serviceId())) {
                    result.add(e);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        Collections.reverse(result);
        return result;
    }

    private static boolean matches(LogEntry e, String serviceId, LogStatus status, Instant from, Instant to) {
        if (serviceId != null && !serviceId.equalsIgnoreCase(e.serviceId())) {
            return false;
        }
        if (status != null && e.status() != status) {
            return false;
        }
        if (from != null && e.timestamp().isBefore(from)) {
            return false;
        }
        return to == null || !e.timestamp().isAfter(to);
    }
}
