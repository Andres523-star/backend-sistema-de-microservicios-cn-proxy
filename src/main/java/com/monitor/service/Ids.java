package com.monitor.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Utilidades pequenas compartidas por los servicios. */
final class Ids {

    private Ids() {
    }

    /** Identificador corto legible, p. ej. "9F3A21BC". */
    static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Mapa sincronizado que descarta las entradas mas antiguas al superar {@code maxEntries}. */
    static <K, V> Map<K, V> boundedMap(int maxEntries) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        });
    }
}
