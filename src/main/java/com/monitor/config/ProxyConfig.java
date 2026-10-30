package com.monitor.config;

import com.monitor.proxy.LoggingProxy;
import com.monitor.proxy.ServiceRegistry;
import com.monitor.service.InventoryService;
import com.monitor.service.OrderService;
import com.monitor.service.PaymentService;
import com.monitor.service.ServiceIds;
import com.monitor.store.LogStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aqui se "cablea" el patron: cada servicio real se envuelve en un LoggingProxy
 * y solo los proxies quedan expuestos al resto de la aplicacion.
 */
@Configuration
public class ProxyConfig {

    @Bean
    public ServiceRegistry serviceRegistry(InventoryService inventory,
                                           OrderService orders,
                                           PaymentService payments,
                                           LogStore logStore) {
        ServiceRegistry registry = new ServiceRegistry();
        registry.register(ServiceIds.INVENTORY, new LoggingProxy<>(ServiceIds.INVENTORY, inventory, logStore));
        registry.register(ServiceIds.ORDERS, new LoggingProxy<>(ServiceIds.ORDERS, orders, logStore));
        registry.register(ServiceIds.PAYMENTS, new LoggingProxy<>(ServiceIds.PAYMENTS, payments, logStore));
        return registry;
    }
}
