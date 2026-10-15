# Vyavsa Backend API Contract Map

This document defines the verified API contracts provided by the Vyavsa backend (`/home/varun/Desktop/vyavsa`), verified against the actual Go codebase (routes, handlers, DTOs, and middleware).

**Base API Path**: `/api/v1`

---

## 1. Response Envelope

All API endpoints return JSON conforming to the standard envelope:

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "message": "operation successful",
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_items": 45,
    "total_pages": 3
  }
}
```
*Note: `pagination` is present only for paginated list endpoints.*

### Error Response
```json
{
  "success": false,
  "message": "invalid credentials",
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "invalid credentials",
    "details": {
      "field": "error detail"
    },
    "fields": {
      "field": "error detail"
    }
  }
}
```

---

## 2. Authentication & Session

### 2.1 Public Catalog: List Subscription Plans
- **Route**: `GET /api/v1/plans`
- **Auth**: None (Public)
- **Response**: Array of subscription plans (`id`, `name`, `price`, `duration_days`, `features`, `status`)

### 2.2 Tenant User Login
- **Route**: `POST /api/v1/auth/tenant/login`
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "email": "owner@example.com",
    "password": "secretpassword",
    "tenant_id": "optional-uuid-if-multi-tenant-user"
  }
  ```
- **Response (`TokenResponse`)**:
  ```json
  {
    "access_token": "eyJhbGciOi...",
    "refresh_token": "eyJhbGciOi...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user": {
      "id": "uuid",
      "name": "Varun",
      "email": "owner@example.com",
      "role": "tenant_admin",
      "user_type": "tenant",
      "tenant_id": "uuid"
    }
  }
  ```

### 2.3 Tenant Self-Registration
- **Route**: `POST /api/v1/auth/tenant/register` (or `POST /api/v1/tenants/register`)
- **Auth**: None (Public)
- **Request Body**:
  ```json
  {
    "name": "My Business",
    "admin_name": "Varun",
    "email": "owner@example.com",
    "password": "secretpassword",
    "phone": "9876543210",
    "plan_id": "optional-uuid"
  }
  ```
- **Response**: `TenantResponse` (created tenant details)

### 2.4 Token Refresh
- **Route**: `POST /api/v1/auth/refresh`
- **Auth**: None (Accepts `refresh_token` in JSON body or cookie)
- **Request Body**:
  ```json
  {
    "refresh_token": "valid_refresh_token"
  }
  ```
- **Response**: `TokenResponse` with new `access_token` and `refresh_token`.

### 2.5 Token Revoke
- **Route**: `POST /api/v1/auth/revoke`
- **Auth**: None
- **Request Body**:
  ```json
  {
    "refresh_token": "valid_refresh_token"
  }
  ```
- **Response**: `{"revoked": true}`

### 2.6 Logout
- **Route**: `POST /api/v1/auth/logout`
- **Auth**: Bearer Token (`Authorization: Bearer <access_token>`)
- **Request Body** (Optional):
  ```json
  {
    "refresh_token": "valid_refresh_token"
  }
  ```
- **Response**: `{"logged_out": true}`

### 2.7 Current User Profile (`Me`)
- **Route**: `GET /api/v1/auth/me`
- **Auth**: Bearer Token
- **Response**: `UserProfile` (`id`, `name`, `email`, `role`, `user_type`, `tenant_id`)

### 2.8 Forgot Password (OTP Generation)
- **Route**: `POST /api/v1/auth/forgot-password`
- **Auth**: None
- **Request Body**:
  ```json
  {
    "email": "user@example.com"
  }
  ```
- **Response**: `{"message": "If an account exists with this email, an OTP has been sent."}`

