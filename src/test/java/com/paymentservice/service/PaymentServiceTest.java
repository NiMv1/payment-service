package com.paymentservice.service;

import com.paymentservice.domain.entity.Payment;
import com.paymentservice.domain.entity.Transaction;
import com.paymentservice.domain.enums.Currency;
import com.paymentservice.domain.enums.PaymentMethod;
import com.paymentservice.domain.enums.PaymentStatus;
import com.paymentservice.domain.enums.TransactionType;
import com.paymentservice.dto.CreatePaymentRequest;
import com.paymentservice.dto.PaymentResponse;
import com.paymentservice.dto.RefundRequest;
import com.paymentservice.exception.InvalidPaymentStateException;
import com.paymentservice.exception.PaymentException;
import com.paymentservice.exception.PaymentNotFoundException;
import com.paymentservice.kafka.PaymentEventProducer;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для PaymentService.
 * Тестирует бизнес-логику платежей с моками зависимостей.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PaymentEventProducer eventProducer;

    @InjectMocks
    private PaymentService paymentService;

    private CreatePaymentRequest validRequest;
    private Payment savedPayment;
    private UUID paymentId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        idempotencyKey = "idem-key-" + UUID.randomUUID();

        validRequest = new CreatePaymentRequest();
        validRequest.setOrderId("ORDER-123");
        validRequest.setUserId("USER-456");
        validRequest.setMerchantId("MERCHANT-789");
        validRequest.setAmount(new BigDecimal("100.00"));
        validRequest.setCurrency(Currency.RUB);
        validRequest.setPaymentMethod(PaymentMethod.CARD);
        validRequest.setDescription("Тестовый платёж");

        savedPayment = Payment.builder()
                .id(paymentId)
                .idempotencyKey(idempotencyKey)
                .orderId("ORDER-123")
                .userId("USER-456")
                .merchantId("MERCHANT-789")
                .amount(new BigDecimal("100.00"))
                .currency(Currency.RUB)
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.PENDING)
                .description("Тестовый платёж")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    @Nested
    @DisplayName("Создание платежа")
    class CreatePaymentTests {

        @Test
        @DisplayName("Успешное создание нового платежа")
        void createPayment_Success() {
            // given
            when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

            // when
            PaymentResponse response = paymentService.createPayment(idempotencyKey, validRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(paymentId);
            assertThat(response.getOrderId()).isEqualTo("ORDER-123");
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);

            verify(paymentRepository).save(any(Payment.class));
            verify(transactionRepository).save(any(Transaction.class));
            verify(eventProducer).sendPaymentCreated(any(Payment.class));
        }

        @Test
        @DisplayName("Идемпотентность: возврат существующего платежа")
        void createPayment_IdempotencyReturnsExisting() {
            // given
            when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(savedPayment));

            // when
            PaymentResponse response = paymentService.createPayment(idempotencyKey, validRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(paymentId);

            // Не должен создавать новый платёж
            verify(paymentRepository, never()).save(any(Payment.class));
            verify(transactionRepository, never()).save(any(Transaction.class));
            verify(eventProducer, never()).sendPaymentCreated(any(Payment.class));
        }

        @Test
        @DisplayName("Создание платежа с кастомным временем истечения")
        void createPayment_WithCustomExpiration() {
            // given
            validRequest.setExpirationMinutes(60);
            when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                p.setId(paymentId);
                return p;
            });
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

            // when
            paymentService.createPayment(idempotencyKey, validRequest);

            // then
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment captured = captor.getValue();
            
            assertThat(captured.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(55));
        }
    }

    @Nested
    @DisplayName("Подтверждение платежа")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("Успешное подтверждение платежа в статусе PENDING")
        void confirmPayment_FromPending_Success() {
            // given
            savedPayment.setStatus(PaymentStatus.PENDING);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(transactionRepository.findByPaymentIdAndType(paymentId, TransactionType.PAYMENT))
                    .thenReturn(List.of(new Transaction()));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

            // when
            PaymentResponse response = paymentService.confirmPayment(paymentId);

            // then
            assertThat(response).isNotNull();
            verify(eventProducer).sendPaymentCompleted(any(Payment.class));
        }

        @Test
        @DisplayName("Успешное подтверждение платежа в статусе PROCESSING")
        void confirmPayment_FromProcessing_Success() {
            // given
            savedPayment.setStatus(PaymentStatus.PROCESSING);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(transactionRepository.findByPaymentIdAndType(paymentId, TransactionType.PAYMENT))
                    .thenReturn(List.of());

            // when
            PaymentResponse response = paymentService.confirmPayment(paymentId);

            // then
            assertThat(response).isNotNull();
            verify(eventProducer).sendPaymentCompleted(any(Payment.class));
        }

        @Test
        @DisplayName("Ошибка подтверждения уже завершённого платежа")
        void confirmPayment_AlreadyCompleted_ThrowsException() {
            // given
            savedPayment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));

            // when/then
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("Ошибка подтверждения отменённого платежа")
        void confirmPayment_Cancelled_ThrowsException() {
            // given
            savedPayment.setStatus(PaymentStatus.CANCELLED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));

            // when/then
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                    .isInstanceOf(InvalidPaymentStateException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("Ошибка: платёж не найден")
        void confirmPayment_NotFound_ThrowsException() {
            // given
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Отмена платежа")
    class CancelPaymentTests {

        @Test
        @DisplayName("Успешная отмена платежа в статусе PENDING")
        void cancelPayment_FromPending_Success() {
            // given
            savedPayment.setStatus(PaymentStatus.PENDING);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

            // when
            PaymentResponse response = paymentService.cancelPayment(paymentId);

            // then
            assertThat(response).isNotNull();
            verify(eventProducer).sendPaymentCancelled(any(Payment.class));
        }

        @Test
        @DisplayName("Ошибка отмены завершённого платежа")
        void cancelPayment_Completed_ThrowsException() {
            // given
            savedPayment.setStatus(PaymentStatus.COMPLETED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));

            // when/then
            assertThatThrownBy(() -> paymentService.cancelPayment(paymentId))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }
    }

    @Nested
    @DisplayName("Возврат платежа")
    class RefundPaymentTests {

        @BeforeEach
        void setUpRefund() {
            savedPayment.setStatus(PaymentStatus.COMPLETED);
            savedPayment.setRefundedAmount(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Полный возврат платежа")
        void refundPayment_FullRefund_Success() {
            // given
            RefundRequest request = new RefundRequest();
            request.setAmount(new BigDecimal("100.00"));
            request.setReason("Отмена заказа");

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

            // when
            PaymentResponse response = paymentService.refundPayment(paymentId, request);

            // then
            assertThat(response).isNotNull();
            verify(eventProducer).sendPaymentRefunded(any(Payment.class), eq(new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Частичный возврат платежа")
        void refundPayment_PartialRefund_Success() {
            // given
            RefundRequest request = new RefundRequest();
            request.setAmount(new BigDecimal("30.00"));

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                assertThat(p.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
                return p;
            });
            when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

            // when
            paymentService.refundPayment(paymentId, request);

            // then
            verify(eventProducer).sendPaymentRefunded(any(Payment.class), eq(new BigDecimal("30.00")));
        }

        @Test
        @DisplayName("Ошибка: сумма возврата превышает доступную")
        void refundPayment_AmountExceeded_ThrowsException() {
            // given
            RefundRequest request = new RefundRequest();
            request.setAmount(new BigDecimal("150.00")); // Больше чем 100

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));

            // when/then
            assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("превышает");
        }

        @Test
        @DisplayName("Ошибка возврата для неоплаченного платежа")
        void refundPayment_NotCompleted_ThrowsException() {
            // given
            savedPayment.setStatus(PaymentStatus.PENDING);
            RefundRequest request = new RefundRequest();
            request.setAmount(new BigDecimal("50.00"));

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));

            // when/then
            assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                    .isInstanceOf(InvalidPaymentStateException.class);
        }
    }

    @Nested
    @DisplayName("Получение платежа")
    class GetPaymentTests {

        @Test
        @DisplayName("Успешное получение платежа по ID")
        void getPayment_Success() {
            // given
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(savedPayment));

            // when
            PaymentResponse response = paymentService.getPayment(paymentId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(paymentId);
        }

        @Test
        @DisplayName("Ошибка: платёж не найден")
        void getPayment_NotFound_ThrowsException() {
            // given
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("Успешное получение платежа по orderId")
        void getPaymentByOrderId_Success() {
            // given
            when(paymentRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(savedPayment));

            // when
            PaymentResponse response = paymentService.getPaymentByOrderId("ORDER-123");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("ORDER-123");
        }

        @Test
        @DisplayName("Ошибка: платёж по orderId не найден")
        void getPaymentByOrderId_NotFound_ThrowsException() {
            // given
            when(paymentRepository.findByOrderId("UNKNOWN")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> paymentService.getPaymentByOrderId("UNKNOWN"))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }
}
