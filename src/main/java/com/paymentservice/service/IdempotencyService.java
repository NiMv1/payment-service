package com.paymentservice.service;

import com.paymentservice.domain.entity.IdempotencyRecord;
import com.paymentservice.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для обеспечения идемпотентности операций.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    /** Время жизни записи идемпотентности (24 часа) */
    private static final int IDEMPOTENCY_TTL_HOURS = 24;

    /**
     * Проверить, существует ли запись идемпотентности.
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .filter(record -> !record.isExpired());
    }

    /**
     * Сохранить запись идемпотентности.
     */
    @Transactional
    public IdempotencyRecord saveRecord(String idempotencyKey, UUID paymentId, 
                                         int responseStatus, String responseBody) {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .paymentId(paymentId)
                .responseStatus(responseStatus)
                .responseBody(responseBody)
                .expiresAt(LocalDateTime.now().plusHours(IDEMPOTENCY_TTL_HOURS))
                .build();
        
        return idempotencyRepository.save(record);
    }

    /**
     * Удалить истёкшие записи (запускается по расписанию).
     */
    @Scheduled(fixedRate = 3600000) // Каждый час
    @Transactional
    public void cleanupExpiredRecords() {
        int deleted = idempotencyRepository.deleteExpiredRecords(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Удалено {} истёкших записей идемпотентности", deleted);
        }
    }
}
