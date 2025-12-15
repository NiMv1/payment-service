package com.paymentservice.domain.entity;

import com.paymentservice.domain.enums.Currency;
import com.paymentservice.domain.enums.PaymentMethod;
import com.paymentservice.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сущность платежа.
 * Основная бизнес-сущность системы.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_idempotency_key", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_payment_order_id", columnList = "orderId"),
    @Index(name = "idx_payment_user_id", columnList = "userId"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Ключ идемпотентности для предотвращения дублирования платежей */
    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    /** ID заказа во внешней системе */
    @Column(nullable = false, length = 64)
    private String orderId;

    /** ID пользователя */
    @Column(nullable = false, length = 64)
    private String userId;

    /** ID продавца/мерчанта */
    @Column(length = 64)
    private String merchantId;

    /** Сумма платежа */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Валюта */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** Метод оплаты */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** Статус платежа */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** Описание платежа */
    @Column(length = 500)
    private String description;

    /** ID транзакции во внешней платёжной системе */
    @Column(length = 100)
    private String externalTransactionId;

    /** Код ошибки (если есть) */
    @Column(length = 50)
    private String errorCode;

    /** Сообщение об ошибке */
    @Column(length = 500)
    private String errorMessage;

    /** Сумма возврата */
    @Column(precision = 19, scale = 4)
    private BigDecimal refundedAmount;

    /** Метаданные платежа (JSON) */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /** IP адрес клиента */
    @Column(length = 45)
    private String clientIp;

    /** User-Agent клиента */
    @Column(length = 500)
    private String userAgent;

    /** Связанные транзакции */
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    /** Время создания */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Время последнего обновления */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Время завершения платежа */
    private LocalDateTime completedAt;

    /** Время истечения (для pending платежей) */
    private LocalDateTime expiresAt;

    /** Версия для оптимистичной блокировки */
    @Version
    private Long version;

    /**
     * Добавить транзакцию к платежу.
     */
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setPayment(this);
    }

    /**
     * Проверить, можно ли отменить платёж.
     */
    public boolean isCancellable() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    /**
     * Проверить, можно ли сделать возврат.
     */
    public boolean isRefundable() {
        return status == PaymentStatus.COMPLETED || status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * Получить доступную сумму для возврата.
     */
    public BigDecimal getRefundableAmount() {
        if (!isRefundable()) {
            return BigDecimal.ZERO;
        }
        BigDecimal refunded = refundedAmount != null ? refundedAmount : BigDecimal.ZERO;
        return amount.subtract(refunded);
    }
}
