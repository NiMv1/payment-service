package com.paymentservice.exception;

/**
 * Исключение когда платёж не найден.
 */
public class PaymentNotFoundException extends PaymentException {

    public PaymentNotFoundException(String message) {
        super("PAYMENT_NOT_FOUND", message);
    }
}
