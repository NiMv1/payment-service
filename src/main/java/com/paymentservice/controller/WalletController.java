package com.paymentservice.controller;

import com.paymentservice.domain.enums.Currency;
import com.paymentservice.dto.TransferRequest;
import com.paymentservice.dto.TransferResponse;
import com.paymentservice.dto.WalletResponse;
import com.paymentservice.saga.TransferSaga;
import com.paymentservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST контроллер для работы с кошельками.
 */
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Кошельки", description = "API для работы с кошельками и переводами")
public class WalletController {

    private final WalletService walletService;
    private final TransferSaga transferSaga;

    @PostMapping
    @Operation(summary = "Создать кошелёк", description = "Создание нового кошелька для пользователя")
    public ResponseEntity<WalletResponse> createWallet(
            @Parameter(description = "ID пользователя")
            @RequestParam String userId,
            @Parameter(description = "Валюта кошелька")
            @RequestParam Currency currency,
            @Parameter(description = "Начальный баланс")
            @RequestParam(required = false) BigDecimal initialBalance) {
        
        WalletResponse response = walletService.createWallet(userId, currency, initialBalance);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}/{currency}")
    @Operation(summary = "Получить кошелёк", description = "Получение кошелька пользователя по валюте")
    public ResponseEntity<WalletResponse> getWallet(
            @Parameter(description = "ID пользователя")
            @PathVariable String userId,
            @Parameter(description = "Валюта")
            @PathVariable Currency currency) {
        
        return ResponseEntity.ok(walletService.getWallet(userId, currency));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить все кошельки", description = "Получение всех кошельков пользователя")
    public ResponseEntity<List<WalletResponse>> getUserWallets(
            @Parameter(description = "ID пользователя")
            @PathVariable String userId) {
        
        return ResponseEntity.ok(walletService.getUserWallets(userId));
    }

    @PostMapping("/{userId}/{currency}/deposit")
    @Operation(summary = "Пополнить кошелёк", description = "Пополнение баланса кошелька")
    public ResponseEntity<WalletResponse> deposit(
            @Parameter(description = "ID пользователя")
            @PathVariable String userId,
            @Parameter(description = "Валюта")
            @PathVariable Currency currency,
            @Parameter(description = "Сумма пополнения")
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount) {
        
        return ResponseEntity.ok(walletService.deposit(userId, currency, amount));
    }

    @PostMapping("/{userId}/{currency}/withdraw")
    @Operation(summary = "Списать с кошелька", description = "Списание средств с кошелька")
    public ResponseEntity<WalletResponse> withdraw(
            @Parameter(description = "ID пользователя")
            @PathVariable String userId,
            @Parameter(description = "Валюта")
            @PathVariable Currency currency,
            @Parameter(description = "Сумма списания")
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount) {
        
        return ResponseEntity.ok(walletService.withdraw(userId, currency, amount));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Перевод между кошельками", 
               description = "Перевод средств между кошельками с использованием Saga Pattern")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        
        TransferResponse response = transferSaga.executeTransfer(request);
        return ResponseEntity.ok(response);
    }
}
