# Enrichment Service

A Spring Boot microservice that enriches and validates transaction data with customer profile information and historical transaction metrics.

## Overview

The Enrichment Service receives transaction details from the Transaction Service along with:
- Current transaction details
- Customer profile information
- Last 5 previous transactions (or available transactions)

The service then:
1. Validates the transaction data
2. Removes unwanted fields (receiver's account number)
3. Enriches the transaction with customer information
4. Calculates metrics from previous transactions
5. Returns validated and enriched transaction data

## Project Structure

```
enrichmentService/
├── src/main/java/com/bankguard/enrichmentservice/
│   ├── dto/                # Data Transfer Objects
│   │   ├── TransactionDTO.java
│   │   ├── CustomerDTO.java
│   │   ├── EnrichmentRequest.java
│   │   └── EnrichedTransactionDTO.java
│   ├── service/            # Business Logic
│   │   └── EnrichmentService.java
│   ├── controller/         # REST API Endpoints
│   │   └── EnrichmentController.java
│   └── EnrichmentServiceApplication.java
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

## DTOs

### EnrichmentRequest
Contains:
- `currentTransaction`: TransactionDTO - details of the current transaction
- `customer`: CustomerDTO - customer profile
- `previousTransactions`: List of TransactionDTO - last 5 transactions

### EnrichedTransactionDTO
Contains enriched transaction data:
- `transactionId`: Long
- `amount`: Double
- `location`: String
- `time`: LocalDateTime
- `riskScore`: Double
- `customerId`: Long
- `customerName`: String (from Customer)
- `customerEmail`: String (from Customer)
- `customerAccountNo`: String (from Customer)
- `customerBalance`: Double (from Customer)
- `previousTransactionCount`: Integer
- `previousTransactionAverageAmount`: Double

**Note:** `receiverAccountNumber` is intentionally excluded for security

## API Endpoints

### 1. Enrich Transaction
**POST** `/api/enrich/transaction`

Enriches transaction data with customer profile and historical metrics.

**Request Body:**
```json
{
  "currentTransaction": {
    "transactionId": 1,
    "amount": 5000,
    "location": "NYC",
    "ipAddress": "192.168.1.1",
    "time": "2024-04-09T10:30:00",
    "riskScore": 0.2,
    "receiverAccountNumber": "9876543210",
    "customerId": 1
  },
  "customer": {
    "customerId": 1,
    "bankName": "State Bank",
    "balance": 50000,
    "accountType": "Savings",
    "name": "John Doe",
    "email": "john@example.com",
    "accountNo": "1234567890"
  },
  "previousTransactions": [
    {
      "transactionId": 5,
      "amount": 1000,
      "location": "LA",
      "ipAddress": "192.168.1.5",
      "time": "2024-04-08T14:00:00",
      "riskScore": 0.1,
      "customerId": 1
    }
  ]
}
```

**Response:**
```json
{
  "transactionId": 1,
  "amount": 5000,
  "location": "NYC",
  "time": "2024-04-09T10:30:00",
  "riskScore": 0.2,
  "customerId": 1,
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "customerAccountNo": "1234567890",
  "customerBalance": 50000,
  "previousTransactionCount": 1,
  "previousTransactionAverageAmount": 1000.0
}
```

### 2. Validate Transaction Amount
**POST** `/api/enrich/validate/amount?amount=5000`

Validates if transaction amount is valid (> 0).

### 3. Validate Sufficient Balance
**POST** `/api/enrich/validate/balance?customerBalance=50000&transactionAmount=5000`

Validates if customer has sufficient balance for the transaction.

### 4. Validate IP Address
**POST** `/api/enrich/validate/ip?ipAddress=192.168.1.1`

Validates if IP address format is correct.

### 5. Health Check
**GET** `/api/enrich/health`

Returns service status.

## Configuration

### application.properties
```properties
spring.application.name=enrichmentService
server.port=8081

# Logging Level
logging.level.root=INFO
logging.level.com.bankguard.enrichmentservice=DEBUG
```

## Prerequisites

- Java 17 or higher
- Spring Boot 3.2.4
- Maven 3.6+

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

## Integration with Transaction Service

The Transaction Service calls the Enrichment Service using `TransactionEnrichmentIntegrationService`:

1. **Before creating a transaction**, call enrichment service to validate data
2. **After transaction creation**, send enriched data for fraud detection
3. The Enrichment Service removes sensitive fields like `receiverAccountNumber`

### How the Integration Works

```
Transaction Service Request
    ↓
Collects: Current Transaction + Customer Profile + Last 5 Transactions
    ↓
Calls: POST /api/enrich/transaction
    ↓
Enrichment Service
    ↓
Validates, Removes Sensitive Fields, Calculates Metrics
    ↓
Returns Enriched Transaction
    ↓
Transaction Service uses enriched data for further processing
```

## Key Features

✅ **Data Validation**
- Transaction amount validation
- Customer balance validation
- IP address format validation

✅ **Data Security**
- Removes sensitive fields (receiver account number)
- Only exposes necessary information

✅ **Transaction Metrics**
- Calculates average amount from previous transactions
- Counts previous transactions
- Enables fraud detection patterns

✅ **Customer Enrichment**
- Includes customer profile in response
- Provides context for risk score calculation

## Testing with cURL

### Test Enrichment Request
```bash
curl -X POST http://localhost:8081/api/enrich/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "currentTransaction": {
      "transactionId": 1,
      "amount": 5000,
      "location": "NYC",
      "ipAddress": "192.168.1.1",
      "time": "2024-04-09T10:30:00",
      "riskScore": 0.2,
      "receiverAccountNumber": "9876543210",
      "customerId": 1
    },
    "customer": {
      "customerId": 1,
      "bankName": "State Bank",
      "balance": 50000,
      "accountType": "Savings",
      "name": "John Doe",
      "email": "john@example.com",
      "accountNo": "1234567890"
    },
    "previousTransactions": []
  }'
```

### Health Check
```bash
curl http://localhost:8081/api/enrich/health
```

## Dependencies

- Spring Boot 3.2.4
- Lombok
- Jackson
- Spring Boot Devtools

## Error Handling

- **400 Bad Request**: Invalid enrichment request data
- **500 Internal Server Error**: Processing error
- Returns appropriate HTTP status codes for validation failures

## Author

Bank Guard Team
