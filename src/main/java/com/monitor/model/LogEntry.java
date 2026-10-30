package com.monitor.model;

import java.time.Instant;
import java.util.List;

/**
 * Registro de una llamada a un microservicio, creado por el LoggingProxy.
 *
 * @param requestId    UUID unico de la llamada
 * @param serviceId    inventory | orders | payments
 * @param operation    operacion invocada (checkStock, createOrder, ...)
 * @param durationMs   duracion de la ejecucion real del servicio
 * @param status       SUCCESS o ERROR
 * @param timestamp    momento en que termino la llamada y se registro
 * @param params       parametros de entrada recibidos
 * @param response     respuesta completa (null si hubo error)
 * @param errorMessage "Clase: mensaje" de la excepcion (null si SUCCESS)
 * @param errorTrace   stack trace resumido (null si SUCCESS)
 */
public record LogEntry(
        String requestId,
        String serviceId,
        String operation,
        long durationMs,
        LogStatus status,
        Instant timestamp,
        List<Object> params,
        Object response,
        String errorMessage,
        String errorTrace) {
}
