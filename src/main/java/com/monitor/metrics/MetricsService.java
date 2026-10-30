package com.monitor.metrics;

import com.monitor.dto.MetricsSummary;
import com.monitor.dto.ServiceMetrics;
import com.monitor.model.LogEntry;
import com.monitor.model.LogStatus;
import com.monitor.proxy.ServiceRegistry;
import com.monitor.store.LogStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Calcula las metricas agregadas a partir de los logs que dejo el LoggingProxy. */
@Service
public class MetricsService {

    private final LogStore logStore;
    private final ServiceRegistry registry;
    private final double errorRateThreshold;

    public MetricsService(LogStore logStore,
                          ServiceRegistry registry,
                          @Value("${monitor.error-rate-threshold:15}") double errorRateThreshold) {
        this.logStore = logStore;
        this.registry = registry;
        this.errorRateThreshold = errorRateThreshold;
    }

    public MetricsSummary summary() {
        List<LogEntry> logs = logStore.snapshot();
        Map<String, List<LogEntry>> byService = logs.stream()
                .collect(Collectors.groupingBy(LogEntry::serviceId));

        List<ServiceMetrics> services = new ArrayList<>();
        for (String serviceId : registry.ids()) {
            services.add(compute(serviceId, byService.getOrDefault(serviceId, List.of())));
        }

        ServiceMetrics overall = compute("all", logs);
        return new MetricsSummary(
                overall.totalCalls(),
                overall.errorCount(),
                overall.errorRate(),
                overall.successRate(),
                overall.avgResponseTimeMs(),
                errorRateThreshold,
                services);
    }

    private ServiceMetrics compute(String serviceId, List<LogEntry> entries) {
        long total = entries.size();
        long errors = entries.stream().filter(e -> e.status() == LogStatus.ERROR).count();

        double errorRate = total == 0 ? 0 : round2(errors * 100.0 / total);
        double successRate = total == 0 ? 0 : round2((total - errors) * 100.0 / total);
        double avg = total == 0 ? 0
                : round2(entries.stream().mapToLong(LogEntry::durationMs).average().orElse(0));

        return new ServiceMetrics(serviceId, total, errors, errorRate, successRate, avg,
                errorRate > errorRateThreshold);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
