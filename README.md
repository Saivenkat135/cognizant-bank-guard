# 🚨 Real-Time Payment Monitoring & Fraud Detection System

## 📌 Overview

A scalable **Microservices-Based Real-Time Payment Monitoring and Fraud Detection System** designed to detect and prevent fraudulent transactions **before they are completed**.

Unlike traditional fraud detection systems that rely solely on rigid predefined rules, this platform combines:

* Rule-Based Validation
* AI-Powered Decision Making using Google Gemini
* Dynamic Risk Scoring
* Case Management
* Regulatory SAR Reporting

The system acts as an intelligent fraud detection layer between third-party payment applications (such as Google Pay, PhonePe, Paytm, etc.) and banking systems.

When a transaction is initiated, the payment application sends:

* Customer Details
* Current Transaction Information
* Previous 5 Transaction Records

The system then performs real-time analysis and determines whether the transaction should be:

✅ Approved

⚠️ Flagged

❌ Terminated

before money movement occurs.

---

# 🎯 Problem Statement

Traditional fraud detection systems rely on static rules such as:

```
IF amount > 10000 THEN Block Transaction
```

These approaches:

* Generate false positives
* Are easy for fraudsters to bypass
* Cannot understand customer behavior
* Lack contextual reasoning

This project addresses those limitations by integrating **Google Gemini AI** into the fraud detection pipeline.

Gemini analyzes:

* Customer transaction history
* Geographical patterns
* Transaction anomalies
* Risk manager policies
* Behavioral indicators

and generates:

* Fraud Status
* Risk Score
* Human-readable Reasoning

for every transaction.

---

# 🏗 System Architecture

The complete architecture diagram is available in:

```bash
/architecture-diagram.png
```

> Refer to the architecture diagram in the root folder for a visual representation of the end-to-end system flow.

---

# ⚙️ Technology Stack

## Backend

* Java
* Spring Boot
* Spring Cloud
* Spring Security
* Spring Data JPA
* Spring Validation
* REST APIs

## AI & Fraud Analysis

* Google Gemini API
* Gemini 3 Flash Preview Model
* Prompt Engineering
* Risk Scoring Engine

## Database

* MySQL

## Microservices Infrastructure

* Eureka Server
* Spring Cloud Config Server
* API Gateway

## Frontend

* React.js
* Bootstrap / Tailwind CSS

## Reporting & Analytics

* SAR Reporting Service
* Fraud Analytics Dashboard

---

# 🧩 Microservices

## 1️⃣ Transaction Service

Responsible for:

* Receiving transaction requests
* Fetching customer details
* Fetching previous transaction history
* Sending transaction data to Enrichment Service
* Completing money transfer for approved transactions

---

## 2️⃣ Enrichment Service

Responsible for:

### Business Validations

* Customer existence validation
* Balance verification
* Account number validation
* Transaction integrity checks

### Data Transformation

Creates:

```java
DecisionRequest
```

which contains:

* Current Transaction
* Last 5 Transactions

and forwards it to the Decision Engine Service.

---

## 3️⃣ Decision Engine Service

### Core Fraud Detection Engine

This service was fully implemented by me.

Built using:

```text
Google Gemini API
Model: gemini-3-flash-preview
```

Responsibilities:

* Rule-based fraud checks
* Risk score calculation
* Prompt generation
* AI fraud analysis

### Sample Validations

#### Location Validation

```text
Current State == Previous Transaction State
```

Failure:

```text
+10 Risk Score
```

---

#### Transaction Amount Validation

```text
Current Amount <= Average of Previous Transactions
```

Failure:

```text
Risk Score Increased
```

---

### Gemini Analysis

The Decision Engine builds a structured prompt containing:

* Current Transaction
* Historical Transactions
* Validation Results
* Risk Indicators
* Risk Manager Rules

Gemini returns:

```json
{
  "riskScore": 82,
  "status": "FLAGGED",
  "reason": "Transaction amount significantly exceeds historical spending behavior and location mismatch detected."
}
```

---

## 4️⃣ Alert & Case Service

Triggered when a transaction is:

* FLAGGED
* TERMINATED

Responsibilities:

### Alert Management

Generates:

* Alert ID

### Case Management

Generates:

* Case ID

Stores:

* Customer Details
* Fraud Indicators
* Transaction Information

### Notification System

Sends fraud alert emails to customers.

---

## 5️⃣ SAR Report Service

Responsible for:

* Regulatory Reporting
* Fraud Investigation Support
* Long-Term Fraud Storage
* Compliance Tracking

Stores:

* Customer Information
* Fraud Details
* Alert Information
* Case Information
* Risk Scores

---

## 6️⃣ API Gateway

Single entry point for all client requests.

Responsibilities:

