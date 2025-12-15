package com.paymentservice.service;

import com.paymentservice.domain.entity.Wallet;
import com.paymentservice.domain.enums.Currency;
import com.paymentservice.dto.WalletResponse;
import com.paymentservice.exception.InsufficientFundsException;
import com.paymentservice.exception.PaymentException;
import com.paymentservice.exception.PaymentNotFoundException;
import com.paymentservice.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для WalletService.
 * Тестирует операции с кошельками.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet testWallet;
    private UUID walletId;
    private String userId;
    private Currency currency;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        userId = "USER-" + UUID.randomUUID().toString().substring(0, 8);
        currency = Currency.RUB;

        testWallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .balance(new BigDecimal("1000.00"))
                .blockedAmount(BigDecimal.ZERO)
                .currency(currency)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Получение кошелька")
    class GetWalletTests {

        @Test
        @DisplayName("Успешное получение кошелька по userId и currency")
        void getWallet_ByUserIdAndCurrency_Success() {
            // given
            when(walletRepository.findByUserIdAndCurrency(userId, currency))
                    .thenReturn(Optional.of(testWallet));

            // when
            WalletResponse response = walletService.getWallet(userId, currency);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(walletId);
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("Ошибка: кошелёк не найден")
        void getWallet_NotFound_ThrowsException() {
            // given
            when(walletRepository.findByUserIdAndCurrency(userId, currency))
                    .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> walletService.getWallet(userId, currency))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("Успешное получение всех кошельков пользователя")
        void getUserWallets_Success() {
            // given
            when(walletRepository.findByUserIdAndActiveTrue(userId))
                    .thenReturn(List.of(testWallet));

            // when
            List<WalletResponse> responses = walletService.getUserWallets(userId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("Пополнение кошелька")
    class DepositTests {

        @Test
        @DisplayName("Успешное пополнение кошелька")
        void deposit_Success() {
            // given
            BigDecimal depositAmount = new BigDecimal("500.00");
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            WalletResponse response = walletService.deposit(userId, currency, depositAmount);

            // then
            assertThat(response).isNotNull();
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Ошибка: кошелёк не найден при пополнении")
        void deposit_WalletNotFound_ThrowsException() {
            // given
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> walletService.deposit(userId, currency, new BigDecimal("100.00")))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Списание с кошелька")
    class WithdrawTests {

        @Test
        @DisplayName("Успешное списание с кошелька")
        void withdraw_Success() {
            // given
            BigDecimal withdrawAmount = new BigDecimal("300.00");
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            WalletResponse response = walletService.withdraw(userId, currency, withdrawAmount);

            // then
            assertThat(response).isNotNull();
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Ошибка: недостаточно средств")
        void withdraw_InsufficientFunds_ThrowsException() {
            // given
            BigDecimal largeAmount = new BigDecimal("2000.00");
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.of(testWallet));

            // when/then
            assertThatThrownBy(() -> walletService.withdraw(userId, currency, largeAmount))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("Ошибка: кошелёк не найден при списании")
        void withdraw_WalletNotFound_ThrowsException() {
            // given
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> walletService.withdraw(userId, currency, new BigDecimal("100.00")))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Блокировка средств")
    class BlockAmountTests {

        @Test
        @DisplayName("Успешная блокировка средств")
        void blockAmount_Success() {
            // given
            BigDecimal blockAmount = new BigDecimal("200.00");
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.of(testWallet));

            // when
            walletService.blockAmount(userId, currency, blockAmount);

            // then
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Ошибка: недостаточно средств для блокировки")
        void blockAmount_InsufficientFunds_ThrowsException() {
            // given
            BigDecimal largeBlockAmount = new BigDecimal("1500.00");
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.of(testWallet));

            // when/then
            assertThatThrownBy(() -> walletService.blockAmount(userId, currency, largeBlockAmount))
                    .isInstanceOf(InsufficientFundsException.class);
        }
    }

    @Nested
    @DisplayName("Разблокировка средств")
    class UnblockAmountTests {

        @BeforeEach
        void setUpBlocked() {
            testWallet.setBlockedAmount(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Успешная разблокировка средств")
        void unblockAmount_Success() {
            // given
            BigDecimal unblockAmount = new BigDecimal("100.00");
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.of(testWallet));

            // when
            walletService.unblockAmount(userId, currency, unblockAmount);

            // then
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Ошибка: кошелёк не найден при разблокировке")
        void unblockAmount_WalletNotFound_ThrowsException() {
            // given
            when(walletRepository.findByUserIdAndCurrencyForUpdate(userId, currency))
                    .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> walletService.unblockAmount(userId, currency, new BigDecimal("100.00")))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Создание кошелька")
    class CreateWalletTests {

        @Test
        @DisplayName("Успешное создание кошелька")
        void createWallet_Success() {
            // given
            String newUserId = "NEW-USER-001";
            when(walletRepository.existsByUserIdAndCurrency(newUserId, currency)).thenReturn(false);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
                Wallet w = invocation.getArgument(0);
                w.setId(UUID.randomUUID());
                return w;
            });

            // when
            WalletResponse response = walletService.createWallet(newUserId, currency, BigDecimal.ZERO);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(newUserId);
        }

        @Test
        @DisplayName("Создание кошелька с начальным балансом")
        void createWallet_WithInitialBalance_Success() {
            // given
            String newUserId = "NEW-USER-002";
            BigDecimal initialBalance = new BigDecimal("500.00");
            when(walletRepository.existsByUserIdAndCurrency(newUserId, currency)).thenReturn(false);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
                Wallet w = invocation.getArgument(0);
                w.setId(UUID.randomUUID());
                return w;
            });

            // when
            WalletResponse response = walletService.createWallet(newUserId, currency, initialBalance);

            // then
            assertThat(response).isNotNull();
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Ошибка: кошелёк уже существует")
        void createWallet_AlreadyExists_ThrowsException() {
            // given
            when(walletRepository.existsByUserIdAndCurrency(userId, currency)).thenReturn(true);

            // when/then
            assertThatThrownBy(() -> walletService.createWallet(userId, currency, BigDecimal.ZERO))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("уже существует");
        }
    }
}
