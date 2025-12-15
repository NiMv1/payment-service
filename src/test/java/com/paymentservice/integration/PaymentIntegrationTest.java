package com.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentservice.domain.enums.Currency;
import com.paymentservice.domain.enums.PaymentMethod;
import com.paymentservice.domain.enums.PaymentStatus;
import com.paymentservice.dto.CreatePaymentRequest;
import com.paymentservice.dto.PaymentResponse;
import com.paymentservice.dto.RefundRequest;
import com.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для Payment API.
 * Используют Testcontainers для PostgreSQL и Kafka.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Payment API Integration Tests")
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("payment_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        
        // Отключаем Redis для тестов
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        
        // Flyway
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    private static UUID createdPaymentId;

    @BeforeEach
    void setUp() {
        // Очистка перед каждым тестом не нужна, т.к. тесты упорядочены
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/payments - Создание платежа")
    void createPayment_Success() throws Exception {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("ORDER-INT-" + UUID.randomUUID().toString().substring(0, 8));
        request.setUserId("USER-INT-001");
        request.setMerchantId("MERCHANT-INT-001");
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency(Currency.RUB);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setDescription("Интеграционный тест");

        String idempotencyKey = "int-test-" + UUID.randomUUID();

        // when
        MvcResult result = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(request.getOrderId()))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        // then
        PaymentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), PaymentResponse.class);
        
        createdPaymentId = response.getId();
        assertThat(createdPaymentId).isNotNull();
        
        // Проверяем в БД
        assertThat(paymentRepository.findById(createdPaymentId)).isPresent();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/payments - Идемпотентность (повторный запрос)")
    void createPayment_Idempotency() throws Exception {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("ORDER-IDEM-" + UUID.randomUUID().toString().substring(0, 8));
        request.setUserId("USER-IDEM-001");
        request.setMerchantId("MERCHANT-IDEM-001");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency(Currency.RUB);
        request.setPaymentMethod(PaymentMethod.CARD);

        String idempotencyKey = "idem-test-" + UUID.randomUUID();

        // when - первый запрос
        MvcResult firstResult = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(), PaymentResponse.class);

        // when - повторный запрос с тем же ключом
        MvcResult secondResult = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(), PaymentResponse.class);

        // then - должен вернуться тот же платёж
        assertThat(secondResponse.getId()).isEqualTo(firstResponse.getId());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/payments/{id} - Получение платежа")
    void getPayment_Success() throws Exception {
        // given - используем платёж из первого теста
        assertThat(createdPaymentId).isNotNull();

        // when/then
        mockMvc.perform(get("/api/payments/{id}", createdPaymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdPaymentId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/payments/{id}/confirm - Подтверждение платежа")
    void confirmPayment_Success() throws Exception {
        // given
        assertThat(createdPaymentId).isNotNull();

        // when/then
        mockMvc.perform(post("/api/payments/{id}/confirm", createdPaymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.externalTransactionId").isNotEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/payments/{id}/refund - Частичный возврат")
    void refundPayment_Partial_Success() throws Exception {
        // given
        assertThat(createdPaymentId).isNotNull();
        
        RefundRequest request = new RefundRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setReason("Частичный возврат");

        // when/then
        mockMvc.perform(post("/api/payments/{id}/refund", createdPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.refundedAmount").value(50.00));
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/payments/{id}/refund - Полный возврат оставшейся суммы")
    void refundPayment_Full_Success() throws Exception {
        // given
        assertThat(createdPaymentId).isNotNull();
        
        RefundRequest request = new RefundRequest();
        request.setAmount(new BigDecimal("200.00")); // Оставшаяся сумма
        request.setReason("Полный возврат");

        // when/then
        mockMvc.perform(post("/api/payments/{id}/refund", createdPaymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundedAmount").value(250.00));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/payments/{id} - Платёж не найден")
    void getPayment_NotFound() throws Exception {
        // given
        UUID unknownId = UUID.randomUUID();

        // when/then
        mockMvc.perform(get("/api/payments/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/payments - Валидация: отсутствует сумма")
    void createPayment_ValidationError_MissingAmount() throws Exception {
        // given
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId("ORDER-VAL-001");
        request.setUserId("USER-VAL-001");
        // amount не указан

        // when/then
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "val-test-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
