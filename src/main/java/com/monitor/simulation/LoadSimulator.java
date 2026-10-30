package com.monitor.simulation;

import com.monitor.dto.SimulationResult;
import com.monitor.proxy.ServiceRegistry;
import com.monitor.service.InventoryService;
import com.monitor.service.ServiceIds;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Genera trafico sintetico: 50 llamadas aleatorias repartidas entre los 3 servicios.
 * Todas pasan por los LoggingProxy (via ServiceRegistry), asi que quedan registradas
 * exactamente igual que las llamadas reales. Se ejecutan en paralelo para responder rapido.
 */
@Component
public class LoadSimulator {

    public static final int TOTAL_CALLS = 50;
    private static final int THREADS = 8;

    private record PlannedCall(String serviceId, String operation, Object[] params) {
    }

    private final ServiceRegistry registry;

    public LoadSimulator(ServiceRegistry registry) {
        this.registry = registry;
    }

    public SimulationResult run() {
        List<PlannedCall> plan = new ArrayList<>();
        Map<String, Integer> callsByService = new LinkedHashMap<>();
        for (String id : registry.ids()) {
            callsByService.put(id, 0);
        }
        for (int i = 0; i < TOTAL_CALLS; i++) {
            PlannedCall call = randomCall();
            plan.add(call);
            callsByService.merge(call.serviceId(), 1, Integer::sum);
        }

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (PlannedCall call : plan) {
            tasks.add(() -> invoke(call));
        }

        int success = 0;
        int errors = 0;
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            for (Future<Boolean> future : pool.invokeAll(tasks)) {
                if (future.get()) {
                    success++;
                } else {
                    errors++;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Load simulation interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Load simulation failed", e.getCause());
        } finally {
            pool.shutdown();
        }
        return new SimulationResult(TOTAL_CALLS, success, errors, callsByService);
    }

    /** true = la llamada termino bien, false = el servicio lanzo excepcion (ya registrada por el proxy). */
    private Boolean invoke(PlannedCall call) {
        try {
            registry.get(call.serviceId()).execute(call.operation(), call.params());
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private PlannedCall randomCall() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return switch (r.nextInt(3)) {
            case 0 -> inventoryCall(r);
            case 1 -> orderCall(r);
            default -> paymentCall(r);
        };
    }

    private PlannedCall inventoryCall(ThreadLocalRandom r) {
        String sku = InventoryService.skuFor(r.nextInt(1, InventoryService.SKU_COUNT + 1));
        int roll = r.nextInt(7);
        if (roll < 4) {
            return new PlannedCall(ServiceIds.INVENTORY, "checkStock", new Object[]{sku});
        }
        if (roll < 6) {
            return new PlannedCall(ServiceIds.INVENTORY, "reserveStock", new Object[]{sku, r.nextInt(1, 4)});
        }
        return new PlannedCall(ServiceIds.INVENTORY, "restock", new Object[]{sku, r.nextInt(5, 21)});
    }

    private PlannedCall orderCall(ThreadLocalRandom r) {
        if (r.nextInt(4) == 3) {
            return new PlannedCall(ServiceIds.ORDERS, "listOrders", new Object[0]);
        }
        String customerId = "CUST-" + r.nextInt(100, 1000);
        String sku = InventoryService.skuFor(r.nextInt(1, InventoryService.SKU_COUNT + 1));
        return new PlannedCall(ServiceIds.ORDERS, "createOrder",
                new Object[]{customerId, sku, r.nextInt(1, 6)});
    }

    private PlannedCall paymentCall(ThreadLocalRandom r) {
        String orderId = "ORD-SIM-" + r.nextInt(1000, 10000);
        double amount = Math.round(r.nextDouble(5, 500) * 100) / 100.0;
        return new PlannedCall(ServiceIds.PAYMENTS, "processPayment", new Object[]{orderId, amount});
    }
}
