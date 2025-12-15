# Этап сборки
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Копируем файлы Maven
COPY pom.xml .
COPY src ./src

# Сборка приложения
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Этап запуска
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Создаём пользователя для безопасности
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Копируем jar из этапа сборки
COPY --from=builder /app/target/payment-service-1.0.0.jar app.jar

# Настройки JVM для контейнера
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Переключаемся на непривилегированного пользователя
USER appuser

# Порты: HTTP и gRPC
EXPOSE 8095 9095

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8095/actuator/health || exit 1

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
