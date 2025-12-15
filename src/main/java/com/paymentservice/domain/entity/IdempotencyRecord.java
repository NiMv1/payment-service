package com.paymentservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Запись идемпотентности.
 * Хранит результаты обработанных запросов для обеспечения идемпотентности.
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_idempotency_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Ключ идемпотентности */
    @Column(nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    /** ID связанного платежа */
    private UUID paymentId;

    /** HTTP статус ответа */
    @Column(nullable = false)
    private Integer responseStatus;

    /** Тело ответа (JSON) */
    @Column(columnDefinition = "TEXT")
    private String responseBody;

    /** Время создания */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Время истечения записи */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Проверить, истекла ли запись.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
