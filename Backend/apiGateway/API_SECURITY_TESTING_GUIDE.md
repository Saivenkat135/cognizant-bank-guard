# BankGuard API Gateway - Spring Security Testing Guide

> **Base URL:** `http://localhost:1001`  
> **Auth Type:** JWT Bearer Token  
> **Content-Type:** `application/json`

---

## Table of Contents

1. [Pre-requisites](#1-pre-requisites)
2. [Step 1 — Register Users (All 3 Roles)](#2-step-1--register-users-all-3-roles)
3. [Step 2 — Login & Get JWT Tokens](#3-step-2--login--get-jwt-tokens)
4. [Step 3 — Test Without Token (401 Unauthorized)](#4-step-3--test-without-token-401-unauthorized)
5. [Step 4 — Test Role-Based Access](#5-step-4--test-role-based-access)
6. [Complete Role-Based Access Matrix](#6-complete-role-based-access-matrix)
7. [Postman Collection Setup Guide](#7-postman-collection-setup-guide)
8. [PowerShell Commands (All-in-One)](#8-powershell-commands-all-in-one)
9. [cURL Commands (Git Bash / Linux)](#9-curl-commands-git-bash--linux)

---

## 1. Pre-requisites

1. MySQL running on `localhost:3306`
2. Database created:
   ```sql
   CREATE DATABASE bankguard_auth;
   ```
3. API Gateway running:
   ```bash
   cd apiGateway
   ./mvnw spring-boot:run
   ```
4. Gateway starts on **port 1001**

---

## 2. Step 1 — Register Users (All 3 Roles)

### 2.1 Register Super Admin

```
POST http://localhost:1001/auth/register
```

**Payload:**
```json
{
  "username": "superadmin",
  "password": "admin123",
  "role": "SUPER_ADMIN"
}
```

**Expected Response (201 Created):**
```json
{
  "token": null,
  "username": "superadmin",
  "role": "SUPER_ADMIN",
  "message": "User registered successfully"
}
```

---

### 2.2 Register Fraud Analyst

```
POST http://localhost:1001/auth/register
```

**Payload:**
```json
{
  "username": "fraudanalyst",
  "password": "analyst123",
  "role": "FRAUD_ANALYST"
}
```

**Expected Response (201 Created):**
```json
{
  "token": null,
  "username": "fraudanalyst",
  "role": "FRAUD_ANALYST",
  "message": "User registered successfully"
}
```

---

### 2.3 Register Risk Manager

```
POST http://localhost:1001/auth/register
```

**Payload:**
```json
{
  "username": "riskmanager",
  "password": "risk123",
  "role": "RISK_MANAGER"
}
```

**Expected Response (201 Created):**
```json
{
  "token": null,
  "username": "riskmanager",
  "role": "RISK_MANAGER",
  "message": "User registered successfully"
}
```

---

### 2.4 Register with Invalid Role (Error Test)

```
POST http://localhost:1001/auth/register
```

**Payload:**
```json
{
  "username": "testuser",
  "password": "test123",
  "role": "INVALID_ROLE"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "token": null,
  "username": null,
  "role": null,
  "message": "Invalid role. Allowed roles: SUPER_ADMIN, FRAUD_ANALYST, RISK_MANAGER"
}
```

---

### 2.5 Register Duplicate Username (Error Test)

```
POST http://localhost:1001/auth/register
```

**Payload:**
```json
{
  "username": "superadmin",
  "password": "anything",
  "role": "SUPER_ADMIN"
}
```

**Expected Response (409 Conflict):**
```json
{
  "token": null,
  "username": null,
  "role": null,
  "message": "Username already exists"
}
```

---

## 3. Step 2 — Login & Get JWT Tokens

### 3.1 Login as Super Admin

```
POST http://localhost:1001/auth/login
```

**Payload:**
```json
{
  "username": "superadmin",
  "password": "admin123"
}
```

**Expected Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...<long_jwt_token>",
  "username": "superadmin",
  "role": "SUPER_ADMIN",
  "message": "Login successful"
}
```

> **Copy the `token` value — you will use it as `Bearer <token>` in the Authorization header.**

---

### 3.2 Login as Fraud Analyst

```
POST http://localhost:1001/auth/login
```

**Payload:**
```json
{
  "username": "fraudanalyst",
  "password": "analyst123"
}
```

---

### 3.3 Login as Risk Manager

```
POST http://localhost:1001/auth/login
```

**Payload:**
```json
{
  "username": "riskmanager",
  "password": "risk123"
}
```

---

### 3.4 Login with Wrong Password (Error Test)

```
POST http://localhost:1001/auth/login
```

**Payload:**
```json
{
  "username": "superadmin",
  "password": "wrongpassword"
}
```

**Expected Response (401 Unauthorized):**
```json
{
  "token": null,
  "username": null,
  "role": null,
  "message": "Invalid username or password"
}
```

---

## 4. Step 3 — Test Without Token (401 Unauthorized)

Any request **without** `Authorization: Bearer <token>` header should return **401**.

```
GET  http://localhost:1001/api/transactions        → 401
GET  http://localhost:1001/api/customers            → 401
GET  http://localhost:1001/api/investigation/alerts → 401
GET  http://localhost:1001/sar/reports              → 401
POST http://localhost:1001/api/enrich/transaction   → 401
POST http://localhost:1001/api/gemini/analyze-transaction → 401
```

---

## 5. Step 4 — Test Role-Based Access

> **How to use the token in Postman:**  
> Go to **Authorization** tab → Type: **Bearer Token** → Paste the JWT token.
>
> **Or** manually add header:  
> `Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...`

> **Note:** If the downstream service (transactionService, etc.) is not running, you'll get `500` or `503` — this is **NORMAL** and means **auth passed** but the backend is unreachable. A `403` means **access denied** (role not authorized).

---

### 5.1 SUPER ADMIN Tests (Full Access to Everything)

All requests use **Super Admin's token**.

| # | Method | Endpoint | Expected |
|---|--------|----------|----------|
| 1 | GET | `/api/transactions` | 200/500 (Allowed) |
| 2 | POST | `/api/transactions` | 200/500 (Allowed) |
| 3 | PUT | `/api/transactions/1` | 200/500 (Allowed) |
| 4 | DELETE | `/api/transactions/1` | 200/500 (Allowed) |
| 5 | GET | `/api/customers` | 200/500 (Allowed) |
| 6 | POST | `/api/customers` | 200/500 (Allowed) |
| 7 | PUT | `/api/customers/1` | 200/500 (Allowed) |
| 8 | DELETE | `/api/customers/1` | 200/500 (Allowed) |
| 9 | GET | `/api/investigation/alerts` | 200/500 (Allowed) |
| 10 | GET | `/api/investigation/cases/1` | 200/500 (Allowed) |
| 11 | PUT | `/api/investigation/cases/1/status?status=CLOSED` | 200/500 (Allowed) |
| 12 | POST | `/api/investigation/ingest` | 200/500 (Allowed) |
| 13 | GET | `/sar/reports` | 200/500 (Allowed) |
| 14 | POST | `/sar/report` | 200/500 (Allowed) |
| 15 | POST | `/api/enrich/transaction` | 200/500 (Allowed) |
| 16 | POST | `/api/gemini/analyze-transaction` | 200/500 (Allowed) |

---

### 5.2 FRAUD ANALYST Tests

All requests use **Fraud Analyst's token**.

| # | Method | Endpoint | Expected | Reason |
|---|--------|----------|----------|--------|
| 1 | GET | `/api/transactions` | 200/500 (Allowed) | Can review transactions |
| 2 | **POST** | `/api/transactions` | **403 Forbidden** | Cannot create transactions |
| 3 | **PUT** | `/api/transactions/1` | **403 Forbidden** | Cannot modify transactions |
| 4 | **DELETE** | `/api/transactions/1` | **403 Forbidden** | Cannot delete transactions |
| 5 | GET | `/api/customers` | 200/500 (Allowed) | Can view customer info |
| 6 | **POST** | `/api/customers` | **403 Forbidden** | Cannot create customers |
| 7 | GET | `/api/investigation/alerts` | 200/500 (Allowed) | Reviews alerts |
| 8 | GET | `/api/investigation/cases/1` | 200/500 (Allowed) | Investigates cases |
| 9 | PUT | `/api/investigation/cases/1/status?status=CLOSED` | 200/500 (Allowed) | Adjudicates cases |
| 10 | GET | `/sar/reports` | 200/500 (Allowed) | Reviews SAR reports |
| 11 | **POST** | `/sar/report` | **403 Forbidden** | Cannot create SAR reports |
| 12 | **POST** | `/api/enrich/transaction` | **403 Forbidden** | No access to enrichment |
| 13 | **POST** | `/api/gemini/analyze-transaction` | **403 Forbidden** | No access to decision engine |

---

### 5.3 RISK MANAGER Tests

All requests use **Risk Manager's token**.

| # | Method | Endpoint | Expected | Reason |
|---|--------|----------|----------|--------|
| 1 | GET | `/api/transactions` | 200/500 (Allowed) | Reviews trends |
| 2 | **POST** | `/api/transactions` | **403 Forbidden** | Cannot create transactions |
| 3 | **PUT** | `/api/transactions/1` | **403 Forbidden** | Cannot modify transactions |
| 4 | GET | `/api/customers` | 200/500 (Allowed) | Views customer risk profiles |
| 5 | **POST** | `/api/customers` | **403 Forbidden** | Cannot create customers |
| 6 | POST | `/api/enrich/transaction` | 200/500 (Allowed) | Defines rules & thresholds |
| 7 | POST | `/api/gemini/analyze-transaction` | 200/500 (Allowed) | Defines detection rules |
| 8 | **GET** | `/api/investigation/alerts` | **403 Forbidden** | No access to alerts |
| 9 | **GET** | `/api/investigation/cases/1` | **403 Forbidden** | No access to cases |
| 10 | **GET** | `/sar/reports` | **403 Forbidden** | No access to SAR reports |
| 11 | **POST** | `/sar/report` | **403 Forbidden** | No access to SAR reports |

---

## 6. Complete Role-Based Access Matrix

| Endpoint | Method | Super Admin | Fraud Analyst | Risk Manager |
|----------|--------|:-----------:|:-------------:|:------------:|
| `/auth/register` | POST | Open | Open | Open |
| `/auth/login` | POST | Open | Open | Open |
| `/api/transactions` | GET | ✅ | ✅ | ✅ |
| `/api/transactions` | POST | ✅ | ❌ 403 | ❌ 403 |
| `/api/transactions/{id}` | PUT | ✅ | ❌ 403 | ❌ 403 |
| `/api/transactions/{id}` | DELETE | ✅ | ❌ 403 | ❌ 403 |
| `/api/customers` | GET | ✅ | ✅ | ✅ |
| `/api/customers` | POST | ✅ | ❌ 403 | ❌ 403 |
| `/api/customers/{id}` | PUT | ✅ | ❌ 403 | ❌ 403 |
| `/api/customers/{id}` | DELETE | ✅ | ❌ 403 | ❌ 403 |
| `/api/investigation/alerts` | GET | ✅ | ✅ | ❌ 403 |
| `/api/investigation/cases/{id}` | GET | ✅ | ✅ | ❌ 403 |
| `/api/investigation/cases/{id}/status` | PUT | ✅ | ✅ | ❌ 403 |
| `/api/investigation/ingest` | POST | ✅ | ❌ 403 | ❌ 403 |
| `/sar/reports` | GET | ✅ | ✅ | ❌ 403 |
| `/sar/report` | POST | ✅ | ❌ 403 | ❌ 403 |
| `/api/enrich/**` | ALL | ✅ | ❌ 403 | ✅ |
| `/api/gemini/**` | ALL | ✅ | ❌ 403 | ✅ |
| No token on any endpoint | ANY | ❌ 401 | ❌ 401 | ❌ 401 |

---

## 7. Postman Collection Setup Guide

### Step 1: Create Environment Variables

In Postman, go to **Environments** → **Create New** → Name it `BankGuard Local`:

| Variable | Initial Value |
|----------|---------------|
| `base_url` | `http://localhost:1001` |
| `super_token` | *(leave empty, will be set after login)* |
| `analyst_token` | *(leave empty, will be set after login)* |
| `risk_token` | *(leave empty, will be set after login)* |

### Step 2: Auto-Save Token on Login

In each **Login** request, go to **Tests** tab and add:

```javascript
var jsonData = pm.response.json();
if (jsonData.token) {
    // Change variable name based on role:
    // "super_token" for Super Admin
    // "analyst_token" for Fraud Analyst  
    // "risk_token" for Risk Manager
    pm.environment.set("super_token", jsonData.token);
}
```

### Step 3: Use Token in Requests

For each protected request:
1. Go to **Authorization** tab
2. Type: **Bearer Token**
3. Token: `{{super_token}}` or `{{analyst_token}}` or `{{risk_token}}`

---

## 8. PowerShell Commands (All-in-One)

Copy-paste these into PowerShell to test everything.

### Register All Users

```powershell
# Register Super Admin
Invoke-RestMethod -Uri "http://localhost:1001/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"superadmin","password":"admin123","role":"SUPER_ADMIN"}' | ConvertTo-Json

# Register Fraud Analyst
Invoke-RestMethod -Uri "http://localhost:1001/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"fraudanalyst","password":"analyst123","role":"FRAUD_ANALYST"}' | ConvertTo-Json

# Register Risk Manager
Invoke-RestMethod -Uri "http://localhost:1001/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"riskmanager","password":"risk123","role":"RISK_MANAGER"}' | ConvertTo-Json
```

### Login & Save Tokens

```powershell
# Login Super Admin
$r1 = Invoke-RestMethod -Uri "http://localhost:1001/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"superadmin","password":"admin123"}'
$superToken = $r1.token
Write-Host "Super Admin Token: $superToken"

# Login Fraud Analyst
$r2 = Invoke-RestMethod -Uri "http://localhost:1001/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"fraudanalyst","password":"analyst123"}'
$analystToken = $r2.token
Write-Host "Fraud Analyst Token: $analystToken"

# Login Risk Manager
$r3 = Invoke-RestMethod -Uri "http://localhost:1001/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"riskmanager","password":"risk123"}'
$riskToken = $r3.token
Write-Host "Risk Manager Token: $riskToken"
```

### Test No Token (Should be 401)

```powershell
try { Invoke-WebRequest -Uri "http://localhost:1001/api/transactions" -Method GET -UseBasicParsing } catch { "No Token => Status: $($_.Exception.Response.StatusCode.Value__) (Expected: 401)" }
```

### Test Super Admin (Should ALL Pass)

```powershell
$h = @{Authorization="Bearer $superToken"}

try { Invoke-WebRequest -Uri "http://localhost:1001/api/transactions" -Method GET -Headers $h -UseBasicParsing } catch { "SuperAdmin GET /api/transactions => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/customers" -Method GET -Headers $h -UseBasicParsing } catch { "SuperAdmin GET /api/customers => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/investigation/alerts" -Method GET -Headers $h -UseBasicParsing } catch { "SuperAdmin GET /api/investigation/alerts => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/sar/reports" -Method GET -Headers $h -UseBasicParsing } catch { "SuperAdmin GET /sar/reports => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/enrich/transaction" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "SuperAdmin POST /api/enrich => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/gemini/analyze-transaction" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "SuperAdmin POST /api/gemini => $($_.Exception.Response.StatusCode.Value__)" }
```

> **500/503 = Auth PASSED** (backend service not running). **403 = Access Denied**.

### Test Fraud Analyst (Mixed — some pass, some 403)

```powershell
$h = @{Authorization="Bearer $analystToken"}

# Should PASS (500 = auth passed, backend down)
try { Invoke-WebRequest -Uri "http://localhost:1001/api/transactions" -Method GET -Headers $h -UseBasicParsing } catch { "Analyst GET /api/transactions => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/investigation/alerts" -Method GET -Headers $h -UseBasicParsing } catch { "Analyst GET /investigation/alerts => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/sar/reports" -Method GET -Headers $h -UseBasicParsing } catch { "Analyst GET /sar/reports => $($_.Exception.Response.StatusCode.Value__)" }

# Should FAIL with 403
try { Invoke-WebRequest -Uri "http://localhost:1001/api/transactions" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "Analyst POST /api/transactions => $($_.Exception.Response.StatusCode.Value__) (Expected: 403)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/enrich/transaction" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "Analyst POST /api/enrich => $($_.Exception.Response.StatusCode.Value__) (Expected: 403)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/gemini/analyze-transaction" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "Analyst POST /api/gemini => $($_.Exception.Response.StatusCode.Value__) (Expected: 403)" }
```

### Test Risk Manager (Mixed — some pass, some 403)

```powershell
$h = @{Authorization="Bearer $riskToken"}

# Should PASS (500 = auth passed, backend down)
try { Invoke-WebRequest -Uri "http://localhost:1001/api/transactions" -Method GET -Headers $h -UseBasicParsing } catch { "RiskMgr GET /api/transactions => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/enrich/transaction" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "RiskMgr POST /api/enrich => $($_.Exception.Response.StatusCode.Value__)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/gemini/analyze-transaction" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "RiskMgr POST /api/gemini => $($_.Exception.Response.StatusCode.Value__)" }

# Should FAIL with 403
try { Invoke-WebRequest -Uri "http://localhost:1001/api/investigation/alerts" -Method GET -Headers $h -UseBasicParsing } catch { "RiskMgr GET /investigation/alerts => $($_.Exception.Response.StatusCode.Value__) (Expected: 403)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/sar/reports" -Method GET -Headers $h -UseBasicParsing } catch { "RiskMgr GET /sar/reports => $($_.Exception.Response.StatusCode.Value__) (Expected: 403)" }

try { Invoke-WebRequest -Uri "http://localhost:1001/api/transactions" -Method POST -Headers $h -ContentType "application/json" -Body '{}' -UseBasicParsing } catch { "RiskMgr POST /api/transactions => $($_.Exception.Response.StatusCode.Value__) (Expected: 403)" }
```

---

## 9. cURL Commands (Git Bash / Linux)

### Register

```bash
# Super Admin
curl -X POST http://localhost:1001/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123","role":"SUPER_ADMIN"}'

# Fraud Analyst
curl -X POST http://localhost:1001/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"fraudanalyst","password":"analyst123","role":"FRAUD_ANALYST"}'

# Risk Manager
curl -X POST http://localhost:1001/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"riskmanager","password":"risk123","role":"RISK_MANAGER"}'
```

### Login & Get Tokens

```bash
# Login Super Admin (copy the token from response)
curl -X POST http://localhost:1001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}'

# Login Fraud Analyst
curl -X POST http://localhost:1001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"fraudanalyst","password":"analyst123"}'

# Login Risk Manager
curl -X POST http://localhost:1001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"riskmanager","password":"risk123"}'
```

### Test with Token (replace YOUR_TOKEN_HERE)

```bash
# Test Super Admin — should pass (500 = backend down but auth passed)
curl -X GET http://localhost:1001/api/transactions \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Test Fraud Analyst POST transactions — should get 403
curl -X POST http://localhost:1001/api/transactions \
  -H "Authorization: Bearer YOUR_ANALYST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'

# Test Risk Manager GET alerts — should get 403
curl -X GET http://localhost:1001/api/investigation/alerts \
  -H "Authorization: Bearer YOUR_RISK_TOKEN"

# Test without any token — should get 401
curl -X GET http://localhost:1001/api/transactions
```

---

## Quick Reference: Response Code Meanings

| Status Code | Meaning |
|-------------|---------|
| **200** | Success — endpoint accessible and backend responded |
| **201** | Created — registration successful |
| **401** | Unauthorized — no token or invalid/expired token |
| **403** | Forbidden — valid token but role not authorized for this endpoint |
| **409** | Conflict — username already exists (registration) |
| **500/503** | Auth PASSED but downstream service is not running (normal in standalone testing) |
