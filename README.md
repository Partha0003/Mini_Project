# Digital Banking Fraud Detection System

Simple fraud monitoring app with login, dashboards, reports, alerts, and rule-based + ML-based fraud checks.

---

## 1) Quick Start (Non-Developer Friendly)

1. Start MySQL and create database:
   ```sql
   CREATE DATABASE frauddb;
   ```
2. Open `src/main/resources/application.properties` and set your DB username/password.
3. Run project:
   ```powershell
   mvn clean spring-boot:run
   ```
4. Open:
   - [http://localhost:8082/login](http://localhost:8082/login)

---

## 2) Login Credentials

### Admin
- Username: `admin`
- Password: `admin123`

### User
- Username: `user`
- Password: `user123`

---

## 3) What Each Role Can Access

### ADMIN can:
- Open normal dashboard and admin dashboard
- Create transactions
- Delete transactions
- See and download fraud reports
- Use admin analytics APIs
- Use admin export APIs

### USER can:
- Open normal dashboard
- Create transactions
- View reports

### USER cannot:
- Open admin dashboard
- Delete transactions
- Download admin/fraud exports
- Access admin APIs

If unauthorized, app shows custom `403 Access Denied` page.

---

## 4) Fraud Detection Rules (Core Rules)

When a transaction is created, risk is calculated using these rules:

1. **HIGH_AMOUNT**
   - Condition: amount > `50000`
   - Risk: `+50`

2. **RAPID_TRANSACTION**
   - Condition: 3 or more transactions in last 2 minutes for same account
   - Risk: `+30`

3. **LOCATION_MISMATCH**
   - Condition: location is different from previous transaction location
   - Risk: `+20`

---

## 5) ML Plugin Rules (Optional)

If ML is enabled (`fraud.ml.enabled=true`), dummy ML plugin adds extra risk:

1. amount > `60000` -> `+40`
2. location = `UNKNOWN` -> `+30`

ML contribution is logged as `ML_RULE`.

---

## 6) Final Status Logic

After total risk (max 100):

- `>= 70` -> `FRAUD`
- `>= 30` and `< 70` -> `SUSPICIOUS`
- `< 30` -> `NORMAL`

---

## 7) Explainable Fraud Reason

Reason is generated dynamically from triggered rules.

Example:

`High risk due to:`
- `Transaction amount exceeds ₹50,000`
- `ML model flagged unusual behavior`

System also stores rule-level fraud logs and rule count for each transaction.

---

## 8) Main Pages

- `/login` - Login page
- `/dashboard` - Main dashboard (all users)
- `/admin/dashboard` - Admin analytics dashboard (admin only)
- `/fraud-report` - Fraud transactions
- `/suspicious-report` - Suspicious transactions
- `/normal-report` - Normal transactions

---

## 9) Key APIs

- `GET /api/admin/analytics` (admin only)
- `GET /api/admin/alerts` (admin only)
- `GET /api/fraud/logs`
- `POST /api/gateway/transaction`
- `GET /api/gateway/transactions`
- `POST /api/gateway/simulate/{count}`

---

## 10) Notes

- Live fraud alerts are in-memory (keeps latest 10 alerts, demo-friendly).
- For production, move credentials and secrets to environment variables.

