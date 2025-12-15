package com.paymentservice.domain.enums;

/**
 * Типы транзакций.
 */
public enum TransactionType {
    
    /** Оплата */
    PAYMENT,
    
    /** Возврат */
    REFUND,
    
    /** Частичный возврат */
    PARTIAL_REFUND,
    
    /** Авторизация (холдирование) */
    AUTHORIZATION,
    
    /** Списание после авторизации */
    CAPTURE,
    
    /** Отмена авторизации */
    VOID
}
