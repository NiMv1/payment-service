package com.paymentservice.domain.entity;

import com.paymentservice.domain.enums.Currency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность кошелька пользователя.
 * Для демонстрации Saga Pattern при переводах между кошельками.
 */
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_wallet_user_id", columnList = "userId"),
    @Index(name = "idx_wallet_user_currency", columnList = "userId, currency", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID пользователя */
    @Column(nullable = false, length = 64)
    private String userId;

    /** Валюта кошелька */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** Баланс */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /** Заблокированная сумма (холдирование) */
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal blockedAmount = BigDecimal.ZERO;

    /** Активен ли кошелёк */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Время создания */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Время последнего обновления */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Версия для оптимистичной блокировки */
    @Version
    private Long version;

    /**
     * Получить доступный баланс (баланс минус заблокированная сумма).
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(blockedAmount);
    }

    /**
     * Проверить, достаточно ли средств.
     */
    public boolean hasSufficientFunds(BigDecimal amount) {
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    /**
     * Заблокировать сумму.
     */
    public void blockAmount(BigDecimal amount) {
        if (!hasSufficientFunds(amount)) {
            throw new IllegalStateException("Недостаточно средств для блокировки");
        }
        this.blockedAmount = this.blockedAmount.add(amount);
    }

    /**
     * Разблокировать сумму.
     */
    public void unblockAmount(BigDecimal amount) {
        if (this.blockedAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("Невозможно разблокировать больше, чем заблокировано");
        }
        this.blockedAmount = this.blockedAmount.subtract(amount);
    }

    /**
     * Списать заблокированную сумму.
     */
    public void debitBlocked(BigDecimal amount) {
        if (this.blockedAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("Недостаточно заблокированных средств");
        }
        this.blockedAmount = this.blockedAmount.subtract(amount);
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Пополнить баланс.
     */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    /**
     * Списать с баланса напрямую.
     */
    public void debit(BigDecimal amount) {
        if (!hasSufficientFunds(amount)) {
            throw new IllegalStateException("Недостаточно средств");
        }
        this.balance = this.balance.subtract(amount);
    }
}
