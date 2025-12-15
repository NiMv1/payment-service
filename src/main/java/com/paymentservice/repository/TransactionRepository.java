package com.paymentservice.repository;

import com.paymentservice.domain.entity.Transaction;
import com.paymentservice.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с транзакциями.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Найти транзакции по ID платежа.
     */
    List<Transaction> findByPaymentId(UUID paymentId);

    /**
     * Найти транзакцию по внешнему ID.
     */
    Optional<Transaction> findByExternalId(String externalId);

    /**
     * Найти транзакции платежа по типу.
     */
    List<Transaction> findByPaymentIdAndType(UUID paymentId, TransactionType type);
}
