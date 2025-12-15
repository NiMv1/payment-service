package com.paymentservice.service;

import com.paymentservice.domain.entity.Wallet;
import com.paymentservice.domain.enums.Currency;
import com.paymentservice.dto.WalletResponse;
import com.paymentservice.exception.InsufficientFundsException;
import com.paymentservice.exception.PaymentException;
import com.paymentservice.exception.PaymentNotFoundException;
import com.paymentservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для работы с кошельками.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    /**
     * Создать кошелёк для пользователя.
     */
    @Transactional
    public WalletResponse createWallet(String userId, Currency currency, BigDecimal initialBalance) {
        log.info("Создание кошелька: userId={}, currency={}", userId, currency);

        if (walletRepository.existsByUserIdAndCurrency(userId, currency)) {
            throw new PaymentException("WALLET_EXISTS", 
                    "Кошелёк уже существует для пользователя " + userId + " в валюте " + currency);
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .currency(currency)
                .balance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Кошелёк создан: id={}", wallet.getId());
        return toResponse(wallet);
    }

    /**
     * Получить кошелёк пользователя по валюте.
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(String userId, Currency currency) {
        return walletRepository.findByUserIdAndCurrency(userId, currency)
                .map(this::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Кошелёк не найден для пользователя " + userId + " в валюте " + currency));
    }

    /**
     * Получить все кошельки пользователя.
     */
    @Transactional(readOnly = true)
    public List<WalletResponse> getUserWallets(String userId) {
        return walletRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Пополнить кошелёк.
     */
    @Transactional
    public WalletResponse deposit(String userId, Currency currency, BigDecimal amount) {
        log.info("Пополнение кошелька: userId={}, currency={}, amount={}", userId, currency, amount);

        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Кошелёк не найден для пользователя " + userId));

        wallet.credit(amount);
        wallet = walletRepository.save(wallet);

        log.info("Кошелёк пополнен: id={}, newBalance={}", wallet.getId(), wallet.getBalance());
        return toResponse(wallet);
    }

    /**
     * Списать с кошелька.
     */
    @Transactional
    public WalletResponse withdraw(String userId, Currency currency, BigDecimal amount) {
        log.info("Списание с кошелька: userId={}, currency={}, amount={}", userId, currency, amount);

        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Кошелёк не найден для пользователя " + userId));

        if (!wallet.hasSufficientFunds(amount)) {
            throw new InsufficientFundsException(
                    "Недостаточно средств. Доступно: " + wallet.getAvailableBalance());
        }

        wallet.debit(amount);
        wallet = walletRepository.save(wallet);

        log.info("Списание выполнено: id={}, newBalance={}", wallet.getId(), wallet.getBalance());
        return toResponse(wallet);
    }

    /**
     * Заблокировать сумму на кошельке (для Saga).
     */
    @Transactional
    public void blockAmount(String userId, Currency currency, BigDecimal amount) {
        log.info("Блокировка суммы: userId={}, currency={}, amount={}", userId, currency, amount);

        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Кошелёк не найден для пользователя " + userId));

        if (!wallet.hasSufficientFunds(amount)) {
            throw new InsufficientFundsException(
                    "Недостаточно средств для блокировки. Доступно: " + wallet.getAvailableBalance());
        }

        wallet.blockAmount(amount);
        walletRepository.save(wallet);
        log.info("Сумма заблокирована: userId={}, amount={}", userId, amount);
    }

    /**
     * Разблокировать сумму (откат Saga).
     */
    @Transactional
    public void unblockAmount(String userId, Currency currency, BigDecimal amount) {
        log.info("Разблокировка суммы: userId={}, currency={}, amount={}", userId, currency, amount);

        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Кошелёк не найден для пользователя " + userId));

        wallet.unblockAmount(amount);
        walletRepository.save(wallet);
        log.info("Сумма разблокирована: userId={}, amount={}", userId, amount);
    }

    /**
     * Списать заблокированную сумму (подтверждение Saga).
     */
    @Transactional
    public void debitBlocked(String userId, Currency currency, BigDecimal amount) {
        log.info("Списание заблокированной суммы: userId={}, currency={}, amount={}", userId, currency, amount);

        Wallet wallet = walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Кошелёк не найден для пользователя " + userId));

        wallet.debitBlocked(amount);
        walletRepository.save(wallet);
        log.info("Заблокированная сумма списана: userId={}, amount={}", userId, amount);
    }

    /**
     * Преобразовать Wallet в WalletResponse.
     */
    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .blockedAmount(wallet.getBlockedAmount())
                .availableBalance(wallet.getAvailableBalance())
                .active(wallet.getActive())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
