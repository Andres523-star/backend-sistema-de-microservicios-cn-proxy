package com.monitor.exception;

/** Servicio, operacion o recurso inexistente (HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
