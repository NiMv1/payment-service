package com.paymentservice.repository;

import com.paymentservice.domain.entity.Wallet;
import com.paymentservice.domain.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с кошельками.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Найти кошелёк пользователя по валюте.
     */
    Optional<Wallet> findByUserIdAndCurrency(String userId, Currency currency);

    /**
     * Найти кошелёк с блокировкой для обновления (pessimistic lock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.currency = :currency")
    Optional<Wallet> findByUserIdAndCurrencyForUpdate(
            @Param("userId") String userId,
            @Param("currency") Currency currency
    );

    /**
     * Найти все кошельки пользователя.
     */
    List<Wallet> findByUserId(String userId);

    /**
     * Найти активные кошельки пользователя.
     */
    List<Wallet> findByUserIdAndActiveTrue(String userId);

    /**
     * Проверить существование кошелька.
     */
    boolean existsByUserIdAndCurrency(String userId, Currency currency);
}
