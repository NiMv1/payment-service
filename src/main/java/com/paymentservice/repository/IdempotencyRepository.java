package com.paymentservice.repository;

import com.paymentservice.domain.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с записями идемпотентности.
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {

    /**
     * Найти запись по ключу идемпотентности.
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Проверить существование записи.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Удалить истёкшие записи.
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") LocalDateTime now);
}
