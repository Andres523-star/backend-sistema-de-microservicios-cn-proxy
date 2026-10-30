package com.monitor.dto;

public record ExecutionResponse(
        String requestId,
        String serviceId,
        String operation,
        String status,
        Object result) {
}
