package com.monitor.service;

import com.monitor.exception.NotFoundException;
import com.monitor.proxy.MicroserviceProxy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Servicio real de pedidos.
 * <p>
 * Operaciones: {@code createOrder(customerId, sku, quantity)}, {@code getOrder(orderId)},
 * {@code cancelOrder(orderId)}, {@code listOrders()}.
 */
@Service
public class OrderService implements MicroserviceProxy<Object> {

    private static final int MAX_ORDERS = 10_000;
    private static final int LATEST_ORDERS_IN_LIST = 5;

    public record Order(String orderId, String customerId, String sku, int quantity,
                        BigDecimal total, String status, Instant createdAt) {
    }

    private final Map<String, Order> orders = Ids.boundedMap(MAX_ORDERS);
    private final LatencySimulator latency;

    public OrderService(LatencySimulator latency) {
        this.latency = latency;
    }

    @Override
    public Object execute(String operation, Object... params) {
        latency.sleep(30, 120);
        String op = operation == null ? "" : operation;
        return switch (op) {
            case "createOrder" -> createOrder(params);
            case "getOrder" -> getOrder(params);
            case "cancelOrder" -> cancelOrder(params);
            case "listOrders" -> listOrders();
            default -> throw new NotFoundException(
                    "Unknown operation '" + op + "' for service '" + ServiceIds.ORDERS + "'");
        };
    }

    private Order createOrder(Object[] params) {
        String customerId = Params.requireString(params, 0, "customerId");
        String sku = Params.requireString(params, 1, "sku");
        int quantity = Params.optInt(params, 2, 1);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        // Precio unitario determinista a partir del SKU (entre 5 y 99)
        BigDecimal unitPrice = BigDecimal.valueOf(5 + Math.abs(sku.hashCode() % 95));
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        String orderId = "ORD-" + Ids.shortId();
        Order order = new Order(orderId, customerId, sku, quantity, total, "CREATED", Instant.now());
        orders.put(orderId, order);
        return order;
    }

    private Order getOrder(Object[] params) {
        String orderId = Params.requireString(params, 0, "orderId");
        Order order = orders.get(orderId);
        if (order == null) {
            throw new NotFoundException("Order '" + orderId + "' not found");
        }
        return order;
    }

    private Order cancelOrder(Object[] params) {
        String orderId = Params.requireString(params, 0, "orderId");
        Order updated = orders.computeIfPresent(orderId, (id, current) -> {
            if ("CANCELLED".equals(current.status())) {
                throw new IllegalStateException("Order '" + id + "' is already cancelled");
            }
            return new Order(current.orderId(), current.customerId(), current.sku(), current.quantity(),
                    current.total(), "CANCELLED", current.createdAt());
        });
        if (updated == null) {
            throw new NotFoundException("Order '" + orderId + "' not found");
        }
        return updated;
    }

    private Map<String, Object> listOrders() {
        List<Order> all;
        synchronized (orders) {
            all = new ArrayList<>(orders.values());
        }
        int from = Math.max(0, all.size() - LATEST_ORDERS_IN_LIST);
        List<Order> latest = new ArrayList<>(all.subList(from, all.size()));
        Collections.reverse(latest);
        return Map.of("total", all.size(), "latest", latest);
    }
}
