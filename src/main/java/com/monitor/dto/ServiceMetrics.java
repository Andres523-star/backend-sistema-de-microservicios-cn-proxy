package com.monitor.dto;

/**
 * Metricas de un servicio. Las tasas estan en porcentaje (0-100).
 * El tiempo promedio incluye llamadas exitosas y fallidas.
 */
public record ServiceMetrics(
        String serviceId,
        long totalCalls,
        long errorCount,
        double errorRate,
        double successRate,
        double avgResponseTimeMs,
        boolean highErrorRate) {
}
