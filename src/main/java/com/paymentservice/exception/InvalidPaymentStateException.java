package com.paymentservice.exception;

/**
 * Исключение при некорректном состоянии платежа для операции.
 */
public class InvalidPaymentStateException extends PaymentException {

    public InvalidPaymentStateException(String message) {
        super("INVALID_PAYMENT_STATE", message);
    }
}
