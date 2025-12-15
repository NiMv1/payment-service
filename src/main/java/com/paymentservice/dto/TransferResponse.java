package com.paymentservice.dto;

import com.paymentservice.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO ответа на перевод.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private UUID transferId;
    private String fromUserId;
    private String toUserId;
    private BigDecimal amount;
    private Currency currency;
    private String status;
    private String description;
    private LocalDateTime createdAt;
}
