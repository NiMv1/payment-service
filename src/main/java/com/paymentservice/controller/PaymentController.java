package com.paymentservice.controller;

import com.paymentservice.dto.CreatePaymentRequest;
import com.paymentservice.dto.PaymentResponse;
import com.paymentservice.dto.RefundRequest;
import com.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST контроллер для работы с платежами.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Платежи", description = "API для работы с платежами")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Создать платёж", description = "Создание нового платежа с проверкой идемпотентности")
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(description = "Ключ идемпотентности", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        
        PaymentResponse response = paymentService.createPayment(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Получить платёж", description = "Получение информации о платеже по ID")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "ID платежа")
            @PathVariable UUID paymentId) {
        
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Получить платёж по заказу", description = "Получение платежа по ID заказа")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @Parameter(description = "ID заказа")
            @PathVariable String orderId) {
        
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Получить платежи пользователя", description = "Получение списка платежей пользователя")
    public ResponseEntity<Page<PaymentResponse>> getUserPayments(
            @Parameter(description = "ID пользователя")
            @PathVariable String userId,
            Pageable pageable) {
        
        return ResponseEntity.ok(paymentService.getUserPayments(userId, pageable));
    }

    @PostMapping("/{paymentId}/confirm")
    @Operation(summary = "Подтвердить платёж", description = "Подтверждение платежа (симуляция успешной оплаты)")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @Parameter(description = "ID платежа")
            @PathVariable UUID paymentId) {
        
        return ResponseEntity.ok(paymentService.confirmPayment(paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Отменить платёж", description = "Отмена платежа")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @Parameter(description = "ID платежа")
            @PathVariable UUID paymentId) {
        
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Возврат платежа", description = "Полный или частичный возврат платежа")
    public ResponseEntity<PaymentResponse> refundPayment(
            @Parameter(description = "ID платежа")
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request) {
        
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, request));
    }
}
