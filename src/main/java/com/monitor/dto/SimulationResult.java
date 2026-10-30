package com.monitor.dto;

import java.util.Map;

public record SimulationResult(
        int generated,
        int success,
        int errors,
        Map<String, Integer> callsByService) {
}
