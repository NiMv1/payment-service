package com.paymentservice.domain.enums;

/**
 * Методы оплаты.
 */
public enum PaymentMethod {
    
    /** Банковская карта */
    CARD,
    
    /** Банковский перевод */
    BANK_TRANSFER,
    
    /** Электронный кошелёк */
    E_WALLET,
    
    /** Криптовалюта */
    CRYPTO,
    
    /** Система быстрых платежей */
    SBP,
    
    /** Apple Pay */
    APPLE_PAY,
    
    /** Google Pay */
    GOOGLE_PAY
}
