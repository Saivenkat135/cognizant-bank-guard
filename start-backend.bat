@echo off
REM Start all backend services in sequence

REM 1. Start Eureka Server
start "EurekaServer" cmd /k "cd Backend\EurekaServer && mvnw spring-boot:run"

REM 2. Start Config Server
start "ConfigServer" cmd /k "cd Backend\ConfigServer && mvnw spring-boot:run"

REM 3. Start Transaction Service
start "TransactionService" cmd /k "cd Backend\transactionService && mvnw spring-boot:run"

REM 4. Start Enrichment Service
start "EnrichmentService" cmd /k "cd Backend\enrichmentService && mvnw spring-boot:run"

REM 5. Start Decision Engine Service
start "DecisionEngineService" cmd /k "cd Backend\Decision_Engine_service && mvnw spring-boot:run"

REM 6. Start Alert Case Service
start "AlertCaseService" cmd /k "cd Backend\AlertCaseService && mvnw spring-boot:run"

REM 7. Start SAR Report Service
start "SarReportService" cmd /k "cd Backend\sarReport && mvnw spring-boot:run"

REM 8. Start API Gateway
start "ApiGateway" cmd /k "cd Backend\apiGateway && mvnw spring-boot:run"

REM All services started in separate windows
