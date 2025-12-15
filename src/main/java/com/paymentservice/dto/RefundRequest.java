package com.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для запроса возврата.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    /** Сумма возврата (если null - полный возврат) */
    @DecimalMin(value = "0.01", message = "Сумма возврата должна быть больше 0")
    @Digits(integer = 15, fraction = 4, message = "Некорректный формат суммы")
    private BigDecimal amount;

    /** Причина возврата */
    @Size(max = 500, message = "Причина не должна превышать 500 символов")
    private String reason;
}
