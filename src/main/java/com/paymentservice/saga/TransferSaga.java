package com.paymentservice.saga;

import com.paymentservice.domain.enums.Currency;
import com.paymentservice.dto.TransferRequest;
import com.paymentservice.dto.TransferResponse;
import com.paymentservice.exception.PaymentException;
import com.paymentservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Реализация Saga Pattern для переводов между кошельками.
 * 
 * Шаги Saga:
 * 1. Блокировка суммы на кошельке отправителя
 * 2. Пополнение кошелька получателя
 * 3. Списание заблокированной суммы у отправителя
 * 
 * Компенсация (при ошибке):
 * - Разблокировка суммы на кошельке отправителя
 * - Списание с кошелька получателя (если было пополнение)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferSaga {

    private final WalletService walletService;

    /**
     * Выполнить перевод между кошельками с использованием Saga Pattern.
     */
    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        UUID transferId = UUID.randomUUID();
        String fromUserId = request.getFromUserId();
        String toUserId = request.getToUserId();
        BigDecimal amount = request.getAmount();
        Currency currency = request.getCurrency();

        log.info("Начало Saga перевода: transferId={}, from={}, to={}, amount={}", 
                transferId, fromUserId, toUserId, amount);

        // Состояние Saga для компенсации
        boolean amountBlocked = false;
        boolean recipientCredited = false;

        try {
            // Шаг 1: Блокировка суммы у отправителя
            log.info("Saga шаг 1: Блокировка суммы у отправителя");
            walletService.blockAmount(fromUserId, currency, amount);
            amountBlocked = true;

            // Шаг 2: Пополнение кошелька получателя
            log.info("Saga шаг 2: Пополнение кошелька получателя");
            walletService.deposit(toUserId, currency, amount);
            recipientCredited = true;

            // Шаг 3: Списание заблокированной суммы у отправителя
            log.info("Saga шаг 3: Списание заблокированной суммы");
            walletService.debitBlocked(fromUserId, currency, amount);

            log.info("Saga перевода завершена успешно: transferId={}", transferId);

            return TransferResponse.builder()
                    .transferId(transferId)
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .amount(amount)
                    .currency(currency)
                    .status("COMPLETED")
                    .description(request.getDescription())
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Ошибка в Saga перевода: transferId={}, error={}", transferId, e.getMessage());

            // Компенсация
            compensate(fromUserId, toUserId, amount, currency, amountBlocked, recipientCredited);

            throw new PaymentException("TRANSFER_FAILED", 
                    "Ошибка перевода: " + e.getMessage(), e);
        }
    }

    /**
     * Компенсация при ошибке Saga.
     */
    private void compensate(String fromUserId, String toUserId, BigDecimal amount, 
                           Currency currency, boolean amountBlocked, boolean recipientCredited) {
        log.info("Начало компенсации Saga");

        // Компенсация шага 2: списание с получателя
        if (recipientCredited) {
            try {
                log.info("Компенсация: списание с получателя");
                walletService.withdraw(toUserId, currency, amount);
            } catch (Exception e) {
                log.error("Ошибка компенсации (списание с получателя): {}", e.getMessage());
            }
        }

        // Компенсация шага 1: разблокировка у отправителя
        if (amountBlocked) {
            try {
                log.info("Компенсация: разблокировка у отправителя");
                walletService.unblockAmount(fromUserId, currency, amount);
            } catch (Exception e) {
                log.error("Ошибка компенсации (разблокировка): {}", e.getMessage());
            }
        }

        log.info("Компенсация Saga завершена");
    }
}
