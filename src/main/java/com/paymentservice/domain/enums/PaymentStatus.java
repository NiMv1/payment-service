package com.paymentservice.domain.enums;

/**
 * Статусы платежа в системе.
 */
public enum PaymentStatus {
    
    /** Платёж создан, ожидает обработки */
    PENDING,
    
    /** Платёж обрабатывается */
    PROCESSING,
    
    /** Платёж успешно завершён */
    COMPLETED,
    
    /** Платёж отклонён */
    DECLINED,
    
    /** Платёж отменён пользователем */
    CANCELLED,
    
    /** Возврат средств */
    REFUNDED,
    
    /** Частичный возврат */
    PARTIALLY_REFUNDED,
    
    /** Ошибка при обработке */
    FAILED,
    
    /** Истёк срок ожидания */
    EXPIRED
}
