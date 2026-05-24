# BankGuard Project Overview

## Project Description
BankGuard is a comprehensive microservices-based fraud detection system designed for banking transactions. The system leverages Google's Gemini AI to analyze and determine whether a transaction is genuine or fraudulent, providing real-time protection against financial fraud.

## Architecture Overview
The project follows a microservices architecture with the following components:

1. **API Gateway** - Entry point for all client requests, handles routing and load balancing
2. **Transaction Service** - Manages customer and transaction data
3. **Enrichment Service** - Enriches transaction data and performs initial validations
4. **Decision Engine Service** - Uses Gemini AI for fraud detection analysis
5. **AlertCase Service** - Handles fraud alerts and investigation cases
6. **SAR Report Service** - Manages Suspicious Activity Reports

## Transaction Flow

```
Client Request
      ↓
  API Gateway
      ↓
Transaction Service (Store/Retrieve transaction data)
      ↓
Enrichment Service (Validate & enrich transaction)
      ↓
Decision Engine Service (Gemini AI analysis)
      ↓
Response (Fraud decision)
      ↓
├── AlertCase Service (Generate alerts if fraudulent)
└── SAR Report Service (Generate reports if needed)
```

## Microservices and Their APIs

### 1. API Gateway
- **Purpose**: Routes incoming requests to appropriate microservices
- **Technology**: Spring Cloud Gateway (WebFlux)
- **APIs**: Acts as a proxy, no direct business APIs

### 2. Transaction Service
- **Purpose**: Manages customer profiles and transaction records
- **Technology**: Spring Boot with JPA and MySQL
- **APIs**:
  - `POST /api/customers` - Create customer
  - `GET /api/customers` - Get all customers
  - `GET /api/customers/{customerId}` - Get customer by ID
  - `PUT /api/customers/{customerId}` - Update customer
  - `DELETE /api/customers/{customerId}` - Delete customer
  - `GET /api/customers/email/{email}` - Get customer by email
  - `GET /api/customers/account/{accountNo}` - Get customer by account
  - `POST /api/transactions` - Create transaction
  - `GET /api/transactions` - Get all transactions
  - `GET /api/transactions/{transactionId}` - Get transaction by ID
  - `PUT /api/transactions/{transactionId}` - Update transaction
  - `DELETE /api/transactions/{transactionId}` - Delete transaction
  - `GET /api/transactions/customer/{customerId}` - Get transactions by customer
  - `GET /api/transactions/receiver/{receiverAccountNumber}` - Get transactions by receiver

### 3. Enrichment Service
- **Purpose**: Validates and enriches transaction data before fraud analysis
- **Technology**: Spring Boot with WebFlux
- **APIs**:
  - `POST /api/enrich/transaction/with-decision-and-alert` - Process transaction with full flow
  - `POST /api/enrich/transaction` - Enrich transaction data
  - `POST /api/enrich/transaction/with-decision` - Enrich and get decision
  - `POST /api/enrich/validate/amount` - Validate transaction amount
  - `POST /api/enrich/validate/balance` - Validate account balance
  - `POST /api/enrich/validate/ip` - Validate IP address

### 4. Decision Engine Service
- **Purpose**: Uses Google's Gemini AI to analyze transactions for fraud detection
- **Technology**: Spring Boot
- **APIs**:
  - `POST /api/gemini/analyze-transaction` - Analyze transaction using Gemini AI

### 5. AlertCase Service
- **Purpose**: Manages fraud alerts and investigation cases
- **Technology**: Spring Boot with JPA
- **APIs**:
  - `POST /api/investigation/ingest-fraud-alert` - Ingest fraud alert
  - `POST /api/investigation/ingest` - Ingest investigation data

### 6. SAR Report Service
- **Purpose**: Generates and manages Suspicious Activity Reports
- **Technology**: Spring Boot with JPA and MySQL
- **APIs**:
  - `POST /sar/ingest-report` - Ingest SAR report
  - `POST /sar/report` - Create SAR report
  - `GET /sar/reports` - Get all reports
  - `GET /sar/report/id/{sarId}` - Get report by ID
  - `GET /sar/report/name/{customerName}` - Get reports by customer name
  - `GET /sar/report/account/{customerAccountNo}` - Get reports by account
  - `GET /sar/report/status/{status}` - Get reports by status
  - `GET /sar/report/transaction/{transactionId}` - Get reports by transaction
  - `GET /sar/report/city/{city}` - Get reports by city
  - `GET /sar/report/state/{state}` - Get reports by state

## Key Features

- **AI-Powered Fraud Detection**: Utilizes Google's Gemini AI for intelligent transaction analysis
- **Microservices Architecture**: Scalable and maintainable service-oriented design
- **Real-time Processing**: End-to-end transaction processing with immediate fraud detection
- **Comprehensive Validation**: Multi-layer validation including amount, balance, and IP checks
- **Alert Management**: Automated alert generation for suspicious activities
- **Regulatory Compliance**: SAR report generation for compliance requirements
- **Database Integration**: MySQL databases for persistent data storage

## Technology Stack

- **Backend**: Spring Boot, Spring Cloud Gateway
- **AI Integration**: Google Gemini API
- **Database**: MySQL
- **Communication**: REST APIs, WebFlux for reactive programming
- **Build Tool**: Maven
- **Language**: Java 17/21</content>
<parameter name="filePath">c:\Users\2485162\Documents\CTS_Bankguard-main 1\CTS_Bankguard-main\OverallProjectOverview.md