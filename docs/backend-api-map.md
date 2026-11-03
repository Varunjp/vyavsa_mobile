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
- **Authorized Roles**: `tenant_admin`, `tenant_user` (returns 403 for `sales` or `office` roles)
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

### 3.1.1 Sales Role Dashboard
- **Route**: `GET /api/v1/tenant/dashboard/sales?date=YYYY-MM-DD`
- **Authorized Roles**: `tenant_admin`, `sales`
- **Response (`SalesDashboardResponse`)**:
  - `business_date`: Authoritative server business date.
  - `currency`: Default `"INR"`.
  - `total_cash_collected`: Total cash collected by sales agents on route today.
  - `total_bank_received`: Total bank/UPI payments collected today.
  - `total_sales_count`: Number of completed line sales today.
  - `total_sales_amount`: Total gross value of line sales today.
  - `outstanding_dues`: Outstanding balance from route customers.

### 3.1.2 Office Role Dashboard
- **Route**: `GET /api/v1/tenant/dashboard/office?date=YYYY-MM-DD`
- **Authorized Roles**: `tenant_admin`, `office`
- **Response (`OfficeDashboardResponse`)**:
  - `business_date`: Authoritative server business date.
  - `currency`: Default `"INR"`.
  - `total_cash_collected`: Counter retail cash received in store drawer.
  - `total_bank_received`: POS / UPI card collections received.
  - `total_attendance`: Total active employees registered today.
  - `present_count`: Number of employees present.
  - `absent_count`: Number of employees marked absent.
  - `half_day_count`: Number of employees on half-day shift.
  - `total_active_employees`: Headcount of all active employees.
  - `total_sales_count`: Number of completed counter retail bills today.
  - `total_sales_amount`: Total gross value of counter retail sales today.

### 3.2 Tenant Profile, Address, Logo & Settings
- **Profile**: `GET /api/v1/tenant/profile`
  - Response includes:
    - `logo_url`: `"/uploads/logos/tenant_...png"` or `null`.
    - `address_line1`: string or null (max 255 chars).
    - `address_line2`: string or null (max 255 chars).
    - `city`: string or null (max 100 chars).
    - `state`: string or null (max 100 chars).
    - `postal_code`: string or null (max 20 chars).
    - `country`: string or null (max 100 chars).
    - `formatted_address`: string or null (joined multi-line or comma-separated company address).
- **Update Profile**: `PUT /api/v1/tenant/profile` (`tenant_admin` only)
  - Request:
    ```json
    {
      "name": "Vyavsa Enterprises",
      "email": "owner@vyavsa.com",
      "phone": "+91 9876543210",
      "address_line1": "Plot 42, Industrial Area Phase 2",
      "address_line2": "Near Metro Pillar 124",
      "city": "Bengaluru",
      "state": "Karnataka",
      "postal_code": "560001",
      "country": "India"
    }
    ```
  - Validation:
    - `name`: Required, max 255 chars.
    - `email`: Required, valid email format.
    - `phone`: Optional, max 50 chars.
    - `address_line1`, `address_line2`: Optional, max 255 chars.
    - `city`, `state`, `country`: Optional, max 100 chars.
    - `postal_code`: Optional, max 20 chars.
- **Get Company Logo**: `GET /api/v1/tenant/logo`
  - Response: `{"logo_url": "/uploads/logos/tenant_...png"}` (or `null`)
- **Upload Company Logo**: `POST /api/v1/tenant/logo` (`tenant_admin` only)
  - Content-Type: `multipart/form-data`
  - Form field: `logo` (binary image data)
  - Validation: Max 2MB (`2 << 20` bytes). MIME magic bytes verified: `image/jpeg`, `image/png`, `image/webp`, `image/gif`. Path traversal containment and sanitization enforced.
  - Response: `{"logo_url": "/uploads/logos/tenant_...png"}`
- **Delete Company Logo**: `DELETE /api/v1/tenant/logo` (`tenant_admin` only)
  - Removes stored logo file and resets branding to default.
  - Response: `{"logo_url": null}`
- **Subscription**: `GET /api/v1/tenant/subscription`
  - Response: Subscription status, plan details, start/end dates, days remaining.

