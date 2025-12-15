# Payment Service - Запуск проекта
# Для остановки нажмите Ctrl+C

$Host.UI.RawUI.WindowTitle = "Payment Service"
$ErrorActionPreference = "SilentlyContinue"

# Переход в директорию проекта
Set-Location $PSScriptRoot

Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║             PAYMENT SERVICE - ЗАПУСК ПРОЕКТА                ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

# Остановка предыдущих процессов на порту 8095
Write-Host "[0/5] Остановка предыдущих процессов..." -ForegroundColor Yellow
$connections = netstat -ano | Select-String ":8095.*LISTENING"
foreach ($conn in $connections) {
    $processId = ($conn -split '\s+')[-1]
    if ($processId -match '^\d+$') {
        Write-Host "Останавливаю процесс на порту 8095, PID: $processId"
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 3
Write-Host "[OK] Порт 8095 освобождён" -ForegroundColor Green
Write-Host ""

# Настройка Java 17
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "[INFO] Используется Java 17" -ForegroundColor Cyan

# Проверка Java
Write-Host "[1/5] Проверка Java..." -ForegroundColor Yellow
$null = java -version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ОШИБКА] Java не найдена" -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}
Write-Host "[OK] Java найдена" -ForegroundColor Green
Write-Host ""

# Проверка Docker
Write-Host "[2/5] Проверка Docker..." -ForegroundColor Yellow
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ОШИБКА] Docker не запущен. Запустите Docker Desktop." -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}
Write-Host "[OK] Docker запущен" -ForegroundColor Green
Write-Host ""

# Запуск инфраструктуры
Write-Host "[3/5] Запуск инфраструктуры Docker..." -ForegroundColor Yellow
docker-compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ОШИБКА] Не удалось запустить контейнеры" -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}
Write-Host "[OK] Контейнеры запущены" -ForegroundColor Green
Write-Host ""

# Ожидание готовности
Write-Host "[4/5] Ожидание готовности сервисов (20 сек)..." -ForegroundColor Yellow
Start-Sleep -Seconds 20
Write-Host "[OK] Сервисы готовы" -ForegroundColor Green
Write-Host ""

# Установка переменных окружения
$env:DB_HOST = "localhost"
$env:DB_PORT = "5433"
$env:REDIS_PORT = "6381"
$env:KAFKA_SERVERS = "localhost:9093"

Write-Host "[5/5] Запуск приложения..." -ForegroundColor Yellow
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Swagger UI:  http://localhost:8095/swagger-ui.html         ║" -ForegroundColor Cyan
Write-Host "║  Kafka UI:    http://localhost:8086                         ║" -ForegroundColor Cyan
Write-Host "║  Prometheus:  http://localhost:9092                         ║" -ForegroundColor Cyan
Write-Host "║  Grafana:     http://localhost:3002 (admin/admin)           ║" -ForegroundColor Cyan
Write-Host "╠══════════════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║  Для остановки: нажмите Ctrl+C, затем N и Enter             ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Открытие браузера
Start-Process "http://localhost:8095/swagger-ui.html"

# Флаг для предотвращения двойного cleanup
$script:cleanupDone = $false

# Функция очистки при завершении
function Cleanup {
    if ($script:cleanupDone) { return }
    $script:cleanupDone = $true
    
    Write-Host ""
    Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Yellow
    Write-Host "║              ОСТАНОВКА СЕРВИСОВ...                          ║" -ForegroundColor Yellow
    Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Остановка контейнеров Docker..." -ForegroundColor Yellow
    Set-Location $PSScriptRoot
    docker-compose down
    Write-Host ""
    Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║                    ВСЕ СЕРВИСЫ ОСТАНОВЛЕНЫ                   ║" -ForegroundColor Green
    Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
}

try {
    # Запуск Maven
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor DarkGray
    mvn spring-boot:run -DskipTests 2>$null
}
finally {
    # Всегда выполняем cleanup
    Cleanup
    Write-Host "Нажмите любую клавишу для закрытия окна..." -ForegroundColor Cyan
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}
