package com.paymentservice.domain.entity;

import com.paymentservice.domain.enums.Currency;
import com.paymentservice.domain.enums.PaymentStatus;
import com.paymentservice.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность транзакции.
 * Каждый платёж может иметь несколько транзакций (оплата, возврат, частичный возврат).
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_payment_id", columnList = "payment_id"),
    @Index(name = "idx_transaction_external_id", columnList = "externalId"),
    @Index(name = "idx_transaction_type", columnList = "type"),
    @Index(name = "idx_transaction_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Связанный платёж */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /** Тип транзакции */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /** Сумма транзакции */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Валюта */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** Статус транзакции */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** ID транзакции во внешней платёжной системе */
    @Column(length = 100)
    private String externalId;

    /** Код авторизации */
    @Column(length = 50)
    private String authorizationCode;

    /** Код ошибки */
    @Column(length = 50)
    private String errorCode;

    /** Сообщение об ошибке */
    @Column(length = 500)
    private String errorMessage;

    /** Сырой ответ от платёжной системы (JSON) */
    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    /** Время создания */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Время обработки */
    private LocalDateTime processedAt;
}