---

## 4. Bank Account Management

All bank account operations require `Authorization: Bearer <access_token>` and tenant context (`tenant_admin` or `tenant_user`).

### 4.1 List Tenant Bank Accounts
- **Route**: `GET /api/v1/tenant/banks?page=1&page_size=100`
- **Auth**: Bearer Token
- **Response**: Array of `TenantBank` objects:
  ```json
  [
    {
      "id": "uuid",
      "tenant_id": "uuid",
      "bank_name": "HDFC Bank",
      "account_number": "50100234567890",
      "ifsc": "HDFC0001234",
      "current_balance": "45000.00",
      "status": "active",
      "created_at": "2026-10-01T10:00:00Z",
      "updated_at": "2026-10-15T12:00:00Z"
    }
  ]
  ```

### 4.2 Create Bank Account
- **Route**: `POST /api/v1/tenant/banks`
- **Auth**: Bearer Token (`tenant_admin` or authorized tenant user)
- **Request Body (`CreateBankRequest`)**:
  ```json
  {
    "bank_name": "HDFC Bank",
    "account_number": "50100234567890",
    "ifsc": "HDFC0001234",
    "opening_balance": "10000.00",
    "status": "active"
  }
  ```
- **Business Rules**:
  - `opening_balance` is credited to the bank ledger automatically upon creation.
  - Subsequent balances are authoritatively tracked and calculated by backend transaction entries (sales, settlements, expenses). Direct arbitrary balance manipulation is prohibited by accounting rules.

### 4.3 Get Bank Account Details
- **Route**: `GET /api/v1/tenant/banks/:id`
- **Auth**: Bearer Token
- **Response**: `TenantBank` object.

### 4.4 Update Bank Account
- **Route**: `PUT /api/v1/tenant/banks/:id`
- **Auth**: Bearer Token
- **Request Body (`UpdateBankRequest`)**:
  ```json
  {
    "bank_name": "HDFC Bank - Current",
    "account_number": "50100234567890",
    "ifsc": "HDFC0001234",
    "status": "active"
  }
  ```

### 4.5 Delete Bank Account
- **Route**: `DELETE /api/v1/tenant/banks/:id`
- **Auth**: Bearer Token
- **Response**: `{"message": "bank account deleted successfully"}`

---

## 5. Line Sales & Counter Sales (Transactions)

### Financial Calculation & Discount Rules
- **Calculation Formula**: `Final Total Amount = Subtotal - Discount Amount`
- **Automatic Contra Adjustment (FIFO)**:
  - When a sale is created for a customer who also has pending supplier payables (`totalPendingPayable > 0`), the backend automatically creates a contra settlement offsetting pending purchases FIFO.
  - Adjustment Formula: `contra_amount = min(dueOnThisSale, totalPendingPayable)`.
  - Sale Response includes:
    - `contra_amount`: Confirmed adjusted amount against supplier payable.
    - `previous_payable`: Party payable before sale creation.
    - `updated_balance`: Customer dues after advance & contra offsets.
  - Unified Party Model:
    - `role`: `"customer"`, `"supplier"`, or `"both"`.
    - `is_customer`: boolean, `is_supplier`: boolean.
    - `total_receivable`: Customer dues owed to business.
    - `total_payable`: Supplier purchase liabilities owed by business.
    - `net_balance`: `|total_receivable - total_payable|`.
    - `net_status`: `"owes_us"`, `"we_owe"`, or `"settled"`.
- **Validation Rules**:
  - `subtotal`: Required, must be >= 0.00.
  - `discount` / `discount_amount`: Non-negative (>= 0.00), must not exceed `subtotal`. Defaults to 0.00 if omitted.
  - `collected_amount`: `cash + bank_amount` cannot exceed `total_amount`.
  - Precision: All monetary calculations follow 2 decimal places with `HALF_UP` rounding.
  - Backward Compatibility: Missing `subtotal` defaults to `total_amount`; missing `discount` defaults to `0.00`.

