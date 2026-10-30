package com.monitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Introduce una latencia aleatoria para que los tiempos de respuesta sean realistas. */
@Component
public class LatencySimulator {

    private final boolean enabled;

    public LatencySimulator(@Value("${monitor.latency.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void sleep(int minMs, int maxMs) {
        if (!enabled) {
            return;
        }
        int upper = Math.max(minMs, maxMs) + 1;
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, upper));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating latency", e);
        }
    }
}