* Routing
* Load balancing
* Security
* Request forwarding

---

## 7️⃣ Eureka Server

Provides:

* Service Discovery
* Dynamic Registration
* Service Lookup

---

## 8️⃣ Config Server

Provides centralized configuration management for all microservices.

---

# 🔄 End-to-End Transaction Flow

## Step 1

User initiates payment through:

* GPay
* PhonePe
* Paytm
* Any external payment application

Transaction details are sent to:

```text
Transaction Service
```

---

## Step 2

Transaction Service gathers:

* Customer Details
* Current Transaction
* Last 5 Transactions

and forwards them to:

```text
Enrichment Service
```

---

## Step 3

Enrichment Service validates:

* Balance
* Customer existence
* Account number
* Transaction consistency

Creates:

```text
DecisionRequest
```

and sends it to:

```text
Decision Engine Service
```

---

## Step 4

Decision Engine Service:

* Executes fraud rules
* Calculates risk score
* Builds Gemini prompt
* Receives AI response

Returns:

```json
{
  "status": "GENUINE | FLAGGED | TERMINATED",
  "riskScore": 0-100,
  "reason": "Fraud explanation"
}
```

---

## Step 5

Enrichment Service processes decision.

### Genuine

Transaction continues successfully.

### Flagged / Terminated

* Transaction stopped
* User notified
* Fraud case generated

---

## Step 6

Alert & Case Service:

* Creates Alert
* Creates Case
* Stores investigation data
* Sends notification email

---

## Step 7

SAR Report Service stores fraud records for:

* Compliance
* Investigation
* Analytics
* Reporting

---

# 🎯 Fraud Detection Strategy

The system uses a hybrid fraud detection model.

## Rule-Based Layer

Examples:

* Location mismatch
* Amount anomaly
* Account validation
* Historical behavior checks

---

## AI-Based Layer

Google Gemini analyzes:

* Customer behavior
* Transaction context
* Historical patterns
* Fraud indicators

and generates intelligent reasoning.

---

# 📊 Risk Scoring System

Every transaction receives a risk score.

| Risk Score | Status     |
| ---------- | ---------- |
| 0 - 30     | Genuine    |
| 31 - 70    | Flagged    |
| 71 - 100   | Terminated |

Example:

| Validation Failure       | Risk Added |
| ------------------------ | ---------- |
| Location Mismatch        | +10        |
| Amount Anomaly           | +20        |
| Multiple Risk Indicators | +30        |

---

# 👥 User Roles

## Customer

### Features

* View Profile
* Send Money
* Transaction History
* View Risk Scores
* Notifications

---

## Fraud Analyst

### Features

* Investigate Fraud Cases
* Review Alerts
* Fraud Analytics
* Filter Fraud Records
* Area-wise Analysis
* Bank-wise Analysis

---

## Risk Manager

### Features

* Manage Fraud Rules
* Define Risk Thresholds
* Monitor Risk Trends
* Customer Monitoring
* Transaction Monitoring

---

## Super Admin

Full system access.

Capabilities:

* Manage Rules
* Manage Users
* View Analytics
* Fraud Monitoring
* Risk Monitoring
* System Administration

---

# 📈 Analytics & Reporting

The system supports:

### Transaction Analytics

* Genuine Transactions
* Fraud Transactions
* Risk Distribution

### Fraud Analytics

* Area-wise Fraud Analysis
* Bank-wise Fraud Analysis
* Fraud Trends
* Risk Heatmaps

### SAR Reporting

* Regulatory Compliance
* Suspicious Activity Reports
* Investigation Support

---

# 🔒 Key Features

✅ Real-Time Fraud Detection

✅ AI-Powered Decision Engine

✅ Google Gemini Integration

✅ Risk Scoring Mechanism

✅ Rule-Based Validation Engine

✅ Automatic Fraud Alerts

✅ Case Management System

✅ SAR Reporting Service

✅ Microservices Architecture

✅ Role-Based Dashboards

✅ Service Discovery with Eureka

✅ Centralized Configuration

✅ API Gateway Routing

---

# 🚀 Future Enhancements

* Machine Learning Based Risk Prediction
* Real-Time Kafka Event Streaming
* Device Fingerprinting
* Behavioral Biometrics
* Geo-Fencing
* Multi-Factor Authentication
* Fraud Pattern Clustering
* Real-Time Dashboard Analytics
* Cloud Deployment (AWS / Azure)

---

# 👨‍💻 Author

**Sai Venkat**

MERN Stack & Java Full Stack Developer

Special Interests:

* Distributed Systems
* Spring Boot Microservices
* Fraud Detection Systems
* Artificial Intelligence
* Cyber Security
* Cloud-Native Applications

---

## ⭐ If you found this project interesting, don't forget to star the repository.
