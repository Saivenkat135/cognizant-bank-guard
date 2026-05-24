# BankGuard Backend API Documentation

**Last Updated**: May 7, 2026  
**API Version**: 1.0.0  
**Status**: Production Ready

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Services](#architecture--services)
3. [Authentication & Authorization](#authentication--authorization)
4. [Role-Based Access Control (RBAC)](#role-based-access-control-rbac)
5. [API Endpoints](#api-endpoints)
6. [Data Models & DTOs](#data-models--dtos)
7. [Error Handling](#error-handling)
8. [Service Integration Flow](#service-integration-flow)
9. [Configuration & Environment](#configuration--environment)
10. [Important Notes for Frontend Development](#important-notes-for-frontend-development)

---

## Project Overview

**BankGuard** is a comprehensive **microservices-based fraud detection system** designed for banking transactions. The system leverages **Google's Gemini AI** to analyze and determine whether a transaction is genuine or fraudulent, providing real-time protection against financial fraud.

### Key Features
- Real-time fraud detection using AI
- Role-based access control for different user types
- Multi-stage transaction validation (enrichment → decision engine → alerts)
- Suspicious Activity Reports (SAR) generation
- Investigation case management
- JWT-based authentication
- Microservices architecture with service-to-service communication

### Transaction Decision States
- **genuine**: Transaction is legitimate
- **flagged**: Transaction is marked for fraud investigation
- **terminated**: Transaction is blocked/cancelled

---

## Architecture & Services

### Microservices Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Port 1001)                 │
│                    - JWT Authentication Filter                  │
│                    - Request Routing & Load Balancing           │
│                    - Security & RBAC Enforcement                │
└─────────────────────────────────────────────────────────────────┘
                                 ↓
    ┌─────────────────────────────────────────────────────────────┐
    │           TRANSACTION SERVICE (Port 8089)                    │
    │  - Creates/retrieves transaction & customer data             │
    │  - Manages customer profiles & risk scores                   │
    │  - Primary data persistence layer                            │
    └─────────────────────────────────────────────────────────────┘
                                 ↓
    ┌─────────────────────────────────────────────────────────────┐
    │        ENRICHMENT SERVICE (Port 8010)                        │
    │  - Validates & enriches transaction data                     │
    │  - Fetches customer history & previous transactions          │
    │  - Communicates with Decision Engine                         │
    │  - Generates fraud alerts to AlertCase Service               │
    └─────────────────────────────────────────────────────────────┘
                    ├──────────────────┤
                    ↓                  ↓
        ┌──────────────────┐  ┌──────────────────────────┐
        │ DECISION ENGINE  │  │  ALERT CASE SERVICE      │
        │ (Gemini AI)      │  │  - Case Management       │
        │ (Port 8071)      │  │  - Alert Investigation   │
        └──────────────────┘  │  - Case Status Updates   │
                              │  (Port 8092)             │
                              └──────────────────────────┘
                                        ↓
                              ┌──────────────────────────┐
                              │   SAR REPORT SERVICE     │
                              │  - Report Generation     │
                              │  - Report Management     │
                              │  (Port 8079)             │
                              └──────────────────────────┘
```

### Service Communication Flow

```
1. CLIENT REQUEST → API GATEWAY (Port 1001)
                ↓
2. AUTH CHECK → Extract JWT token, validate, extract role
                ↓
3. TRANSACTION & CUSTOMER SERVICE (Port 8089)
   - Validate customer exists
   - Create transaction entry
   - Calculate initial risk score
                ↓
4. ENRICHMENT SERVICE (Port 8010)
   - Enrich transaction with historical data
   - Validate amount, balance, IP address
   - Call Decision Engine (Gemini AI)
                ↓
5. DECISION ENGINE / GEMINI SERVICE (Port 8071)
   - Analyze transaction using AI
   - Generate risk score & fraud decision
   - Return decision: genuine/flagged/terminated
                ↓
6. IF FLAGGED → ALERT CASE SERVICE (Port 8092)
   - Create investigation case
   - Generate fraud alert
                ↓
7. IF ALERT CREATED → SAR REPORT SERVICE (Port 8079)
   - Generate Suspicious Activity Report
   - Store report for audit trail
                ↓
8. RESPONSE → CLIENT
   - Return transaction decision
   - Include risk score & reasoning
```

---

## Authentication & Authorization

### JWT Token Structure

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "username",           // Username (subject)
  "role": "SUPER_ADMIN",       // User role
  "iat": 1704067200,           // Issued at (timestamp)
  "exp": 1704153600            // Expiration (24 hours later)
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

### JWT Configuration

```properties
jwt.secret=BankGuardSuperSecretKeyForJWT2025ThisMustBeLongEnoughForHS256Algorithm
jwt.expiration=86400000  # 24 hours in milliseconds
```

### How to Use JWT

#### Step 1: Register User
```http
POST /auth/register
Content-Type: application/json

{
  "username": "admin_user",
  "password": "SecurePassword123",
  "role": "SUPER_ADMIN"
}
```

**Response:**
```json
{
  "username": "admin_user",
  "role": "SUPER_ADMIN",
  "message": "User registered successfully"
}
```

#### Step 2: Login & Get Token
```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin_user",
  "password": "SecurePassword123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin_user",
  "role": "SUPER_ADMIN",
  "message": "Login successful"
}
```

#### Step 3: Use Token in Requests
```http
GET /api/customers
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Token Extraction on Frontend:**
```javascript
// After login
const token = response.data.token;
localStorage.setItem('token', token);

// In axios interceptor
const token = localStorage.getItem('token');
if (token) {
  config.headers.Authorization = `Bearer ${token}`;
}
```

---

## Role-Based Access Control (RBAC)

### Available Roles

#### 1. **SUPER_ADMIN**
- **Full system access**
- Can perform all CRUD operations
- Access to all endpoints
- Can manage users and roles
- Can view all sensitive data

#### 2. **FRAUD_ANALYST**
- **Investigation & Analysis Focus**
- View transactions (read-only)
- View customer information (read-only)
- Access fraud alerts (read)
- Access SAR reports (read-only)
- Update investigation case status
- Cannot modify transactions or customer data

#### 3. **RISK_MANAGER**
- **Risk Assessment & Rule Management**
- View transactions (read-only)
- View customer information (read-only)
- Define enrichment rules & thresholds
- Define decision engine rules & risk policies
- Cannot view alerts or investigation cases
- No transaction modification access

### RBAC Matrix

| Endpoint | GET | POST | PUT | DELETE | SUPER_ADMIN | FRAUD_ANALYST | RISK_MANAGER |
|----------|-----|------|-----|--------|-------------|---------------|--------------|
| `/api/customers/**` | ✓ | ✓ | ✓ | ✓ | ✅ | 🔍 | 🔍 |
| `/api/transactions/**` | ✓ | ✓ | ✓ | ✓ | ✅ | 🔍 | 🔍 |
| `/api/investigation/**` | ✓ | ✓ | ✓ | - | ✅ | ✅* | ❌ |
| `/sar/**` | ✓ | ✓ | - | - | ✅ | 🔍 | ❌ |
| `/api/enrich/**` | ✓ | ✓ | ✓ | - | ✅ | ❌ | ✅* |
| `/api/gemini/**` | ✓ | ✓ | ✓ | - | ✅ | ❌ | ✅* |

**Legend:**
- ✅ = Full access (all HTTP methods allowed)
- 🔍 = Read-only (GET only)
- ✅* = Limited access (specific methods)
- ❌ = No access

### Detailed RBAC Rules

#### Transaction Service (`/api/transactions/**` and `/api/customers/**`)
```
GET (READ):         SUPER_ADMIN | FRAUD_ANALYST | RISK_MANAGER
POST (CREATE):      SUPER_ADMIN
PUT (UPDATE):       SUPER_ADMIN
DELETE:             SUPER_ADMIN
```

#### Alert & Investigation Service (`/api/investigation/**`)
```
GET (READ):         SUPER_ADMIN | FRAUD_ANALYST
PUT UPDATE STATUS:  SUPER_ADMIN | FRAUD_ANALYST (only case status updates)
POST (CREATE):      SUPER_ADMIN
```

#### SAR Report Service (`/sar/**`)
```
GET (READ):         SUPER_ADMIN | FRAUD_ANALYST
POST (CREATE):      SUPER_ADMIN
```

#### Enrichment Service (`/api/enrich/**`)
```
All Methods:        SUPER_ADMIN | RISK_MANAGER
```

#### Decision Engine / Gemini (`/api/gemini/**`)
```
All Methods:        SUPER_ADMIN | RISK_MANAGER
```

---

## API Endpoints

### Base URL
```
http://localhost:1001
```

### 1. Authentication Endpoints (`/auth`)

#### 1.1 Register User
```http
POST /auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123",
  "role": "FRAUD_ANALYST"
}
```

**Allowed Roles**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (201 Created)**:
```json
{
  "username": "john_doe",
  "role": "FRAUD_ANALYST",
  "message": "User registered successfully"
}
```

**Error Response (409 Conflict)**:
```json
{
  "message": "Username already exists"
}
```

**Error Response (400 Bad Request)**:
```json
{
  "message": "Invalid role. Allowed roles: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER"
}
```

---

#### 1.2 Login
```http
POST /auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123"
}
```

**Success Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsInJvbGUiOiJGUkFVRF9BTkFMWVNUIiwiaWF0IjoxNzE0MDY3MjAwLCJleHAiOjE3MTQxNTM2MDB9.xK2qUU8q_...",
  "username": "john_doe",
  "role": "FRAUD_ANALYST",
  "message": "Login successful"
}
```

**Error Response (401 Unauthorized)**:
```json
{
  "message": "Invalid username or password"
}
```

---

### 2. Customer Service Endpoints (`/api/customers`)

#### 2.1 Create Customer
```http
POST /api/customers
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "John Smith",
  "email": "john.smith@example.com",
  "bankName": "Chase Bank",
  "accountNo": "ACC123456789",
  "accountType": "Savings",
  "balance": 50000.00,
  "riskScore": 0.0
}
```

**Required Role**: SUPER_ADMIN

**Success Response (201 Created)**:
```json
{
  "customerId": 1,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "bankName": "Chase Bank",
  "accountNo": "ACC123456789",
  "accountType": "Savings",
  "balance": 50000.00,
  "riskScore": 0.0
}
```

---

#### 2.2 Get All Customers
```http
GET /api/customers
Authorization: Bearer {token}
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
[
  {
    "customerId": 1,
    "name": "John Smith",
    "email": "john.smith@example.com",
    "bankName": "Chase Bank",
    "accountNo": "ACC123456789",
    "accountType": "Savings",
    "balance": 50000.00,
    "riskScore": 0.0
  },
  {
    "customerId": 2,
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "bankName": "Bank of America",
    "accountNo": "ACC987654321",
    "accountType": "Checking",
    "balance": 25000.00,
    "riskScore": 15.5
  }
]
```

---

#### 2.3 Get Customer by ID
```http
GET /api/customers/{customerId}
Authorization: Bearer {token}

Example: GET /api/customers/1
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
{
  "customerId": 1,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "bankName": "Chase Bank",
  "accountNo": "ACC123456789",
  "accountType": "Savings",
  "balance": 50000.00,
  "riskScore": 0.0
}
```

**Error Response (404 Not Found)**:
```
204 No Content
```

---

#### 2.4 Update Customer
```http
PUT /api/customers/{customerId}
Authorization: Bearer {token}
Content-Type: application/json

Example: PUT /api/customers/1

{
  "name": "John Smith Updated",
  "balance": 55000.00,
  "riskScore": 5.0
}
```

**Required Role**: SUPER_ADMIN

**Success Response (200 OK)**:
```json
{
  "customerId": 1,
  "name": "John Smith Updated",
  "email": "john.smith@example.com",
  "bankName": "Chase Bank",
  "accountNo": "ACC123456789",
  "accountType": "Savings",
  "balance": 55000.00,
  "riskScore": 5.0
}
```

---

#### 2.5 Delete Customer
```http
DELETE /api/customers/{customerId}
Authorization: Bearer {token}

Example: DELETE /api/customers/1
```

**Required Role**: SUPER_ADMIN

**Success Response (200 OK)**:
```json
{
  "message": "Customer deleted successfully"
}
```

**Error Response (404 Not Found)**:
```json
{
  "message": "Customer not found"
}
```

---

#### 2.6 Get Customer by Email
```http
GET /api/customers/email/{email}
Authorization: Bearer {token}

Example: GET /api/customers/email/john.smith@example.com
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
{
  "customerId": 1,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "bankName": "Chase Bank",
  "accountNo": "ACC123456789",
  "accountType": "Savings",
  "balance": 50000.00,
  "riskScore": 0.0
}
```

---

#### 2.7 Get Customer by Account Number
```http
GET /api/customers/account/{accountNo}
Authorization: Bearer {token}

Example: GET /api/customers/account/ACC123456789
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
{
  "customerId": 1,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "bankName": "Chase Bank",
  "accountNo": "ACC123456789",
  "accountType": "Savings",
  "balance": 50000.00,
  "riskScore": 0.0
}
```

---

### 3. Transaction Service Endpoints (`/api/transactions`)

#### 3.1 Create Transaction (Full Flow)
```http
POST /api/transactions
Authorization: Bearer {token}
Content-Type: application/json

{
  "customerId": 1,
  "amount": 5000.00,
  "receiverAccountNumber": "REC987654321",
  "city": "New York",
  "state": "NY",
  "ipAddress": "192.168.1.1"
}
```

**Required Role**: SUPER_ADMIN

**Process Flow**:
1. Validates sender customer exists
2. Creates transaction entry
3. Calls Enrichment Service for validation
4. Calls Decision Engine (Gemini AI) for fraud analysis
5. Updates customer risk score
6. If flagged → Creates alert in AlertCase Service
7. If alert created → Generates SAR Report

**Success Response (201 Created)**:
```json
{
  "status": "SUCCESS",
  "transaction": {
    "transactionId": 1001,
    "amount": 5000.00,
    "city": "New York",
    "state": "NY",
    "ipAddress": "192.168.1.1",
    "receiverAccountNumber": "REC987654321",
    "customerId": 1,
    "time": "2025-05-07T10:30:00",
    "status": "genuine",
    "reason": "Transaction analysis completed successfully",
    "riskScore": 12.5
  }
}
```

**Error Response (400 Bad Request)**:
```json
{
  "status": "ERROR",
  "errorCode": "INVALID_REQUEST",
  "message": "Sender customer not found with ID: 1"
}
```

**Error Response (404 Not Found)**:
```json
{
  "status": "ERROR",
  "errorCode": "RECEIVER_NOT_FOUND",
  "message": "Receiver account not found",
  "receiverAccountNumber": "REC987654321"
}
```

**Error Response (500 Internal Server Error)**:
```json
{
  "status": "ERROR",
  "errorCode": "INTERNAL_ERROR",
  "message": "Internal server error occurred"
}
```

---

#### 3.2 Get All Transactions
```http
GET /api/transactions
Authorization: Bearer {token}
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
[
  {
    "transactionId": 1001,
    "amount": 5000.00,
    "city": "New York",
    "state": "NY",
    "ipAddress": "192.168.1.1",
    "receiverAccountNumber": "REC987654321",
    "customerId": 1,
    "time": "2025-05-07T10:30:00",
    "status": "genuine",
    "reason": "Transaction analysis completed successfully",
    "riskScore": 12.5
  },
  {
    "transactionId": 1002,
    "amount": 25000.00,
    "city": "Los Angeles",
    "state": "CA",
    "ipAddress": "192.168.2.2",
    "receiverAccountNumber": "REC111111111",
    "customerId": 2,
    "time": "2025-05-07T11:45:00",
    "status": "flagged",
    "reason": "High-risk transaction detected: Large amount, unusual location",
    "riskScore": 85.0
  }
]
```

---

#### 3.3 Get Transaction by ID
```http
GET /api/transactions/{transactionId}
Authorization: Bearer {token}

Example: GET /api/transactions/1001
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
{
  "transactionId": 1001,
  "amount": 5000.00,
  "city": "New York",
  "state": "NY",
  "ipAddress": "192.168.1.1",
  "receiverAccountNumber": "REC987654321",
  "customerId": 1,
  "time": "2025-05-07T10:30:00",
  "status": "genuine",
  "reason": "Transaction analysis completed successfully",
  "riskScore": 12.5
}
```

---

#### 3.4 Update Transaction
```http
PUT /api/transactions/{transactionId}
Authorization: Bearer {token}
Content-Type: application/json

Example: PUT /api/transactions/1001

{
  "status": "genuine",
  "reason": "Updated reason after manual review"
}
```

**Required Role**: SUPER_ADMIN

**Success Response (200 OK)**:
```json
{
  "transactionId": 1001,
  "amount": 5000.00,
  "city": "New York",
  "state": "NY",
  "ipAddress": "192.168.1.1",
  "receiverAccountNumber": "REC987654321",
  "customerId": 1,
  "time": "2025-05-07T10:30:00",
  "status": "genuine",
  "reason": "Updated reason after manual review",
  "riskScore": 12.5
}
```

---

#### 3.5 Delete Transaction
```http
DELETE /api/transactions/{transactionId}
Authorization: Bearer {token}

Example: DELETE /api/transactions/1001
```

**Required Role**: SUPER_ADMIN

**Success Response (204 No Content)**: Empty response

---

#### 3.6 Get Transactions by Customer
```http
GET /api/transactions/customer/{customerId}
Authorization: Bearer {token}

Example: GET /api/transactions/customer/1
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
[
  {
    "transactionId": 1001,
    "amount": 5000.00,
    "city": "New York",
    "state": "NY",
    "ipAddress": "192.168.1.1",
    "receiverAccountNumber": "REC987654321",
    "customerId": 1,
    "time": "2025-05-07T10:30:00",
    "status": "genuine",
    "reason": "Transaction analysis completed successfully",
    "riskScore": 12.5
  }
]
```

---

#### 3.7 Get Transactions by Receiver Account
```http
GET /api/transactions/receiver/{receiverAccountNumber}
Authorization: Bearer {token}

Example: GET /api/transactions/receiver/REC987654321
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER

**Success Response (200 OK)**:
```json
[
  {
    "transactionId": 1001,
    "amount": 5000.00,
    "city": "New York",
    "state": "NY",
    "ipAddress": "192.168.1.1",
    "receiverAccountNumber": "REC987654321",
    "customerId": 1,
    "time": "2025-05-07T10:30:00",
    "status": "genuine",
    "reason": "Transaction analysis completed successfully",
    "riskScore": 12.5
  }
]
```

---

### 4. Enrichment Service Endpoints (`/api/enrich`)

#### 4.1 Enrich Transaction with Decision and Alert
```http
POST /api/enrich/transaction/with-decision-and-alert
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentTransaction": {
    "amount": 5000.00,
    "city": "New York",
    "state": "NY",
    "ipAddress": "192.168.1.1",
    "receiverAccountNumber": "REC987654321",
    "time": "2025-05-07T10:30:00"
  },
  "customer": {
    "customerId": 1,
    "name": "John Smith",
    "email": "john.smith@example.com",
    "bankName": "Chase Bank",
    "accountNo": "ACC123456789",
    "accountType": "Savings",
    "balance": 50000.00,
    "riskScore": 0.0
  },
  "previousTransactions": [
    {
      "amount": 1000.00,
      "city": "New York",
      "state": "NY",
      "ipAddress": "192.168.1.1",
      "time": "2025-05-06T09:00:00"
    }
  ]
}
```

**Required Role**: SUPER_ADMIN, RISK_MANAGER

**Success Response (200 OK)**:
```json
{
  "amount": 5000.00,
  "city": "New York",
  "state": "NY",
  "time": "2025-05-07T10:30:00",
  "riskScore": 12.5,
  "customerId": 1,
  "customerName": "John Smith",
  "customerEmail": "john.smith@example.com",
  "customerAccountNo": "ACC123456789",
  "customerBalance": 50000.00,
  "decision": "genuine",
  "reason": "Transaction analysis completed successfully",
  "alertSent": false
}
```

---

#### 4.2 Validate Transaction Amount
```http
POST /api/enrich/validate/amount?amount=5000.00
Authorization: Bearer {token}
```

**Required Role**: SUPER_ADMIN, RISK_MANAGER

**Success Response (200 OK)**:
```json
true
```

---

#### 4.3 Validate Account Balance
```http
POST /api/enrich/validate/balance?customerBalance=50000.00&transactionAmount=5000.00
Authorization: Bearer {token}
```

**Required Role**: SUPER_ADMIN, RISK_MANAGER

**Success Response (200 OK)**:
```json
true
```

---

#### 4.4 Validate IP Address
```http
POST /api/enrich/validate/ip?ipAddress=192.168.1.1
Authorization: Bearer {token}
```

**Required Role**: SUPER_ADMIN, RISK_MANAGER

**Success Response (200 OK)**:
```json
true
```

---

### 5. Gemini Decision Engine Endpoints (`/api/gemini`)

#### 5.1 Analyze Transaction with Gemini AI
```http
POST /api/gemini/analyze-transaction
Authorization: Bearer {token}
Content-Type: application/json

{
  "amount": 5000.00,
  "city": "New York",
  "state": "NY",
  "ipAddress": "192.168.1.1",
  "customerName": "John Smith",
  "customerBalance": 50000.00,
  "accountType": "Savings",
  "previousTransactionCount": 50,
  "previousFraudCount": 0,
  "averagePreviousAmount": 2000.00,
  "time": "2025-05-07T10:30:00"
}
```

**Required Role**: SUPER_ADMIN, RISK_MANAGER

**Success Response (200 OK)**:
```json
{
  "decision": "genuine",
  "riskScore": 12.5,
  "reason": "Transaction appears legitimate. Amount is within normal range, customer has good history."
}
```

**Alternative Response (High Risk)**:
```json
{
  "decision": "flagged",
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average. Location is unusual for this customer. Risk score elevated."
}
```

---

### 6. Alert Case Service Endpoints (`/api/investigation`)

#### 6.1 Ingest Fraud Alert
```http
POST /api/investigation/ingest-fraud-alert
Authorization: Bearer {token}
Content-Type: application/json

{
  "decisionStatus": "flagged",
  "geminiRiskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "customerId": 2,
  "customerName": "Jane Doe",
  "customerEmail": "jane.doe@example.com",
  "customerAccountNo": "ACC987654321",
  "customerBalance": 25000.00,
  "city": "Los Angeles",
  "state": "CA",
  "amount": 25000.00,
  "time": "2025-05-07T11:45:00"
}
```

**Required Role**: SUPER_ADMIN

**Success Response (200 OK)**:
```
Empty response (200 OK indicates successful ingestion)
```

---

#### 6.2 Get All Alerts
```http
GET /api/investigation/alerts
Authorization: Bearer {token}
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
[
  {
    "alertId": 1,
    "severity": "HIGH",
    "createdAt": "2025-05-07T11:45:00",
    "decisionStatus": "flagged",
    "riskScore": 85.0,
    "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
    "customerId": 2
  }
]
```

---

#### 6.3 Get Alert by ID
```http
GET /api/investigation/alerts/{alertId}
Authorization: Bearer {token}

Example: GET /api/investigation/alerts/1
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
{
  "alertId": 1,
  "severity": "HIGH",
  "createdAt": "2025-05-07T11:45:00",
  "decisionStatus": "flagged",
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "customerId": 2
}
```

---

#### 6.4 Get Alerts by Severity
```http
GET /api/investigation/alerts/severity/{severity}
Authorization: Bearer {token}

Example: GET /api/investigation/alerts/severity/HIGH
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Severity Levels**: HIGH, MEDIUM, LOW

**Success Response (200 OK)**:
```json
[
  {
    "alertId": 1,
    "severity": "HIGH",
    "createdAt": "2025-05-07T11:45:00",
    "decisionStatus": "flagged",
    "riskScore": 85.0,
    "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
    "customerId": 2
  }
]
```

---

#### 6.5 Get Case by ID
```http
GET /api/investigation/cases/{caseId}
Authorization: Bearer {token}

Example: GET /api/investigation/cases/CASE-001
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
{
  "caseId": "CASE-001",
  "alertId": 1,
  "customerId": 2,
  "status": "OPEN",
  "createdAt": "2025-05-07T11:45:00",
  "riskScore": 85.0,
  "investigator": "fraud.analyst@bank.com"
}
```

---

#### 6.6 Get Cases by Status
```http
GET /api/investigation/cases/status/{status}
Authorization: Bearer {token}

Example: GET /api/investigation/cases/status/OPEN
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Status Values**: OPEN, UNDER_INVESTIGATION, RESOLVED, CLOSED

**Success Response (200 OK)**:
```json
[
  {
    "caseId": "CASE-001",
    "alertId": 1,
    "customerId": 2,
    "status": "OPEN",
    "createdAt": "2025-05-07T11:45:00",
    "riskScore": 85.0,
    "investigator": "fraud.analyst@bank.com"
  }
]
```

---

#### 6.7 Update Case Status
```http
PUT /api/investigation/cases/{caseId}/status?status=UNDER_INVESTIGATION
Authorization: Bearer {token}

Example: PUT /api/investigation/cases/CASE-001/status?status=UNDER_INVESTIGATION
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Status Values**: OPEN, UNDER_INVESTIGATION, RESOLVED, CLOSED

**Success Response (200 OK)**:
```json
{
  "caseId": "CASE-001",
  "alertId": 1,
  "customerId": 2,
  "status": "UNDER_INVESTIGATION",
  "createdAt": "2025-05-07T11:45:00",
  "riskScore": 85.0,
  "investigator": "fraud.analyst@bank.com"
}
```

---

### 7. SAR Report Service Endpoints (`/sar`)

#### 7.1 Ingest Reporting Request
```http
POST /sar/ingest-report
Authorization: Bearer {token}
Content-Type: application/json

{
  "caseId": 1,
  "customerId": 2,
  "status": "OPEN",
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "geminiDecision": "flagged",
  "amount": 25000.00,
  "city": "Los Angeles",
  "state": "CA",
  "customerEmail": "jane.doe@example.com",
  "customerAccountNo": "ACC987654321",
  "customerBalance": 25000.00,
  "customerName": "Jane Doe",
  "time": "2025-05-07T11:45:00"
}
```

**Required Role**: SUPER_ADMIN

**Success Response (201 Created)**:
```json
{
  "sarId": 1001,
  "caseId": 1,
  "customerId": 2,
  "status": "OPEN",
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "amount": 25000.00,
  "city": "Los Angeles",
  "state": "CA",
  "customerEmail": "jane.doe@example.com",
  "customerAccountNo": "ACC987654321",
  "customerName": "Jane Doe",
  "time": "2025-05-07T11:45:00",
  "localDate": "2025-05-07"
}
```

---

#### 7.2 Get All Reports
```http
GET /sar/reports
Authorization: Bearer {token}
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
[
  {
    "sarId": 1001,
    "caseId": 1,
    "customerId": 2,
    "status": "OPEN",
    "riskScore": 85.0,
    "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
    "amount": 25000.00,
    "city": "Los Angeles",
    "state": "CA",
    "customerEmail": "jane.doe@example.com",
    "customerAccountNo": "ACC987654321",
    "customerName": "Jane Doe",
    "time": "2025-05-07T11:45:00",
    "localDate": "2025-05-07"
  }
]
```

---

#### 7.3 Get Report by ID
```http
GET /sar/report/id/{sarId}
Authorization: Bearer {token}

Example: GET /sar/report/id/1001
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
{
  "sarId": 1001,
  "caseId": 1,
  "customerId": 2,
  "status": "OPEN",
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "amount": 25000.00,
  "city": "Los Angeles",
  "state": "CA",
  "customerEmail": "jane.doe@example.com",
  "customerAccountNo": "ACC987654321",
  "customerName": "Jane Doe",
  "time": "2025-05-07T11:45:00",
  "localDate": "2025-05-07"
}
```

---

#### 7.4 Get Report by Customer Name
```http
GET /sar/report/name/{customerName}
Authorization: Bearer {token}

Example: GET /sar/report/name/Jane Doe
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
{
  "sarId": 1001,
  "caseId": 1,
  "customerId": 2,
  "status": "OPEN",
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "amount": 25000.00,
  "city": "Los Angeles",
  "state": "CA",
  "customerEmail": "jane.doe@example.com",
  "customerAccountNo": "ACC987654321",
  "customerName": "Jane Doe",
  "time": "2025-05-07T11:45:00",
  "localDate": "2025-05-07"
}
```

---

#### 7.5 Get Reports by Account Number
```http
GET /sar/report/account/{customerAccountNo}
Authorization: Bearer {token}

Example: GET /sar/report/account/ACC987654321
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
[
  {
    "sarId": 1001,
    "caseId": 1,
    "customerId": 2,
    "status": "OPEN",
    "riskScore": 85.0,
    "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
    "amount": 25000.00,
    "city": "Los Angeles",
    "state": "CA",
    "customerEmail": "jane.doe@example.com",
    "customerAccountNo": "ACC987654321",
    "customerName": "Jane Doe",
    "time": "2025-05-07T11:45:00",
    "localDate": "2025-05-07"
  }
]
```

---

#### 7.6 Get Reports by Status
```http
GET /sar/report/status/{status}
Authorization: Bearer {token}

Example: GET /sar/report/status/OPEN
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Status Values**: OPEN, UNDER_INVESTIGATION, RESOLVED, CLOSED

**Success Response (200 OK)**:
```json
[
  {
    "sarId": 1001,
    "caseId": 1,
    "customerId": 2,
    "status": "OPEN",
    "riskScore": 85.0,
    "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
    "amount": 25000.00,
    "city": "Los Angeles",
    "state": "CA",
    "customerEmail": "jane.doe@example.com",
    "customerAccountNo": "ACC987654321",
    "customerName": "Jane Doe",
    "time": "2025-05-07T11:45:00",
    "localDate": "2025-05-07"
  }
]
```

---

#### 7.7 Get Reports by City
```http
GET /sar/report/city/{city}
Authorization: Bearer {token}

Example: GET /sar/report/city/Los Angeles
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
[
  {
    "sarId": 1001,
    "caseId": 1,
    "customerId": 2,
    "status": "OPEN",
    "riskScore": 85.0,
    "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
    "amount": 25000.00,
    "city": "Los Angeles",
    "state": "CA",
    "customerEmail": "jane.doe@example.com",
    "customerAccountNo": "ACC987654321",
    "customerName": "Jane Doe",
    "time": "2025-05-07T11:45:00",
    "localDate": "2025-05-07"
  }
]
```

---

#### 7.8 Get Reports by State
```http
GET /sar/report/state/{state}
Authorization: Bearer {token}

Example: GET /sar/report/state/CA
```

**Required Role**: SUPER_ADMIN, FRAUD_ANALYST

**Success Response (200 OK)**:
```json
[
  {
    "sarId": 1001,
    "caseId": 1,
    "customerId": 2,
    "status": "OPEN",
    "riskScore": 85.0,
    "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
    "amount": 25000.00,
    "city": "Los Angeles",
    "state": "CA",
    "customerEmail": "jane.doe@example.com",
    "customerAccountNo": "ACC987654321",
    "customerName": "Jane Doe",
    "time": "2025-05-07T11:45:00",
    "localDate": "2025-05-07"
  }
]
```

---

## Data Models & DTOs

### Customer Entity
```json
{
  "customerId": 1,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "bankName": "Chase Bank",
  "accountNo": "ACC123456789",
  "accountType": "Savings",  // or "Checking", "Business", etc.
  "balance": 50000.00,
  "riskScore": 0.0
}
```

**Validations:**
- `name`: Required, non-empty string
- `email`: Required, unique, valid email format
- `accountNo`: Required, unique
- `balance`: Required, >= 0
- `riskScore`: Calculated field, range 0-100

---

### Transaction Entity
```json
{
  "transactionId": 1001,
  "amount": 5000.00,
  "city": "New York",
  "state": "NY",
  "ipAddress": "192.168.1.1",
  "receiverAccountNumber": "REC987654321",
  "customerId": 1,
  "time": "2025-05-07T10:30:00",
  "status": "genuine",  // "genuine", "flagged", "terminated"
  "reason": "Transaction analysis completed successfully",
  "riskScore": 12.5
}
```

**Validations:**
- `amount`: Required, > 0
- `customerId`: Required, must exist in system
- `receiverAccountNumber`: Required, must be valid account format
- `status`: One of: genuine, flagged, terminated
- `time`: Timestamp, auto-set to current time if not provided

---

### Alert Entity
```json
{
  "alertId": 1,
  "severity": "HIGH",  // "HIGH", "MEDIUM", "LOW"
  "createdAt": "2025-05-07T11:45:00",
  "decisionStatus": "flagged",  // "flagged", "terminated"
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "customerId": 2
}
```

---

### Case Entity
```json
{
  "caseId": "CASE-001",
  "alertId": 1,
  "customerId": 2,
  "status": "OPEN",  // "OPEN", "UNDER_INVESTIGATION", "RESOLVED", "CLOSED"
  "createdAt": "2025-05-07T11:45:00",
  "riskScore": 85.0,
  "investigator": "fraud.analyst@bank.com"
}
```

---

### SAR Report Entity
```json
{
  "sarId": 1001,
  "caseId": 1,
  "customerId": 2,
  "status": "OPEN",  // "OPEN", "UNDER_INVESTIGATION", "RESOLVED", "CLOSED"
  "riskScore": 85.0,
  "reason": "HIGH-RISK TRANSACTION DETECTED: Amount is 2.5x higher than customer's average.",
  "amount": 25000.00,
  "city": "Los Angeles",
  "state": "CA",
  "customerEmail": "jane.doe@example.com",
  "customerAccountNo": "ACC987654321",
  "customerName": "Jane Doe",
  "time": "2025-05-07T11:45:00",
  "localDate": "2025-05-07"
}
```

---

## Error Handling

### Standard Error Response Format

```json
{
  "status": "ERROR",
  "errorCode": "ERROR_CODE_NAME",
  "message": "Human-readable error message"
}
```

### Common Error Codes

| HTTP Code | Error Code | Scenario |
|-----------|-----------|----------|
| 400 | `INVALID_REQUEST` | Malformed request body, missing required fields |
| 400 | `INVALID_ROLE` | Role provided is not valid |
| 401 | `UNAUTHORIZED` | Missing or invalid JWT token |
| 403 | `FORBIDDEN` | User lacks required permissions/role |
| 404 | `NOT_FOUND` | Resource doesn't exist |
| 404 | `CUSTOMER_NOT_FOUND` | Customer ID not found |
| 404 | `RECEIVER_NOT_FOUND` | Receiver account number not found |
| 409 | `CONFLICT` | Username already exists (during registration) |
| 500 | `INTERNAL_ERROR` | Server-side error |

### Example Error Responses

**400 Bad Request - Missing Required Fields:**
```json
{
  "status": "ERROR",
  "errorCode": "INVALID_REQUEST",
  "message": "Required field 'amount' is missing"
}
```

**401 Unauthorized - Invalid Token:**
```json
{
  "status": "ERROR",
  "errorCode": "UNAUTHORIZED",
  "message": "Invalid or expired JWT token"
}
```

**403 Forbidden - Insufficient Permissions:**
```json
{
  "status": "ERROR",
  "errorCode": "FORBIDDEN",
  "message": "User with role FRAUD_ANALYST cannot perform this action"
}
```

**404 Not Found:**
```json
{
  "status": "ERROR",
  "errorCode": "CUSTOMER_NOT_FOUND",
  "message": "Customer with ID 99 not found"
}
```

---

## Service Integration Flow

### Complete Transaction Processing Flow

```
1. USER (Frontend)
   ↓
2. LOGIN/REGISTER at /auth/* endpoints
   ├─ Register: /auth/register (POST)
   └─ Login: /auth/login (POST) → Get JWT token
   ↓
3. CREATE TRANSACTION at /api/transactions (POST)
   Request includes: customerId, amount, receiverAccountNumber, city, state, ipAddress
   ↓
4. TRANSACTION SERVICE receives request
   ├─ Validates customer exists
   ├─ Creates initial transaction entry
   ├─ Sets initial risk score from customer profile
   └─ Prepares enrichment request
   ↓
5. ENRICHMENT SERVICE called
   ├─ Fetches customer data
   ├─ Fetches previous transaction history
   ├─ Validates amount, balance, IP address
   ├─ Prepares decision request for Gemini
   └─ Calls Decision Engine
   ↓
6. DECISION ENGINE / GEMINI SERVICE
   ├─ Analyzes transaction using AI
   ├─ Considers:
   │  ├─ Transaction amount vs. average
   │  ├─ Customer location vs. IP location
   │  ├─ Transaction time patterns
   │  ├─ Historical fraud indicators
   │  └─ Receiver account reputation
   ├─ Generates decision: genuine/flagged/terminated
   └─ Returns risk score (0-100) and reason
   ↓
7. ENRICHMENT SERVICE processes Gemini response
   ├─ Updates transaction with decision
   ├─ Updates customer risk score (average of all transactions)
   └─ IF decision is "flagged" → Creates AlertCasePayload
   ↓
8. IF FLAGGED → ALERT CASE SERVICE
   ├─ Creates investigation case
   ├─ Creates alert with HIGH/MEDIUM/LOW severity
   ├─ Sets case status to OPEN
   └─ Prepares ReportingRequest
   ↓
9. IF ALERT CREATED → SAR REPORT SERVICE
   ├─ Receives ReportingRequest
   ├─ Creates Suspicious Activity Report
   ├─ Links to case and customer
   └─ Stores for audit trail
   ↓
10. RESPONSE returned to FRONTEND
    ├─ Status: SUCCESS/ERROR
    ├─ Transaction ID
    ├─ Decision: genuine/flagged/terminated
    ├─ Risk Score
    ├─ Reason for decision
    └─ Alert sent status
```

### Risk Score Calculation

**Per Transaction Risk Score** (0-100):
- Based on Gemini AI analysis
- Considers:
  - Amount deviation from average: 0-30 points
  - Location anomaly: 0-25 points
  - Time pattern mismatch: 0-15 points
  - Historical fraud: 0-30 points

**Customer Overall Risk Score**:
- Average of all transaction risk scores
- Updated after each new transaction
- Used for future transaction analysis
- Range: 0-100

---

## Configuration & Environment

### Services List & Ports

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| API Gateway | 1001 | MySQL (Auth) | Entry point, JWT validation, routing |
| Transaction Service | 8089 | MySQL | Customer & transaction data |
| Enrichment Service | 8010 | In-memory | Data enrichment & validation |
| Decision Engine | 8071 | None | Gemini AI analysis |
| Alert Case Service | 8092 | MySQL | Alert & case management |
| SAR Report Service | 8079 | MySQL | Report generation & storage |
| Config Server | 8888 | None | Centralized configuration |
| Eureka Server | 8761 | None | Service discovery |

### Environment Variables Required

```properties
# Database Configuration
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USERNAME=root
MYSQL_PASSWORD=root
MYSQL_DATABASE=bankguard_db

# JWT Configuration
JWT_SECRET=BankGuardSuperSecretKeyForJWT2025ThisMustBeLongEnoughForHS256Algorithm
JWT_EXPIRATION=86400000

# Gemini AI Configuration
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-pro

# Service URLs
TRANSACTION_SERVICE_URL=http://localhost:8089
ENRICHMENT_SERVICE_URL=http://localhost:8010
DECISION_ENGINE_URL=http://localhost:8071
ALERT_SERVICE_URL=http://localhost:8092
SAR_SERVICE_URL=http://localhost:8079
CONFIG_SERVER_URL=http://localhost:8888
EUREKA_SERVER_URL=http://localhost:8761/eureka/
```

### Application Properties

**API Gateway** (`gateway/application.properties`):
```properties
spring.application.name=apiGateway
server.port=1001
spring.main.web-application-type=reactive

# R2DBC MySQL (Auth DB)
spring.r2dbc.url=r2dbc:mysql://localhost:3306/bankguard_auth
spring.r2dbc.username=root
spring.r2dbc.password=root

# JWT
jwt.secret=BankGuardSuperSecretKeyForJWT2025...
jwt.expiration=86400000

# Config Server
spring.config.import=configserver:http://localhost:8888
spring.cloud.config.profile=dev
```

**Transaction Service** (`transaction/application.properties`):
```properties
spring.application.name=transactionService
server.port=8089

# Config Server
spring.config.import=configserver:http://localhost:8888

# Eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

---

## Important Notes for Frontend Development

### 1. Authentication Flow

**Step-by-Step Implementation:**

```javascript
// Step 1: Register User (Optional - Admin can create users)
async function register(username, password, role) {
  const response = await axios.post('http://localhost:1001/auth/register', {
    username,
    password,
    role
  });
  return response.data;
}

// Step 2: Login to get JWT Token
async function login(username, password) {
  const response = await axios.post('http://localhost:1001/auth/login', {
    username,
    password
  });
  
  const token = response.data.token;
  localStorage.setItem('token', token);
  localStorage.setItem('role', response.data.role);
  localStorage.setItem('username', response.data.username);
  
  return response.data;
}

// Step 3: Use Token in All Requests
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Step 4: Handle 401 Unauthorized (Token Expired)
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

### 2. Role-Based UI Protection

**Frontend Access Control:**

```javascript
// Role constants
const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  FRAUD_ANALYST: 'FRAUD_ANALYST',
  RISK_MANAGER: 'RISK_MANAGER'
};

// Check user role
function hasRole(requiredRole) {
  const userRole = localStorage.getItem('role');
  return userRole === requiredRole;
}

function hasAnyRole(requiredRoles) {
  const userRole = localStorage.getItem('role');
  return requiredRoles.includes(userRole);
}

// Conditional rendering
{
  hasRole(ROLES.SUPER_ADMIN) && (
    <button onClick={deleteTransaction}>Delete</button>
  )
}

{
  hasAnyRole([ROLES.SUPER_ADMIN, ROLES.FRAUD_ANALYST]) && (
    <div>Investigation Dashboard</div>
  )
}
```

---

### 3. Transaction Creation - Complete Example

```javascript
async function createTransaction(transactionData) {
  try {
    const response = await axios.post(
      'http://localhost:1001/api/transactions',
      {
        customerId: transactionData.customerId,
        amount: transactionData.amount,
        receiverAccountNumber: transactionData.receiverAccountNumber,
        city: transactionData.city,
        state: transactionData.state,
        ipAddress: transactionData.ipAddress
      }
    );

    const result = response.data;
    
    if (result.status === 'SUCCESS') {
      const transaction = result.transaction;
      
      console.log(`Transaction ID: ${transaction.transactionId}`);
      console.log(`Decision: ${transaction.status}`);
      console.log(`Risk Score: ${transaction.riskScore}`);
      console.log(`Reason: ${transaction.reason}`);
      
      // Show decision to user
      if (transaction.status === 'genuine') {
        showSuccessAlert('Transaction approved');
      } else if (transaction.status === 'flagged') {
        showWarningAlert('Transaction flagged for review');
      } else if (transaction.status === 'terminated') {
        showErrorAlert('Transaction blocked');
      }
      
      return transaction;
    } else {
      showErrorAlert(result.message);
    }
  } catch (error) {
    if (error.response?.data?.errorCode === 'RECEIVER_NOT_FOUND') {
      showErrorAlert('Invalid receiver account number');
    } else if (error.response?.status === 403) {
      showErrorAlert('Insufficient permissions');
    } else {
      showErrorAlert('Transaction creation failed');
    }
  }
}
```

---

### 4. Token Refresh Strategy

```javascript
// Since JWT has 24-hour expiration, implement refresh check
function checkTokenExpiry() {
  const token = localStorage.getItem('token');
  if (!token) return false;
  
  try {
    // Decode JWT (without verification on frontend)
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64).split('').map(c => 
        '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
      ).join('')
    );
    
    const payload = JSON.parse(jsonPayload);
    const expiryTime = payload.exp * 1000; // Convert to milliseconds
    
    // If token expires in less than 1 hour, show re-login warning
    if (expiryTime - Date.now() < 3600000) {
      showWarningAlert('Your session will expire soon. Please log in again.');
    }
    
    return true;
  } catch (error) {
    return false;
  }
}

// Check on app load and periodically
setInterval(checkTokenExpiry, 300000); // Every 5 minutes
```

---

### 5. Handling Different Responses

**Customer Lookup:**
```javascript
async function getCustomer(customerId) {
  try {
    const response = await axios.get(
      `http://localhost:1001/api/customers/${customerId}`
    );
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      showErrorAlert('Customer not found');
    } else if (error.response?.status === 403) {
      showErrorAlert('You do not have permission to view this customer');
    }
    return null;
  }
}
```

**Transaction Search:**
```javascript
async function getTransactionsByCustomer(customerId) {
  try {
    const response = await axios.get(
      `http://localhost:1001/api/transactions/customer/${customerId}`
    );
    return response.data || [];
  } catch (error) {
    console.error('Failed to fetch transactions:', error);
    return [];
  }
}
```

---

### 6. Fraud Alert Monitoring Dashboard

**Real-time Alert Component:**
```javascript
async function getRecentAlerts() {
  try {
    const response = await axios.get(
      'http://localhost:1001/api/investigation/alerts'
    );
    
    return response.data.filter(alert => {
      const alertTime = new Date(alert.createdAt);
      const now = new Date();
      const diffMinutes = (now - alertTime) / (1000 * 60);
      return diffMinutes < 60; // Last hour
    });
  } catch (error) {
    console.error('Failed to fetch alerts:', error);
    return [];
  }
}

// Display with severity colors
function getSeverityColor(severity) {
  switch(severity) {
    case 'HIGH': return 'red';
    case 'MEDIUM': return 'orange';
    case 'LOW': return 'yellow';
    default: return 'gray';
  }
}
```

---

### 7. Case Management for Fraud Analysts

```javascript
// Get all open cases
async function getOpenCases() {
  try {
    const response = await axios.get(
      'http://localhost:1001/api/investigation/cases/status/OPEN'
    );
    return response.data;
  } catch (error) {
    console.error('Failed to fetch cases:', error);
    return [];
  }
}

// Update case status
async function updateCaseStatus(caseId, newStatus) {
  try {
    const response = await axios.put(
      `http://localhost:1001/api/investigation/cases/${caseId}/status`,
      null,
      {
        params: { status: newStatus }
      }
    );
    showSuccessAlert(`Case status updated to ${newStatus}`);
    return response.data;
  } catch (error) {
    if (error.response?.status === 403) {
      showErrorAlert('You do not have permission to update cases');
    }
    return null;
  }
}
```

---

### 8. SAR Report Generation

```javascript
// Generate reports for audit
async function getSARReports(filters = {}) {
  try {
    let url = 'http://localhost:1001/sar/reports';
    
    if (filters.status) {
      url = `http://localhost:1001/sar/report/status/${filters.status}`;
    } else if (filters.city) {
      url = `http://localhost:1001/sar/report/city/${filters.city}`;
    } else if (filters.state) {
      url = `http://localhost:1001/sar/report/state/${filters.state}`;
    }
    
    const response = await axios.get(url);
    return response.data || [];
  } catch (error) {
    console.error('Failed to fetch SAR reports:', error);
    return [];
  }
}

// Export report as CSV/PDF
function exportSARReport(report) {
  const csv = `
    SAR Report ID,Case ID,Customer ID,Status,Risk Score,Amount,City,State
    ${report.sarId},${report.caseId},${report.customerId},${report.status},${report.riskScore},${report.amount},${report.city},${report.state}
  `;
  
  const blob = new Blob([csv], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `SAR-${report.sarId}.csv`;
  a.click();
}
```

---

### 9. Error Interceptor Setup

```javascript
// Comprehensive error handling
axios.interceptors.response.use(
  response => response,
  error => {
    const errorData = error.response?.data || {};
    
    switch (error.response?.status) {
      case 400:
        showErrorAlert(`Bad Request: ${errorData.message}`);
        break;
      
      case 401:
        localStorage.clear();
        window.location.href = '/login';
        showErrorAlert('Session expired. Please login again.');
        break;
      
      case 403:
        showErrorAlert('You do not have permission to perform this action');
        break;
      
      case 404:
        showErrorAlert(`Resource not found: ${errorData.message}`);
        break;
      
      case 409:
        showErrorAlert(`Conflict: ${errorData.message}`);
        break;
      
      case 500:
        showErrorAlert('Server error occurred. Please try again later.');
        console.error('Server error details:', errorData);
        break;
      
      default:
        showErrorAlert('An unexpected error occurred');
    }
    
    return Promise.reject(error);
  }
);
```

---

### 10. Logout Implementation

```javascript
function logout() {
  // Clear stored data
  localStorage.removeItem('token');
  localStorage.removeItem('role');
  localStorage.removeItem('username');
  
  // Clear axios default header
  axios.defaults.headers.common['Authorization'] = '';
  
  // Redirect to login
  window.location.href = '/login';
}

// Add logout on token expiry
window.addEventListener('beforeunload', () => {
  const token = localStorage.getItem('token');
  if (!token) {
    logout();
  }
});
```

---

## Troubleshooting Guide for Frontend Developers

| Issue | Cause | Solution |
|-------|-------|----------|
| 401 Unauthorized | Token missing/expired | Login again, check localStorage for token |
| 403 Forbidden | User role insufficient | Check user role matches endpoint requirements |
| 404 Not Found | Resource doesn't exist | Verify resource ID, customer exists, etc. |
| 500 Internal Error | Backend service issue | Check backend logs, restart services if needed |
| CORS Error | Cross-origin request blocked | Configure CORS in gateway or use proxy |
| Connection Refused | Service not running | Ensure all services are running on correct ports |
| Timeout | Service slow/unresponsive | Check backend service health, database connection |

---

## Summary

This documentation provides **complete frontend development guidance** including:

✅ All 7 microservices with full endpoint specifications  
✅ Complete request/response examples for every endpoint  
✅ Role-based access control matrix  
✅ JWT authentication implementation details  
✅ Data models and entity structures  
✅ Error handling and codes  
✅ Service integration flow diagram  
✅ Frontend implementation examples  
✅ Security configuration overview  
✅ Environment setup requirements  

**Ready for Frontend Development** - Use this as your single source of truth for API integration.

---

**Document Version**: 1.0  
**Last Updated**: May 7, 2026  
**Created for**: Frontend Development Team
