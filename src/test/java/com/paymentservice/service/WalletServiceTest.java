package com.paymentservice.service;

import com.paymentservice.domain.entity.Wallet;
import com.paymentservice.domain.enums.Currency;
import com.paymentservice.dto.WalletResponse;
import com.paymentservice.exception.InsufficientFundsException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        userId = "USER-" + UUID.randomUUID().toString().substring(0, 8);

        testWallet = Wallet.builder()
                .id(walletId)
                .userId(userId)
                .balance(new BigDecimal("1000.00"))
                .blockedAmount(BigDecimal.ZERO)
                .currency(Currency.RUB)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Получение кошелька")
    class GetWalletTests {

        @Test
        @DisplayName("Успешное получение кошелька по ID")
        void getWallet_ById_Success() {
            // given
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

            // when
            WalletResponse response = walletService.getWallet(walletId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(walletId);
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("Ошибка: кошелёк не найден")
        void getWallet_NotFound_ThrowsException() {
            // given
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> walletService.getWallet(walletId))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("Успешное получение кошелька по userId")
        void getWalletByUserId_Success() {
            // given
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(testWallet));

            // when
            WalletResponse response = walletService.getWalletByUserId(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
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
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            WalletResponse response = walletService.deposit(walletId, depositAmount);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("Ошибка: отрицательная сумма пополнения")
        void deposit_NegativeAmount_ThrowsException() {
            // given
            BigDecimal negativeAmount = new BigDecimal("-100.00");

            // when/then
            assertThatThrownBy(() -> walletService.deposit(walletId, negativeAmount))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Ошибка: нулевая сумма пополнения")
        void deposit_ZeroAmount_ThrowsException() {
            // given
            BigDecimal zeroAmount = BigDecimal.ZERO;

            // when/then
            assertThatThrownBy(() -> walletService.deposit(walletId, zeroAmount))
                    .isInstanceOf(IllegalArgumentException.class);
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
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            WalletResponse response = walletService.withdraw(walletId, withdrawAmount);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        }

        @Test
        @DisplayName("Ошибка: недостаточно средств")
        void withdraw_InsufficientFunds_ThrowsException() {
            // given
            BigDecimal largeAmount = new BigDecimal("2000.00");
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

            // when/then
            assertThatThrownBy(() -> walletService.withdraw(walletId, largeAmount))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("Ошибка: списание с заблокированными средствами")
        void withdraw_WithBlockedAmount_InsufficientFunds() {
            // given
            testWallet.setBlockedAmount(new BigDecimal("800.00")); // Доступно только 200
            BigDecimal withdrawAmount = new BigDecimal("500.00");
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(testWallet));

            // when/then
            assertThatThrownBy(() -> walletService.withdraw(walletId, withdrawAmount))
                    .isInstanceOf(InsufficientFundsException.class);
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
            when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            walletService.blockAmount(walletId, blockAmount);

            // then
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Ошибка: недостаточно средств для блокировки")
        void blockAmount_InsufficientFunds_ThrowsException() {
            // given
            BigDecimal largeBlockAmount = new BigDecimal("1500.00");
            when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(testWallet));

            // when/then
            assertThatThrownBy(() -> walletService.blockAmount(walletId, largeBlockAmount))
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
            when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            walletService.unblockAmount(walletId, unblockAmount);

            // then
            verify(walletRepository).save(any(Wallet.class));
        }

        @Test
        @DisplayName("Разблокировка суммы больше заблокированной - разблокирует всё")
        void unblockAmount_MoreThanBlocked_UnblocksAll() {
            // given
            BigDecimal largeUnblockAmount = new BigDecimal("500.00");
            when(walletRepository.findByIdWithLock(walletId)).thenReturn(Optional.of(testWallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
                Wallet w = invocation.getArgument(0);
                assertThat(w.getBlockedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                return w;
            });

            // when
            walletService.unblockAmount(walletId, largeUnblockAmount);

            // then
            verify(walletRepository).save(any(Wallet.class));
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
            when(walletRepository.findByUserId(newUserId)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
                Wallet w = invocation.getArgument(0);
                w.setId(UUID.randomUUID());
                return w;
            });

            // when
            WalletResponse response = walletService.createWallet(newUserId, Currency.RUB);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(newUserId);
            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Ошибка: кошелёк уже существует")
        void createWallet_AlreadyExists_ThrowsException() {
            // given
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(testWallet));

            // when/then
            assertThatThrownBy(() -> walletService.createWallet(userId, Currency.RUB))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("уже существует");
        }
    }
}
