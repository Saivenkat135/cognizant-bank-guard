# Transaction Service

A Spring Boot application for managing customer transactions and banking operations.

## Project Structure

```
transactionService/
├── src/main/java/com/bankguard/transactionservice/
│   ├── entity/           # JPA Entity classes
│   ├── repository/       # Data Access Layer
│   ├── service/          # Business Logic Layer
│   ├── controller/       # REST API Endpoints
│   ├── dto/              # Data Transfer Objects
│   ├── config/           # Configuration (RestTemplate)
│   └── TransactionServiceApplication.java
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

## Entities

### Customer
- **customer_id** - Primary Key (Auto-generated)
- **bank_name** - Name of the bank
- **balance** - Account balance
- **account_type** - Type of account (Savings, Current, etc.)
- **name** - Customer name
- **email** - Customer email (Unique)
- **account_no** - Account number (Unique)

### Transaction
- **transaction_id** - Primary Key (Auto-generated)
- **amount** - Transaction amount
- **location** - Transaction location
- **ip_address** - IP address of transaction origin
- **transaction_time** - Date and time of transaction
- **risk_score** - Risk score for the transaction
- **receiver_account_number** - Receiver's account number
- **customer_id** - Foreign Key (Customer)

## API Endpoints

### Customer Endpoints
- `POST /api/customers` - Create a new customer
- `GET /api/customers` - Get all customers
- `GET /api/customers/{customerId}` - Get customer by ID
- `PUT /api/customers/{customerId}` - Update customer
- `DELETE /api/customers/{customerId}` - Delete customer
- `GET /api/customers/email/{email}` - Get customer by email
- `GET /api/customers/account/{accountNo}` - Get customer by account number

### Transaction Endpoints
- `POST /api/transactions` - Create a new transaction
- `GET /api/transactions` - Get all transactions
- `GET /api/transactions/{transactionId}` - Get transaction by ID
- `PUT /api/transactions/{transactionId}` - Update transaction
- `DELETE /api/transactions/{transactionId}` - Delete transaction
- `GET /api/transactions/customer/{customerId}` - Get transactions by customer
- `GET /api/transactions/receiver/{receiverAccountNumber}` - Get transactions by receiver account

## Microservice Architecture

### Transaction Service <-> Enrichment Service Integration

The Transaction Service integrates with the **Enrichment Service** microservice to validate and enrich transaction data:

```
┌─────────────────────────────┐
│  Transaction Service        │
│  (Port 8080)                │
└──────────────┬──────────────┘
               │ POST /api/enrich/transaction
               │ (Current Transaction + Customer + Last 5 Transactions)
               ▼
┌─────────────────────────────┐
│  Enrichment Service         │
│  (Port 8081)                │
│                             │
│  - Validates data           │
│  - Removes sensitive fields │
│  - Calculates metrics       │
│  - Enriches transaction     │
└─────────────────────────────┘
```

### How to Use Enrichment Service

The `TransactionEnrichmentIntegrationService` handles all communication with the Enrichment Service:

```java
@Autowired
private TransactionEnrichmentIntegrationService enrichmentService;

// Enrich a transaction
Object enrichedTransaction = enrichmentService
    .enrichTransactionWithService(transaction, customer);
```

### Features
- ✅ Validates transaction amount
- ✅ Validates customer balance
- ✅ Validates IP address format
- ✅ Removes sensitive fields (receiver account number)
- ✅ Includes customer profile in enriched data
- ✅ Calculates previous transaction metrics (count, average amount)
- ✅ Returns max 5 previous transactions

## Prerequisites

- Java 17 or higher
- MySQL Server
- Maven 3.6+
- **Enrichment Service** running on `http://localhost:8081` (optional but recommended)

## Database Setup

1. Create a MySQL database:
```sql
CREATE DATABASE bankguard_db;
```

2. Update the database credentials in `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bankguard_db
spring.datasource.username=root
spring.datasource.password=your_password
```

## Building the Project

```bash
mvn clean install
```

## Running the Application

### Start Transaction Service
```bash
cd transactionService
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Start Enrichment Service (Optional but Recommended)
```bash
cd enrichmentService
mvn spring-boot:run
```

The enrichment service will start on `http://localhost:8081`

**Note:** Make sure you start the Enrichment Service if you want to use the transaction enrichment features.

## Testing with cURL

### Create a Customer
```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "bankName": "State Bank",
    "balance": 50000,
    "accountType": "Savings",
    "name": "John Doe",
    "email": "john@example.com",
    "accountNo": "1234567890"
  }'
```

### Get All Customers
```bash
curl http://localhost:8080/api/customers
```

### Create a Transaction
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000,
    "location": "NYC",
    "ipAddress": "192.168.1.1",
    "time": "2024-04-09T10:30:00",
    "riskScore": 0.2,
    "receiverAccountNumber": "9876543210",
    "customerId": 1
  }'
```

### Get All Transactions
```bash
curl http://localhost:8080/api/transactions
```

## Dependencies

- Spring Boot 3.2.4
- Spring Data JPA
- MySQL Connector
- Lombok
- Spring Boot Devtools

## Author

Bank Guard Team