### 5.1 Line Sales
- **List**: `GET /api/v1/tenant/line-sales?page=1&page_size=20&date=YYYY-MM-DD&search=`
- **Create**: `POST /api/v1/tenant/line-sales`
  - Request:
    ```json
    {
      "customer_id": "uuid",
      "route": "North Route",
      "salesman": "Ramesh",
      "note": "Morning delivery",
      "subtotal": "1600.00",
      "discount": "100.00",
      "discount_amount": "100.00",
      "total_amount": "1500.00",
      "total_cash_in": "500.00",
      "cash_amount": "500.00",
      "bank_amount": "1000.00",
      "bank_id": "optional-uuid-primary-bank",
      "bank_payments": [
        {
          "bank_id": "uuid-bank-1",
          "bank_name": "HDFC Bank",
          "amount": "600.00",
          "note": "Cheque #123"
        },
        {
          "bank_id": "uuid-bank-2",
          "bank_name": "State Bank of India",
          "amount": "400.00"
        }
      ],
      "items": [
        {
          "product_id": "uuid",
          "quantity": "2.00",
          "unit_price": "800.00"
        }
      ]
    }
    ```
- **Update**: `PUT /api/v1/tenant/line-sales/:id`
- **Details**: `GET /api/v1/tenant/line-sales/:id`
  - Response includes `subtotal`, `discount`, `total_amount`, `total_cash_in`, `bank_amount`, `balance`, `items`, `payments`.
- **Payments**: `GET /api/v1/tenant/line-sales/:id/payments`
- **Add Payment**: `POST /api/v1/tenant/line-sales/:id/payments`
- **Payment Breakdown**: `GET /api/v1/tenant/line-sales/breakdown?date=YYYY-MM-DD&start_date=YYYY-MM-DD&end_date=YYYY-MM-DD`
  - Response:
    ```json
    {
      "success": true,
      "message": "line sale payment breakdown retrieved",
      "data": {
        "total_sales": "85000.00",
        "cash_received": "25000.00",
        "bank_received": "60000.00",
        "total_received": "85000.00",
        "advance_applied": "0.00",
        "payable_applied": "0.00",
        "contra_amount": "0.00",
        "total_due": "0.00",
        "sales_count": 3,
        "start_date": "2026-11-02",
        "end_date": "2026-11-02",
        "is_reconciled": true,
        "bank_breakdown": [
          {
            "bank_id": "uuid-bank-1",
            "bank_name": "State Bank of India",
            "account_number": "1234567890",
            "amount": "30000.00",
            "percentage": 50.0
          }
        ]
      }
    }
    ```

### 5.2 Counter Sales
- **List**: `GET /api/v1/tenant/counter-sales?page=1&page_size=20&date=YYYY-MM-DD&search=`
- **Create**: `POST /api/v1/tenant/counter-sales`
  - Request:
    ```json
    {
      "item": "Product A",
      "price": "300.00",
      "subtotal": "300.00",
      "discount": "50.00",
      "discount_amount": "50.00",
      "total_amount": "250.00",
      "payment_method": "split",
      "cash": "100.00",
      "cash_amount": "100.00",
      "bank_amount": "150.00",
      "bank_id": "optional-uuid-primary-bank",
      "bank_payments": [
        {
          "bank_id": "uuid-bank-1",
          "bank_name": "HDFC Bank",
          "amount": "150.00"
        }
      ],
      "account": "counter"
    }
    ```
- **Update**: `PUT /api/v1/tenant/counter-sales/:id`
- **Details**: `GET /api/v1/tenant/counter-sales/:id`
  - Response includes `item`, `price`, `subtotal`, `discount`, `total_amount`, `payment_method`, `cash`, `bank_amount`, `account`, `items`, `payments`.
- **Payments**: `GET /api/v1/tenant/counter-sales/:id/payments`
- **Add Payment**: `POST /api/v1/tenant/counter-sales/:id/payments`

---

## 6. Purchases & Supplier Dues

### 6.1 Purchases
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
- **Update**: `PUT /api/v1/tenant/purchases/:id`
  - Request: `{"customer_id": "optional-uuid", "item": "Updated Item", "quantity": 10, "total_amount": "5000.00", "total_paid": "2000.00"}`
