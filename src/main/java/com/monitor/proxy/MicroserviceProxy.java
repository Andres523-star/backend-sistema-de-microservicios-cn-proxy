package com.monitor.proxy;

/**
 * "Subject" del patron Proxy.
 * <p>
 * Los servicios reales (InventoryService, OrderService, PaymentService) y el
 * {@link LoggingProxy} implementan esta misma interfaz, de modo que el cliente
 * no sabe si habla con el servicio real o con su proxy.
 *
 * @param <T> tipo de la respuesta
 */
public interface MicroserviceProxy<T> {

    T execute(String operation, Object... params);
}
