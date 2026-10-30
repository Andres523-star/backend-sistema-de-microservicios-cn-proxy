package com.monitor.exception;

/** Fallo (simulado) de la pasarela de pagos (HTTP 502). */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }
}