- **Delete**: `DELETE /api/v1/tenant/purchases/:id` (`tenant_admin` or creator)
- **Payment History**: `GET /api/v1/tenant/purchases/:id/payments`
- **Record Settlement Payment**: `POST /api/v1/tenant/purchases/:id/payments`
  - Request: `{"amount": "1000.00", "payment_method": "cash", "bank_id": "optional-uuid", "payment_date": "2026-10-15", "note": "Settlement"}`

### 6.2 Customer & Supplier Ledger (Dues)
- **List Customers**: `GET /api/v1/tenant/customers?page=1&page_size=20&search=` (`tenant_admin` or `tenant_user`)
- **Customer Details**: `GET /api/v1/tenant/customers/:id`
- **Create Customer**: `POST /api/v1/tenant/customers`
- **Update Customer**: `PUT /api/v1/tenant/customers/:id`
- **Delete Customer**: `DELETE /api/v1/tenant/customers/:id` (`tenant_admin` only)
- **Customer Balance**: `GET /api/v1/tenant/customers/:id/balance`
  - Returns: `current_balance`, `total_purchases`, `total_paid`, `outstanding_payable`
- **Customer Statement**: `GET /api/v1/tenant/customers/:id/statement`
  - Returns chronological statement with running balance and entries.
- **Customer Previous Sales**: `GET /api/v1/tenant/customers/:id/sales`
  - Returns array of customer `LineSale` records.
- **Manual Balance Adjustment**: `POST /api/v1/tenant/customers/:id/adjust-balance` (`tenant_admin` only)
  - Request: `{"new_balance": "500.00", "reason": "Audit adjustment"}`
- **List Adjustments**: `GET /api/v1/tenant/customers/:id/adjustments`
  - Returns audit trail of customer balance adjustments with previous balance, new balance, and reason.
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

## 7. Expenses

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
  - **Employee Advance via Expense**:
    When `category` is set to `"employee_advance"`, provide `"employee_id": "uuid"`. The backend records the expense, creates a ledger entry in `employee_advances`, updates daily attendance, and adjusts the employee's pending salary balance automatically.
- **Details**: `GET /api/v1/tenant/expenses/:id`
- **Update**: `PUT /api/v1/tenant/expenses/:id`
- **Delete**: `DELETE /api/v1/tenant/expenses/:id`

---

## 8. Employees, Attendance & Salary (Operational Staff)

*Employees are physical staff members on payroll. They do not have system login credentials unless explicitly created as Tenant Users.*

### 8.1 Employees
- **List**: `GET /api/v1/tenant/employees?page=1&page_size=20&search=`
- **Create**: `POST /api/v1/tenant/employees` (`tenant_admin`)
  - Request: `{"name": "...", "phone": "...", "salary": "700.00", "ot_rate": "50.00"}`
  - `salary`: Optional (`*decimal.Decimal`). Omit or pass `null` for employees on variable daily wages. Does NOT default to 0.00.
- **Update**: `PUT /api/v1/tenant/employees/:id` (`tenant_admin`)
  - Request: `{"name": "...", "phone": "...", "salary": "...", "clear_salary": false, "ot_rate": "...", "status": "active"}`
  - `clear_salary`: Set to `true` to remove employee's configured default salary and switch to daily wages.
- **Delete**: `DELETE /api/v1/tenant/employees/:id` (`tenant_admin`)

### 8.2 Attendance & Advances
- **List**: `GET /api/v1/tenant/attendance?date=YYYY-MM-DD`
  - Response records include `daily_salary` and `salary_source`:
    - `"default"`: Prefilled from employee default base salary.
    - `"override"`: Administrator explicitly modified daily wage for this date.
    - `"manual"`: Employee has no default salary; daily wage entered manually.
    - `"pending"`: Daily wage not yet specified / attendance marked without wage.
    - `"unpaid"`: Zero earnings for absent or leave days.
- **Record / Upsert**: `POST /api/v1/tenant/attendance`
  - Request:
    ```json
    {
      "employee_id": "uuid",
      "date": "2026-10-14",
      "status": "present",
      "daily_salary": "900.00",
      "ot": 0,
      "ot_amount": "150.00",
      "advance": "200.00"
    }
    ```
  - `daily_salary`: Optional per-date wage override. Does not modify employee's default salary.
