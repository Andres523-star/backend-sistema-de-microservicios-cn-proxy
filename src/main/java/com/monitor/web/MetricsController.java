package com.monitor.web;

import com.monitor.dto.MetricsSummary;
import com.monitor.dto.SimulationResult;
import com.monitor.metrics.MetricsService;
import com.monitor.model.LogEntry;
import com.monitor.model.LogStatus;
import com.monitor.model.PagedResponse;
import com.monitor.proxy.ServiceRegistry;
import com.monitor.simulation.LoadSimulator;
import com.monitor.store.LogStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_RECENT = 100;

    private final MetricsService metricsService;
    private final LogStore logStore;
    private final LoadSimulator loadSimulator;
    private final ServiceRegistry registry;

    public MetricsController(MetricsService metricsService,
                             LogStore logStore,
                             LoadSimulator loadSimulator,
                             ServiceRegistry registry) {
        this.metricsService = metricsService;
        this.logStore = logStore;
        this.loadSimulator = loadSimulator;
        this.registry = registry;
    }

    /** Total de llamadas, tasa de error y tiempo promedio (global y por servicio). */
    @GetMapping("/summary")
    public MetricsSummary summary() {
        return metricsService.summary();
    }

    /**
     * Logs filtrados (mas nuevos primero) con paginacion.
     * from / to en ISO-8601 UTC, p. ej. 2026-10-30T15:00:00Z. Todos los filtros son opcionales.
     */
    @GetMapping("/logs")
    public PagedResponse<LogEntry> logs(@RequestParam(required = false) String service,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String from,
                                        @RequestParam(required = false) String to,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return logStore.query(blankToNull(service), parseStatus(status),
                parseInstant("from", from), parseInstant("to", to), safePage, safeSize);
    }

    /** Ultimas N llamadas de cada servicio en orden cronologico (alimenta la grafica de lineas). */
    @GetMapping("/recent")
    public Map<String, List<LogEntry>> recent(@RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(1, limit), MAX_RECENT);
        Map<String, List<LogEntry>> result = new LinkedHashMap<>();
        for (String serviceId : registry.ids()) {
            result.put(serviceId, logStore.recent(serviceId, safeLimit));
        }
        return result;
    }

    /** Genera 50 llamadas aleatorias a los 3 servicios para poblar los logs. */
    @PostMapping("/simulate-load")
    public SimulationResult simulateLoad() {
        return loadSimulator.run();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LogStatus parseStatus(String value) {
        String clean = blankToNull(value);
        if (clean == null) {
            return null;
        }
        try {
            return LogStatus.valueOf(clean.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status '" + value + "'. Use SUCCESS or ERROR");
        }
    }

    private static Instant parseInstant(String name, String value) {
        String clean = blankToNull(value);
        if (clean == null) {
            return null;
        }
        try {
            return Instant.parse(clean);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid '" + name + "': expected ISO-8601 UTC such as 2026-10-30T15:00:00Z");
        }
    }
}
