package com.monitor.proxy;

import com.monitor.exception.NotFoundException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Catalogo de servicios disponibles. Siempre contiene proxies, nunca servicios "desnudos". */
public class ServiceRegistry {

    private final Map<String, MicroserviceProxy<Object>> proxies = new LinkedHashMap<>();

    public void register(String serviceId, MicroserviceProxy<Object> proxy) {
        proxies.put(serviceId, proxy);
    }

    public MicroserviceProxy<Object> get(String serviceId) {
        MicroserviceProxy<Object> proxy = proxies.get(serviceId);
        if (proxy == null) {
            throw new NotFoundException("Unknown service '" + serviceId
                    + "'. Available: " + proxies.keySet());
        }
        return proxy;
    }

    /** Ids en orden de registro. */
    public Set<String> ids() {
        return Collections.unmodifiableSet(proxies.keySet());
    }
}
