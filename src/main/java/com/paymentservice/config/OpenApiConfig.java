package com.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация OpenAPI/Swagger.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .version("1.0.0")
                        .description("""
                                Платёжный сервис с поддержкой:
                                - **Идемпотентности** — защита от дублирования платежей
                                - **Saga Pattern** — распределённые транзакции для переводов
                                - **Kafka Events** — асинхронные события платежей
                                - **gRPC** — межсервисное взаимодействие
                                
                                ## Особенности
                                - Создание и управление платежами
                                - Возвраты (полные и частичные)
                                - Кошельки пользователей
                                - Переводы между кошельками
                                """)
                        .contact(new Contact()
                                .name("Developer")
                                .email("developer@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8095").description("Local Development")
                ));
    }
}
