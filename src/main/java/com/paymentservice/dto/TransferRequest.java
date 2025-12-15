package com.paymentservice.dto;

import com.paymentservice.domain.enums.Currency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для перевода между кошельками (Saga Pattern).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    /** ID отправителя */
    @NotBlank(message = "ID отправителя обязателен")
    private String fromUserId;

    /** ID получателя */
    @NotBlank(message = "ID получателя обязателен")
    private String toUserId;

    /** Сумма перевода */
    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    private BigDecimal amount;

    /** Валюта */
    @NotNull(message = "Валюта обязательна")
    private Currency currency;

    /** Описание перевода */
    @Size(max = 500)
    private String description;
}
