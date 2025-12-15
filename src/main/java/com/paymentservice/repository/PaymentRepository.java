package com.paymentservice.repository;

import com.paymentservice.domain.entity.Payment;
import com.paymentservice.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с платежами.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Найти платёж по ключу идемпотентности.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Найти платёж по ID заказа.
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Найти все платежи пользователя.
     */
    Page<Payment> findByUserId(String userId, Pageable pageable);

    /**
     * Найти платежи по статусу.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Найти платежи пользователя по статусу.
     */
    Page<Payment> findByUserIdAndStatus(String userId, PaymentStatus status, Pageable pageable);

    /**
     * Найти истёкшие pending платежи.
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    List<Payment> findExpiredPendingPayments(@Param("now") LocalDateTime now);

    /**
     * Найти платежи за период.
     */
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.createdAt BETWEEN :from AND :to")
    Page<Payment> findByUserIdAndPeriod(
            @Param("userId") String userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    /**
     * Подсчитать количество платежей пользователя по статусу.
     */
    long countByUserIdAndStatus(String userId, PaymentStatus status);

    /**
     * Проверить существование платежа по ключу идемпотентности.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