### 2.9 Verify Reset OTP
- **Route**: `POST /api/v1/auth/verify-reset-otp`
- **Auth**: None
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "otp": "123456"
  }
  ```
- **Response**:
  ```json
  {
    "reset_token": "temporary-reset-token-uuid"
  }
  ```

### 2.10 Reset Password
- **Route**: `POST /api/v1/auth/reset-password`
- **Auth**: None
- **Request Body**:
  ```json
  {
    "reset_token": "temporary-reset-token-uuid",
    "new_password": "newSecurePassword123"
  }
  ```
- **Response**: `{"message": "Password reset successfully."}`

---

## 3. Tenant Business & Financial Operations

All tenant operations require:
- Header: `Authorization: Bearer <access_token>`
- Role: Tenant Member (`tenant_admin` or `tenant_user`) unless specifically noted `tenant_admin` only.

### 3.1 Financial Metrics & Dashboard
- **Route**: `GET /api/v1/tenant/dashboard` or `GET /api/v1/tenant/metrics`
- **Response (`FinancialMetrics`)**:
  - `cash_balance`: Authoritative cash on hand.
  - `bank_balance`: Total across all bank accounts.
  - `total_receivable`: Net customer balances receivable.
  - `total_payable`: Outstanding supplier payables.
  - `net_dues`: Net balance (receivable - payable).
  - `pending_salary`: Total unpaid employee salaries.
  - `today_employee_salary`: Salary earned today.
  - `bank_balances`: Array of `TenantBank` accounts.
  - `today_overview`:
    - `attendance`: `{"present": 5, "total": 6, "absent": 1, "has_records": true}`
    - `line_sale`: Line sales amount today.
    - `counter_sale`: Counter sales amount today.
    - `employee_advance`: Advances disbursed today.
    - `outstanding_payable`: Total outstanding payables.
    - `date`: YYYY-MM-DD

### 3.2 Tenant Profile & Settings
- **Route**: `GET /api/v1/tenant/profile`
- **Route**: `PUT /api/v1/tenant/profile` (`tenant_admin` only)
  - Request: `{"name": "...", "email": "...", "phone": "..."}`
- **Route**: `GET /api/v1/tenant/subscription`
  - Response: Subscription status, plan details, start/end dates, days remaining.

---

## 4. Line Sales & Counter Sales (Transactions)

### 4.1 Line Sales
- **List**: `GET /api/v1/tenant/line-sales?page=1&page_size=20&date=YYYY-MM-DD&search=`
- **Create**: `POST /api/v1/tenant/line-sales`
  - Request:
    ```json
    {
      "customer_id": "uuid",
      "route": "North Route",
      "salesman": "Ramesh",
      "note": "Morning delivery",
      "total_amount": "1500.00",
      "total_cash_in": "1000.00",
      "bank_amount": "500.00",
      "bank_id": "optional-uuid"
    }
    ```
- **Details**: `GET /api/v1/tenant/line-sales/:id`
- **Payments**: `GET /api/v1/tenant/line-sales/:id/payments`
- **Add Payment**: `POST /api/v1/tenant/line-sales/:id/payments`

### 4.2 Counter Sales
- **List**: `GET /api/v1/tenant/counter-sales?page=1&page_size=20&date=YYYY-MM-DD&search=`
- **Create**: `POST /api/v1/tenant/counter-sales`
  - Request:
    ```json
    {
      "item": "Product A",
      "price": "250.00",
      "total_amount": "250.00",
      "payment_method": "cash",
      "cash": "250.00"
    }
    ```
- **Details**: `GET /api/v1/tenant/counter-sales/:id`

---

## 5. Purchases & Supplier Dues

### 5.1 Purchases
- **List**: `GET /api/v1/tenant/purchases?page=1&page_size=20&date=YYYY-MM-DD&search=`
- **Create**: `POST /api/v1/tenant/purchases`
  - Request:
    ```json
    {
      "customer_id": "optional-uuid-supplier",
      "item": "Raw Material X",
      "quantity": 10,
      "total_amount": "5000.00",
      "total_paid": "2000.00"
    }
    ```
- **Details**: `GET /api/v1/tenant/purchases/:id`
- **Record Settlement Payment**: `POST /api/v1/tenant/purchases/:id/payments`

### 5.2 Customer & Supplier Ledger (Dues)
- **List Customers**: `GET /api/v1/tenant/customers?page=1&page_size=20&search=` (`tenant_admin`)
- **Customer Balance**: `GET /api/v1/tenant/customers/:id/balance`
  - Returns: `current_balance`, `total_purchases`, `total_paid`, `outstanding_payable`
- **Customer Statement**: `GET /api/v1/tenant/customers/:id/statement`
  - Returns chronological statement with running balance.
- **Record Settlement Payment**: `POST /api/v1/tenant/customers/:id/payments`
  - Request:
    ```json
    {
      "amount": "1000.00",
      "payment_method": "cash",
      "payment_date": "2026-10-14",
      "note": "Settlement"
    }
    ```

---

## 6. Expenses

- **List**: `GET /api/v1/tenant/expenses?page=1&page_size=20&date=YYYY-MM-DD&search=`
- **Create**: `POST /api/v1/tenant/expenses`
  - Request:
    ```json
    {
      "item": "Fuel for delivery van",
      "category": "transport",
      "total_amount": "450.00",
      "payment_method": "cash"
    }
    ```
- **Details**: `GET /api/v1/tenant/expenses/:id`

---

## 7. Employees, Attendance & Salary

### 7.1 Employees
- **List**: `GET /api/v1/tenant/employees?page=1&page_size=20&search=` (`tenant_admin`)
- **Create**: `POST /api/v1/tenant/employees`
  - Request:
    ```json
    {
      "name": "Sunil Kumar",
      "phone": "9876501234",
      "salary": "15000.00",
      "ot_rate": "100.00",
      "status": "active"
    }
    ```

### 7.2 Attendance
- **List**: `GET /api/v1/tenant/attendance?date=YYYY-MM-DD`
- **Record**: `POST /api/v1/tenant/attendance`
  - Request:
    ```json
    {
      "employee_id": "uuid",
      "date": "2026-10-14",
      "status": "present",
      "ot_amount": "150.00",
      "advance": "200.00"
    }
    ```

### 7.3 Salary Management (`tenant_admin`)
- **Pending Salaries**: `GET /api/v1/tenant/salaries/pending`
- **Employee Statement**: `GET /api/v1/tenant/salaries/:employee_id/statement`
- **Disburse Salary**: `POST /api/v1/tenant/salaries/:employee_id/pay`
  - Request:
    ```json
    {
      "payment_method": "cash",
      "amount": "12000.00",
      "payment_date": "2026-10-14",
      "note": "Monthly payout"
    }
    ```

---

## 8. Cash Book Management

- **Get Authoritative Cash Balance**: `GET /api/v1/tenant/cash/balance`
- **List Cash Transactions**: `GET /api/v1/tenant/cash/transactions?page=1&page_size=20`
- **Add Cash**: `POST /api/v1/tenant/cash/add`
  - Request: `{"amount": "5000.00", "description": "Cash added to till"}`
- **Adjust Cash**: `POST /api/v1/tenant/cash/adjust`
  - Request: `{"actual_balance": "4800.00", "reason": "Cash tally end of day"}`

---

## 9. Reports

- **Daily Operations Report**: `GET /api/v1/tenant/reports/daily?date=YYYY-MM-DD`
- **Daily Report PDF**: `GET /api/v1/tenant/reports/daily/pdf?date=YYYY-MM-DD`
