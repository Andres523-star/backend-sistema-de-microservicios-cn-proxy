package com.monitor.web;

import com.monitor.dto.ExecutionRequest;
import com.monitor.dto.ExecutionResponse;
import com.monitor.proxy.LoggingProxy;
import com.monitor.proxy.MicroserviceProxy;
import com.monitor.proxy.ServiceRegistry;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de entrada unico para los tres servicios:
 * <pre>
 * POST /api/services/inventory/{operation}
 * POST /api/services/orders/{operation}
 * POST /api/services/payments/{operation}
 * </pre>
 * El controlador solo conoce {@link MicroserviceProxy}; el registro le entrega
 * siempre el LoggingProxy, por lo que toda llamada queda registrada.
 * Cuerpo opcional: {@code {"params": ["SKU-001", 2]}}
 */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRegistry registry;

    public ServiceController(ServiceRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/{service}/{operation}")
    public ExecutionResponse execute(@PathVariable String service,
                                     @PathVariable String operation,
                                     @RequestBody(required = false) ExecutionRequest body) {
        MicroserviceProxy<Object> proxy = registry.get(service);
        Object[] params = (body == null || body.getParams() == null)
                ? new Object[0]
                : body.getParams().toArray();

        Object result = proxy.execute(operation, params);

        return new ExecutionResponse(MDC.get(LoggingProxy.REQUEST_ID_KEY), service, operation, "SUCCESS", result);
    }
}
