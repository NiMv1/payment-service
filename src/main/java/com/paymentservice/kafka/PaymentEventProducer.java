package com.paymentservice.kafka;

import com.paymentservice.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Продюсер событий платежей в Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PAYMENT_EVENTS = "payment-events";

    /**
     * Отправить событие создания платежа.
     */
    public void sendPaymentCreated(Payment payment) {
        Map<String, Object> event = createBaseEvent(payment, "PAYMENT_CREATED");
        send(event, payment.getId().toString());
        log.info("Отправлено событие PAYMENT_CREATED: paymentId={}", payment.getId());
    }

    /**
     * Отправить событие успешного платежа.
     */
    public void sendPaymentCompleted(Payment payment) {
        Map<String, Object> event = createBaseEvent(payment, "PAYMENT_COMPLETED");
        event.put("externalTransactionId", payment.getExternalTransactionId());
        event.put("completedAt", payment.getCompletedAt().toString());
        send(event, payment.getId().toString());
        log.info("Отправлено событие PAYMENT_COMPLETED: paymentId={}", payment.getId());
    }

    /**
     * Отправить событие отмены платежа.
     */
    public void sendPaymentCancelled(Payment payment) {
        Map<String, Object> event = createBaseEvent(payment, "PAYMENT_CANCELLED");
        send(event, payment.getId().toString());
        log.info("Отправлено событие PAYMENT_CANCELLED: paymentId={}", payment.getId());
    }

    /**
     * Отправить событие возврата.
     */
    public void sendPaymentRefunded(Payment payment, BigDecimal refundAmount) {
        Map<String, Object> event = createBaseEvent(payment, "PAYMENT_REFUNDED");
        event.put("refundAmount", refundAmount.toString());
        event.put("totalRefunded", payment.getRefundedAmount().toString());
        send(event, payment.getId().toString());
        log.info("Отправлено событие PAYMENT_REFUNDED: paymentId={}, refundAmount={}", 
                payment.getId(), refundAmount);
    }

    /**
     * Отправить событие ошибки платежа.
     */
    public void sendPaymentFailed(Payment payment) {
        Map<String, Object> event = createBaseEvent(payment, "PAYMENT_FAILED");
        event.put("errorCode", payment.getErrorCode());
        event.put("errorMessage", payment.getErrorMessage());
        send(event, payment.getId().toString());
        log.info("Отправлено событие PAYMENT_FAILED: paymentId={}", payment.getId());
    }

    /**
     * Создать базовое событие.
     */
    private Map<String, Object> createBaseEvent(Payment payment, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("paymentId", payment.getId().toString());
        event.put("orderId", payment.getOrderId());
        event.put("userId", payment.getUserId());
        event.put("amount", payment.getAmount().toString());
        event.put("currency", payment.getCurrency().name());
        event.put("status", payment.getStatus().name());
        event.put("timestamp", LocalDateTime.now().toString());
        return event;
    }

    /**
     * Отправить событие в Kafka.
     */
    private void send(Map<String, Object> event, String key) {
        try {
            kafkaTemplate.send(TOPIC_PAYMENT_EVENTS, key, event);
        } catch (Exception e) {
            log.error("Ошибка отправки события в Kafka: {}", e.getMessage(), e);
        }
    }
}
