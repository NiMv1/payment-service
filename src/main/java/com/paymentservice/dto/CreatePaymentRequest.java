package com.paymentservice.dto;

import com.paymentservice.domain.enums.Currency;
import com.paymentservice.domain.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для создания платежа.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    /** ID заказа */
    @NotBlank(message = "ID заказа обязателен")
    @Size(max = 64, message = "ID заказа не должен превышать 64 символа")
    private String orderId;

    /** ID пользователя */
    @NotBlank(message = "ID пользователя обязателен")
    @Size(max = 64, message = "ID пользователя не должен превышать 64 символа")
    private String userId;

    /** ID продавца */
    @Size(max = 64, message = "ID продавца не должен превышать 64 символа")
    private String merchantId;

    /** Сумма платежа */
    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @Digits(integer = 15, fraction = 4, message = "Некорректный формат суммы")
    private BigDecimal amount;

    /** Валюта */
    @NotNull(message = "Валюта обязательна")
    private Currency currency;

    /** Метод оплаты */
    @NotNull(message = "Метод оплаты обязателен")
    private PaymentMethod paymentMethod;

    /** Описание платежа */
    @Size(max = 500, message = "Описание не должно превышать 500 символов")
    private String description;

    /** Метаданные (JSON) */
    private String metadata;

    /** Время жизни платежа в минутах (по умолчанию 30) */
    @Min(value = 1, message = "Время жизни должно быть не менее 1 минуты")
    @Max(value = 1440, message = "Время жизни не должно превышать 24 часа")
    private Integer expirationMinutes;
}
