package com.monitor.service;

import com.monitor.exception.NotFoundException;
import com.monitor.exception.PaymentGatewayException;
import com.monitor.proxy.MicroserviceProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servicio real de pagos.
 * <p>
 * Operaciones: {@code processPayment(orderId, amount)}, {@code getPaymentStatus(paymentId)},
 * {@code refundPayment(paymentId)}.
 * <p>
 * Para poder demostrar el monitoreo, una fraccion de las llamadas
 * ({@code monitor.payment.failure-rate}, 10% por defecto) falla a proposito
 * lanzando {@link PaymentGatewayException}.
 */
@Service
public class PaymentService implements MicroserviceProxy<Object> {

    private static final int MAX_PAYMENTS = 10_000;

    public record Payment(String paymentId, String orderId, BigDecimal amount,
                          String status, Instant createdAt) {
    }

    private final Map<String, Payment> payments = Ids.boundedMap(MAX_PAYMENTS);
    private final LatencySimulator latency;
    private final double failureRate;

    public PaymentService(LatencySimulator latency,
                          @Value("${monitor.payment.failure-rate:0.10}") double failureRate) {
        this.latency = latency;
        this.failureRate = failureRate;
    }

    @Override
    public Object execute(String operation, Object... params) {
        latency.sleep(60, 250);

        // Fallo intencional: afecta a cualquier llamada al servicio de pagos
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new PaymentGatewayException("Payment gateway timeout (simulated failure)");
        }

        String op = operation == null ? "" : operation;
        return switch (op) {
            case "processPayment" -> processPayment(params);
            case "getPaymentStatus" -> getPaymentStatus(params);
            case "refundPayment" -> refundPayment(params);
            default -> throw new NotFoundException(
                    "Unknown operation '" + op + "' for service '" + ServiceIds.PAYMENTS + "'");
        };
    }

    private Payment processPayment(Object[] params) {
        String orderId = Params.requireString(params, 0, "orderId");
        BigDecimal amount = Params.requireAmount(params, 1, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        String paymentId = "PAY-" + Ids.shortId();
        Payment payment = new Payment(paymentId, orderId, amount, "APPROVED", Instant.now());
        payments.put(paymentId, payment);
        return payment;
    }

    private Map<String, Object> getPaymentStatus(Object[] params) {
        String paymentId = Params.requireString(params, 0, "paymentId");
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new NotFoundException("Payment '" + paymentId + "' not found");
        }
        return Map.of("paymentId", payment.paymentId(), "orderId", payment.orderId(),
                "status", payment.status());
    }

    private Payment refundPayment(Object[] params) {
        String paymentId = Params.requireString(params, 0, "paymentId");
        Payment refunded = payments.computeIfPresent(paymentId, (id, current) -> {
            if ("REFUNDED".equals(current.status())) {
                throw new IllegalStateException("Payment '" + id + "' is already refunded");
            }
            return new Payment(current.paymentId(), current.orderId(), current.amount(),
                    "REFUNDED", current.createdAt());
        });
        if (refunded == null) {
            throw new NotFoundException("Payment '" + paymentId + "' not found");
        }
        return refunded;
    }
}
