package com.paymentservice.exception;

/**
 * Исключение при недостаточности средств.
 */
public class InsufficientFundsException extends PaymentException {

    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }
}
