package com.monitor.dto;

import java.util.List;

public record MetricsSummary(
        long totalCalls,
        long errorCount,
        double errorRate,
        double successRate,
        double avgResponseTimeMs,
        double errorRateThreshold,
        List<ServiceMetrics> services) {
}
