package com.paymentservice.exception;

/**
 * Исключение при дублировании платежа (нарушение идемпотентности).
 */
public class DuplicatePaymentException extends PaymentException {

    public DuplicatePaymentException(String message) {
        super("DUPLICATE_PAYMENT", message);
    }
}