- **Update Existing Attendance**: `PUT /api/v1/tenant/attendance/:id` (`tenant_admin` only)
  - Dedicated endpoint to update existing attendance record.
  - Request:
    ```json
    {
      "status": "half_day",
      "daily_salary": "350.00",
      "clear_daily_salary": false,
      "ot": 0,
      "ot_amount": "0.00",
      "advance": "50.00"
    }
    ```
  - `clear_daily_salary`: Set to `true` to clear the daily wage and mark salary as pending/unpaid.

- **Record Employee Advance Entry**: `POST /api/v1/tenant/attendance/advance`
  - Authorized Roles: `tenant_admin`, `office`
  - Request:
    ```json
    {
      "employee_id": "uuid",
      "date": "2026-10-20",
      "amount": "1500.00",
      "payment_method": "cash",
      "bank_id": null,
      "bank_name": null,
      "reference_id": null,
      "note": "Emergency advance"
    }
    ```
  - Response: Updated attendance record reflecting advance.

- **Record Employee Overtime Entry**: `POST /api/v1/tenant/attendance/overtime`
  - Authorized Roles: `tenant_admin`, `office`
  - Request:
    ```json
    {
      "employee_id": "uuid",
      "date": "2026-10-20",
      "amount": "250.00",
      "ot": "3.5",
      "reference_id": null,
      "note": "Night inventory packing"
    }
    ```
  - Response: Updated attendance record reflecting overtime.

### 8.3 Salary Management (`tenant_admin` only)
- **Pending Salaries**: `GET /api/v1/tenant/salaries/pending`
- **Employee Statement**: `GET /api/v1/tenant/salaries/:employee_id/statement`
- **Disburse Salary**: `POST /api/v1/tenant/salaries/:employee_id/pay`

---

## 9. Tenant Users & RBAC (System Accounts)

*Tenant users are application authentication accounts. Strictly separated from physical employees.*

- **List Tenant Users**: `GET /api/v1/tenant/users?page=1&page_size=100&search=&role=&status=` (`tenant_admin` only)
- **Create Tenant User**: `POST /api/v1/tenant/users` (`tenant_admin` only)
  - Request:
    ```json
    {
      "name": "Branch Manager",
      "email": "manager@store.com",
      "password": "strongPassword123",
      "role": "admin"
    }
    ```
- **User Details**: `GET /api/v1/tenant/users/:id` (`tenant_admin` only)
- **Update Tenant User**: `PUT /api/v1/tenant/users/:id` (`tenant_admin` only)
  - Request:
    ```json
    {
      "name": "Updated Name",
      "role": "user",
      "status": "inactive"
    }
    ```
- **Delete Tenant User**: `DELETE /api/v1/tenant/users/:id` (`tenant_admin` only)

---

## 10. Subscriptions & Plans

- **Public Plans Catalog**: `GET /api/v1/plans`
  - Lists available subscription tiers (`plan_name`, `price`, `duration_days`, `features`).
- **Active Tenant Subscription**: `GET /api/v1/tenant/subscription`
  - Returns current active subscription (`current_plan_name`, `status`, `days_remaining`, `is_expired`, `price`).
- **Purchase / Upgrade Plan**: `POST /api/v1/tenant/subscription/purchase` (`tenant_admin` only)
  - Request: `{"plan_id": "uuid-plan", "payment_method": "upi"}`
- **Billing History Transactions**: `GET /api/v1/tenant/subscription/transactions` (`tenant_admin` only)
  - Returns subscription ledger (`transaction_id`, `plan_name`, `amount`, `payment_method`, `status`, `created_at`).

---

## 11. Cash Book Management

- **Get Authoritative Cash Balance**: `GET /api/v1/tenant/cash/balance`
- **List Cash Transactions**: `GET /api/v1/tenant/cash/transactions?page=1&page_size=20`
- **Add Cash**: `POST /api/v1/tenant/cash/add`
  - Request: `{"amount": "5000.00", "description": "Cash added to till"}`
