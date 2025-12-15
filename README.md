# 💳 Payment Service

> **Pet-проект для портфолио Middle Java Backend Developer**

Платёжный сервис с поддержкой идемпотентности, Saga Pattern и событийной архитектуры.

---

## 📋 Содержание

- [Архитектура](#-архитектура)
- [Технологии](#-технологии)
- [Быстрый старт](#-быстрый-старт)
- [API Документация](#-api-документация)
- [Особенности](#-особенности)

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                     Payment Service                          │
│                    (Spring Boot 3.2)                         │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┬─────────────┐
        │             │             │             │
        ▼             ▼             ▼             ▼
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│PostgreSQL │  │   Redis   │  │   Kafka   │  │Prometheus │
│ (Data)    │  │  (Cache)  │  │ (Events)  │  │(Metrics)  │
└───────────┘  └───────────┘  └───────────┘  └───────────┘
```

### Ключевые особенности архитектуры:
- **Идемпотентность** — защита от дублирования платежей через Idempotency-Key
- **Saga Pattern** — распределённые транзакции для переводов между кошельками
- **Event-Driven** — асинхронные события платежей через Kafka
- **gRPC Ready** — подготовлено для межсервисного взаимодействия

---

## 🛠️ Технологии

| Категория | Технологии |
|-----------|------------|
| **Backend** | Java 17, Spring Boot 3.2, Spring MVC, Spring Security |
| **База данных** | PostgreSQL 15, Flyway (миграции) |
| **Кэширование** | Redis 7 |
| **Messaging** | Apache Kafka |
| **gRPC** | grpc-spring-boot-starter |
| **Мониторинг** | Prometheus, Grafana, Micrometer |
| **Контейнеризация** | Docker, Docker Compose |
| **Документация** | OpenAPI/Swagger |
| **Тестирование** | JUnit 5, Testcontainers |

---

## ✨ Особенности

### 1. Идемпотентность платежей
```http
POST /api/v1/payments
Idempotency-Key: unique-key-123
```
- Защита от дублирования при повторных запросах
- Хранение результатов в БД с TTL 24 часа
- Автоматическая очистка истёкших записей

### 2. Saga Pattern для переводов
```
Шаг 1: Блокировка суммы у отправителя
Шаг 2: Пополнение кошелька получателя  
Шаг 3: Списание заблокированной суммы

Компенсация при ошибке:
- Разблокировка суммы у отправителя
- Списание с получателя (если было пополнение)
```

### 3. События в Kafka
- `PAYMENT_CREATED` — платёж создан
- `PAYMENT_COMPLETED` — платёж успешен
- `PAYMENT_CANCELLED` — платёж отменён
- `PAYMENT_REFUNDED` — возврат средств
- `PAYMENT_FAILED` — ошибка платежа

---

## 🚀 Быстрый старт

### Требования
- Docker & Docker Compose
- Java 17+
- Maven 3.9+

### Запуск

```bash
# Клонирование репозитория
git clone https://github.com/your-username/payment-service.git
cd payment-service

# Запуск инфраструктуры
docker-compose up -d

# Запуск приложения
mvn spring-boot:run
```

Или используйте скрипт:
```bash
START_PROJECT.bat
```

### Доступные сервисы

| Сервис | URL |
|--------|-----|
| Payment API | http://localhost:8095 |
| Swagger UI | http://localhost:8095/swagger-ui.html |
| Kafka UI | http://localhost:8086 |
| Prometheus | http://localhost:9092 |
| Grafana | http://localhost:3002 (admin/admin) |

---

## 📝 API Документация

### Платежи

#### Создать платёж
```http
POST /api/v1/payments
Idempotency-Key: unique-key-123
Content-Type: application/json

{
  "orderId": "order-001",
  "userId": "user1",
  "amount": 1000.00,
  "currency": "RUB",
  "paymentMethod": "CARD",
  "description": "Оплата заказа"
}
```

#### Подтвердить платёж
```http
POST /api/v1/payments/{paymentId}/confirm
```

#### Отменить платёж
```http
POST /api/v1/payments/{paymentId}/cancel
```

#### Возврат платежа
```http
POST /api/v1/payments/{paymentId}/refund
Content-Type: application/json

{
  "amount": 500.00,
  "reason": "Частичный возврат"
}
```

### Кошельки

#### Создать кошелёк
```http
POST /api/v1/wallets?userId=user1&currency=RUB&initialBalance=10000
```

#### Пополнить кошелёк
```http
POST /api/v1/wallets/{userId}/{currency}/deposit?amount=5000
```

#### Перевод между кошельками (Saga)
```http
POST /api/v1/wallets/transfer
Content-Type: application/json

{
  "fromUserId": "user1",
  "toUserId": "user2",
  "amount": 1000.00,
  "currency": "RUB",
  "description": "Перевод другу"
}
```

---

## 🔧 Конфигурация

### Переменные окружения

```bash
# Database
DB_HOST=localhost
DB_PORT=5433
DB_NAME=payment_db
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6381

# Kafka
KAFKA_SERVERS=localhost:9093
```

---

## 📊 Мониторинг

### Prometheus Metrics
Все метрики доступны по адресу `/actuator/prometheus`:
- JVM метрики
- HTTP запросы (latency, count, errors)
- Kafka producer метрики
- Custom бизнес-метрики

### Grafana Dashboards
- JVM Dashboard
- Spring Boot Dashboard
- Kafka Dashboard

---

## 🧪 Тестирование

```bash
# Unit тесты
mvn test

# Integration тесты с Testcontainers
mvn verify -DskipUnitTests

# Все тесты
mvn verify
```

---

## 📈 Демонстрируемые навыки

| Навык | Реализация |
|-------|------------|
| **Идемпотентность** | Idempotency-Key header, хранение в БД |
| **Saga Pattern** | Распределённые транзакции с компенсацией |
| **Event-Driven** | Kafka события платежей |
| **gRPC** | Подготовлено для межсервисного взаимодействия |
| **Оптимистичная блокировка** | @Version в entities |
| **Пессимистичная блокировка** | FOR UPDATE в кошельках |
| **Flyway миграции** | Версионирование схемы БД |
| **OpenAPI/Swagger** | Документация API |
| **Мониторинг** | Prometheus + Grafana |

---

## 📄 Лицензия

MIT License

## 👤 Автор

Middle Java Backend Developer Portfolio Project
