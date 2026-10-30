package com.monitor.service;

import com.monitor.exception.NotFoundException;
import com.monitor.proxy.MicroserviceProxy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio real de inventario ("RealSubject" del patron).
 * <p>
 * Operaciones: {@code checkStock(sku)}, {@code reserveStock(sku, quantity)},
 * {@code restock(sku, quantity)}. SKUs validos: SKU-001 ... SKU-010.
 */
@Service
public class InventoryService implements MicroserviceProxy<Object> {

    public static final int SKU_COUNT = 10;
    private static final int INITIAL_STOCK = 500;

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();
    private final LatencySimulator latency;

    public InventoryService(LatencySimulator latency) {
        this.latency = latency;
        for (int i = 1; i <= SKU_COUNT; i++) {
            stock.put(skuFor(i), INITIAL_STOCK);
        }
    }

    /** SKU con formato SKU-001 para n entre 1 y {@link #SKU_COUNT}. */
    public static String skuFor(int n) {
        return String.format("SKU-%03d", n);
    }

    @Override
    public Object execute(String operation, Object... params) {
        latency.sleep(15, 60);
        String op = operation == null ? "" : operation;
        return switch (op) {
            case "checkStock" -> checkStock(params);
            case "reserveStock" -> reserveStock(params);
            case "restock" -> restock(params);
            default -> throw new NotFoundException(
                    "Unknown operation '" + op + "' for service '" + ServiceIds.INVENTORY + "'");
        };
    }

    private Map<String, Object> checkStock(Object[] params) {
        String sku = Params.requireString(params, 0, "sku");
        Integer available = stock.get(sku);
        if (available == null) {
            throw new NotFoundException("Unknown product '" + sku + "'");
        }
        return Map.of("sku", sku, "available", available, "inStock", available > 0);
    }

    private Map<String, Object> reserveStock(Object[] params) {
        String sku = Params.requireString(params, 0, "sku");
        int quantity = Params.optInt(params, 1, 1);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        Integer remaining = stock.computeIfPresent(sku, (key, current) -> {
            if (current < quantity) {
                throw new IllegalStateException("Insufficient stock for '" + key
                        + "': requested " + quantity + ", available " + current);
            }
            return current - quantity;
        });
        if (remaining == null) {
            throw new NotFoundException("Unknown product '" + sku + "'");
        }
        return Map.of("sku", sku, "reserved", quantity, "remaining", remaining);
    }

    private Map<String, Object> restock(Object[] params) {
        String sku = Params.requireString(params, 0, "sku");
        int quantity = Params.optInt(params, 1, 1);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        Integer available = stock.computeIfPresent(sku, (key, current) -> current + quantity);
        if (available == null) {
            throw new NotFoundException("Unknown product '" + sku + "'");
        }
        return Map.of("sku", sku, "added", quantity, "available", available);
    }
}