- **Adjust Cash**: `POST /api/v1/tenant/cash/adjust`
  - Request: `{"actual_balance": "4800.00", "reason": "Cash tally end of day"}`

---

## 12. Reports

- **Daily Operations Report**: `GET /api/v1/tenant/reports/daily?date=YYYY-MM-DD`
- **Daily Report PDF**: `GET /api/v1/tenant/reports/daily/pdf?date=YYYY-MM-DD`

---

## 13. Product & Inventory Catalog Management

All product catalog operations require `Authorization: Bearer <access_token>` and tenant context.

### 13.1 List Products
- **Route**: `GET /api/v1/tenant/products?page=1&page_size=20&search=&status=` (also `/api/v1/products`)
- **Auth**: Tenant Member (`tenant_admin` or `tenant_user`)
- **Response**: Array of `ProductResponse` objects:
  ```json
  [
    {
      "id": "uuid",
      "tenant_id": "uuid",
      "name": "Standard Milk 500ml",
      "unit_price": "28.00",
      "status": "active",
      "created_at": "2026-10-15T10:00:00Z",
      "updated_at": "2026-10-15T10:00:00Z"
    }
  ]
  ```

### 13.2 Create Product
- **Route**: `POST /api/v1/tenant/products` (also `/api/v1/products`)
- **Auth**: `tenant_admin` only
- **Request Body (`CreateProductRequest`)**:
  ```json
  {
    "name": "Full Cream Milk 500ml",
    "unit_price": "34.00"
  }
  ```
- **Response**: `ProductResponse` object.

### 13.3 Get Product by ID
- **Route**: `GET /api/v1/tenant/products/:id` (also `/api/v1/products/:id`)
- **Auth**: Tenant Member
- **Response**: `ProductResponse` object.

### 13.4 Update Product
- **Route**: `PUT /api/v1/tenant/products/:id` (also `/api/v1/products/:id`)
- **Auth**: `tenant_admin` only
- **Request Body (`UpdateProductRequest`)**:
  ```json
  {
    "name": "Full Cream Milk 500ml",
    "unit_price": "35.00",
    "status": "active"
  }
  ```
- **Response**: `ProductResponse` object.

### 13.5 Delete Product
- **Route**: `DELETE /api/v1/tenant/products/:id` (also `/api/v1/products/:id`)
- **Auth**: `tenant_admin` only
- **Business Rules**:
  - If referenced by historical sales items, automatically archived (`status = archived`) to preserve historical accounting integrity.
  - If unreferenced, hard deleted.
- **Response**: `{"deleted": true}`

---

## 14. Product Sales Analytics

- **Route**: `GET /api/v1/tenant/products/analytics?start_date=YYYY-MM-DD&end_date=YYYY-MM-DD&limit=10`
  (also `/api/v1/tenant/analytics/products` and `/api/v1/products/analytics`)
- **Auth**: Tenant Member (`tenant_admin` or `tenant_user`)
- **Query Parameters**:
  - `start_date`: Optional YYYY-MM-DD
  - `end_date`: Optional YYYY-MM-DD
  - `limit`: Optional integer (default 10, max 100)
- **Response (`ProductAnalyticsResponse`)**:
  ```json
  {
    "summary": {
      "total_quantity_sold": "240.00",
      "total_revenue": "8160.00",
      "distinct_products_count": 8,
      "top_product_name": "Full Cream Milk 500ml"
    },
    "top_by_quantity": [
      {
        "rank": 1,
        "product_id": "uuid",
        "product_name": "Full Cream Milk 500ml",
        "quantity_sold": "120.00",
        "revenue": "4080.00"
      }
    ],
    "top_by_revenue": [
      {
        "rank": 1,
        "product_id": "uuid",
        "product_name": "Full Cream Milk 500ml",
        "quantity_sold": "120.00",
        "revenue": "4080.00"
      }
    ],
    "daily_sales": [
      {
        "sale_date": "2026-10-18",
        "daily_quantity": "80.00",
        "daily_revenue": "2720.00"
      }
    ],
    "start_date": "2026-10-12",
    "end_date": "2026-10-19"
  }
  ```

