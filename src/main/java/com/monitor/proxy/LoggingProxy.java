package com.monitor.proxy;

import com.monitor.model.LogEntry;
import com.monitor.model.LogStatus;
import com.monitor.store.LogStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Proxy de logging: envuelve cualquier {@link MicroserviceProxy} y
 * <ol>
 *   <li>registra el inicio de la llamada,</li>
 *   <li>delega en el servicio real,</li>
 *   <li>registra duracion + resultado (o el error con stack trace resumido),</li>
 *   <li>guarda un {@link LogEntry} en el {@link LogStore}.</li>
 * </ol>
 * La excepcion original se relanza siempre: el proxy observa, no oculta fallos.
 */
public class LoggingProxy<T> implements MicroserviceProxy<T> {

    /** Clave MDC con el requestId (la puede fijar un filtro HTTP; si no existe, el proxy genera uno). */
    public static final String REQUEST_ID_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);
    private static final int MAX_TRACE_FRAMES = 5;

    private final String serviceId;
    private final MicroserviceProxy<T> delegate;
    private final LogStore logStore;

    public LoggingProxy(String serviceId, MicroserviceProxy<T> delegate, LogStore logStore) {
        this.serviceId = serviceId;
        this.delegate = delegate;
        this.logStore = logStore;
    }

    @Override
    public T execute(String operation, Object... params) {
        Object[] safeParams = params == null ? new Object[0] : params;
        List<Object> paramList = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(safeParams)));

        String inheritedId = MDC.get(REQUEST_ID_KEY);
        String requestId = inheritedId != null ? inheritedId : UUID.randomUUID().toString();
        if (inheritedId == null) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }

        log.info("START requestId={} serviceId={} operation={} params={}",
                requestId, serviceId, operation, paramList);
        long startNanos = System.nanoTime();

        try {
            T result = delegate.execute(operation, safeParams);
            long durationMs = elapsedMs(startNanos);

            logStore.add(new LogEntry(requestId, serviceId, operation, durationMs, LogStatus.SUCCESS,
                    Instant.now(), paramList, result, null, null));
            log.info("END   requestId={} serviceId={} operation={} durationMs={} status=SUCCESS",
                    requestId, serviceId, operation, durationMs);
            return result;

        } catch (RuntimeException ex) {
            long durationMs = elapsedMs(startNanos);
            String message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            String trace = summarizeStackTrace(ex);

            logStore.add(new LogEntry(requestId, serviceId, operation, durationMs, LogStatus.ERROR,
                    Instant.now(), paramList, null, message, trace));
            log.error("END   requestId={} serviceId={} operation={} durationMs={} status=ERROR error={}\n{}",
                    requestId, serviceId, operation, durationMs, message, trace);
            throw ex;

        } finally {
            if (inheritedId == null) {
                MDC.remove(REQUEST_ID_KEY);
            }
        }
    }

    private static long elapsedMs(long startNanos) {
        return Math.round((System.nanoTime() - startNanos) / 1_000_000.0);
    }

    /** Clase + mensaje + primeros frames + causa raiz (si existe). */
    static String summarizeStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder(ex.getClass().getName());
        if (ex.getMessage() != null) {
            sb.append(": ").append(ex.getMessage());
        }
        StackTraceElement[] frames = ex.getStackTrace();
        int shown = Math.min(MAX_TRACE_FRAMES, frames.length);
        for (int i = 0; i < shown; i++) {
            sb.append("\n    at ").append(frames[i]);
        }
        if (frames.length > shown) {
            sb.append("\n    ... ").append(frames.length - shown).append(" more");
        }
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root != ex) {
            sb.append("\nCaused by: ").append(root.getClass().getName());
            if (root.getMessage() != null) {
                sb.append(": ").append(root.getMessage());
            }
        }
        return sb.toString();
    }
}
