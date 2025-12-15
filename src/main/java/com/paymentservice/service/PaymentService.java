package com.paymentservice.service;

import com.paymentservice.domain.entity.Payment;
import com.paymentservice.domain.entity.Transaction;
import com.paymentservice.domain.enums.PaymentStatus;
import com.paymentservice.domain.enums.TransactionType;
import com.paymentservice.dto.CreatePaymentRequest;
import com.paymentservice.dto.PaymentResponse;
import com.paymentservice.dto.RefundRequest;
import com.paymentservice.exception.*;
import com.paymentservice.kafka.PaymentEventProducer;
import com.paymentservice.repository.PaymentRepository;
import com.paymentservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сервис для работы с платежами.
 * Реализует идемпотентность и бизнес-логику платежей.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventProducer eventProducer;

    /**
     * Создать новый платёж с проверкой идемпотентности.
     */
    @Transactional
    public PaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request) {
        log.info("Создание платежа: orderId={}, amount={}, idempotencyKey={}", 
                request.getOrderId(), request.getAmount(), idempotencyKey);

        // Проверка идемпотентности
        var existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            log.info("Найден существующий платёж по idempotencyKey: {}", idempotencyKey);
            return toResponse(existingPayment.get());
        }

        // Создание платежа
        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .expiresAt(LocalDateTime.now().plusMinutes(
                        request.getExpirationMinutes() != null ? request.getExpirationMinutes() : 30))
                .build();

        payment = paymentRepository.save(payment);

        // Создание транзакции
        Transaction transaction = Transaction.builder()
                .payment(payment)
                .type(TransactionType.PAYMENT)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();
        transactionRepository.save(transaction);

        // Отправка события
        eventProducer.sendPaymentCreated(payment);

        log.info("Платёж создан: id={}, status={}", payment.getId(), payment.getStatus());
        return toResponse(payment);
    }

    /**
     * Подтвердить платёж (симуляция успешной оплаты).
     */
    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId) {
        log.info("Подтверждение платежа: id={}", paymentId);

        Payment payment = getPaymentOrThrow(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStateException(
                    "Невозможно подтвердить платёж в статусе: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setExternalTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment = paymentRepository.save(payment);

        // Обновление транзакции
        var transactions = transactionRepository.findByPaymentIdAndType(paymentId, TransactionType.PAYMENT);
        if (!transactions.isEmpty()) {
            Transaction tx = transactions.get(0);
            tx.setStatus(PaymentStatus.COMPLETED);
            tx.setProcessedAt(LocalDateTime.now());
            tx.setExternalId(payment.getExternalTransactionId());
            transactionRepository.save(tx);
        }

        // Отправка события
        eventProducer.sendPaymentCompleted(payment);

        log.info("Платёж подтверждён: id={}", paymentId);
        return toResponse(payment);
    }

    /**
     * Отменить платёж.
     */
    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId) {
        log.info("Отмена платежа: id={}", paymentId);

        Payment payment = getPaymentOrThrow(paymentId);

        if (!payment.isCancellable()) {
            throw new InvalidPaymentStateException(
                    "Невозможно отменить платёж в статусе: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment = paymentRepository.save(payment);

        // Отправка события
        eventProducer.sendPaymentCancelled(payment);

        log.info("Платёж отменён: id={}", paymentId);
        return toResponse(payment);
    }

    /**
     * Возврат платежа (полный или частичный).
     */
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, RefundRequest request) {
        log.info("Возврат платежа: id={}, amount={}", paymentId, request.getAmount());

        Payment payment = getPaymentOrThrow(paymentId);

        if (!payment.isRefundable()) {
            throw new InvalidPaymentStateException(
                    "Невозможно сделать возврат для платежа в статусе: " + payment.getStatus());
        }

        BigDecimal refundAmount = request.getAmount() != null 
                ? request.getAmount() 
                : payment.getRefundableAmount();

        if (refundAmount.compareTo(payment.getRefundableAmount()) > 0) {
            throw new PaymentException("REFUND_AMOUNT_EXCEEDED", 
                    "Сумма возврата превышает доступную: " + payment.getRefundableAmount());
        }

        // Обновление суммы возврата
        BigDecimal currentRefunded = payment.getRefundedAmount() != null 
                ? payment.getRefundedAmount() 
                : BigDecimal.ZERO;
        payment.setRefundedAmount(currentRefunded.add(refundAmount));

        // Определение статуса
        if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        payment = paymentRepository.save(payment);

        // Создание транзакции возврата
        TransactionType txType = refundAmount.compareTo(payment.getAmount()) < 0 
                ? TransactionType.PARTIAL_REFUND 
                : TransactionType.REFUND;
        
        Transaction refundTx = Transaction.builder()
                .payment(payment)
                .type(txType)
                .amount(refundAmount)
                .currency(payment.getCurrency())
                .status(PaymentStatus.COMPLETED)
                .externalId("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .processedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(refundTx);

        // Отправка события
        eventProducer.sendPaymentRefunded(payment, refundAmount);

        log.info("Возврат выполнен: id={}, refundAmount={}", paymentId, refundAmount);
        return toResponse(payment);
    }

    /**
     * Получить платёж по ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        return toResponse(getPaymentOrThrow(paymentId));
    }

    /**
     * Получить платёж по ID заказа.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new PaymentNotFoundException("Платёж не найден для заказа: " + orderId));
    }

    /**
     * Получить платежи пользователя.
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(String userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Получить платёж или выбросить исключение.
     */
    private Payment getPaymentOrThrow(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Платёж не найден: " + paymentId));
    }

    /**
     * Преобразовать Payment в PaymentResponse.
     */
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .externalTransactionId(payment.getExternalTransactionId())
                .errorCode(payment.getErrorCode())
                .errorMessage(payment.getErrorMessage())
                .refundedAmount(payment.getRefundedAmount())
                .refundableAmount(payment.getRefundableAmount())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .completedAt(payment.getCompletedAt())
                .expiresAt(payment.getExpiresAt())
                .build();
    }
}
