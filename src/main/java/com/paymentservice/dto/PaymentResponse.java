package com.paymentservice.dto;

import com.paymentservice.domain.enums.Currency;
import com.paymentservice.domain.enums.PaymentMethod;
import com.paymentservice.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO ответа с информацией о платеже.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private String orderId;
    private String userId;
    private String merchantId;
    private BigDecimal amount;
    private Currency currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String description;
    private String externalTransactionId;
    private String errorCode;
    private String errorMessage;
    private BigDecimal refundedAmount;
    private BigDecimal refundableAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}
