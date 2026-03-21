# Digital Banking Fraud Detection System

Beginner-friendly fraud monitoring web app for demo/prototype use.

- Backend: Spring Boot + MySQL
- Frontend: Thymeleaf + Bootstrap + Chart.js
- Purpose: detect risky banking transactions, explain why they were flagged, and monitor in dashboards

---

## 1) Quick Start (For Non-Developers)

1. Install and start MySQL.
2. Create database:
   ```sql
   CREATE DATABASE frauddb;
   ```
3. Open `src/main/resources/application.properties` and set DB username/password.
4. Run app:
   ```powershell
   mvn spring-boot:run
   ```
5. Open browser:
   - [http://localhost:8082/login](http://localhost:8082/login)

---

## 2) Login Credentials

- **Admin**
  - Username: `admin`
  - Password: `admin123`
- **User**
  - Username: `user`
  - Password: `user123`

---

## 3) Role Access (Who Can Do What)

- **ADMIN can**
  - Open Transaction Details and Admin Dashboard
  - Add/Delete transactions
  - View rule logs
  - Download Excel reports
  - Use admin analytics and alert APIs
- **USER can**
  - Open Transaction Details
  - Add transactions
  - View reports
  - View rule logs for transactions
- **USER cannot**
  - Open admin dashboard
  - Delete transactions
  - Use admin export/admin APIs

Unauthorized access shows custom `403` page.

---

## 4) Main Pages

- `/login` - login screen
- `/dashboard` - Transaction Details page (main table + add transaction form)
- `/admin/dashboard` - admin analytics, filters, alerts, export
- `/fraud-report` - fraud-only report
- `/suspicious-report` - suspicious-only report
- `/normal-report` - normal-only report

---

## 5) Key Features Implemented

### A) Fraud Detection + Classification
- Rule-based risk scoring with optional ML boost.
- Final status:
  - `FRAUD` if score `>= 70`
  - `SUSPICIOUS` if `30-69`
  - `NORMAL` if `< 30`

### B) Explainable Fraud Logging
- Every triggered rule is stored per transaction.
- Each log includes: rule name, rule risk contribution, timestamp.
- “View Logs” button available in Transaction Details and Admin table.

### C) Transaction Details UX
- Add transaction form with inline validation:
  - account must be 8 alphanumeric chars
  - amount must be positive
- Table supports sorting, pagination, and status-colored rows.
- Serial row numbers remain sequential visually even after delete.
- Admin-only delete action.
- Admin-only “Simulate 10 Transactions” button.

### D) Admin Dashboard
- Advanced filters: status, from date, to date, account.
- Pagination and filtered transaction table.
- Export filtered data to Excel.
- Analytics cards + charts:
  - status distribution (pie)
  - transaction trend (line)
  - KPI cards (total/fraud/high risk/fraud rate)
  - insights cards (highest risk, latest fraud, rate delta)
  - top risky accounts table
- Pie chart click interaction:
  - click FRAUD/SUSPICIOUS/NORMAL slice -> auto-filter table by that status

### E) Live Fraud Alerts
- Polling-based real-time alerts on admin page (no websocket complexity).
- Global mute/unmute alerts.
- Per-alert mute/unmute.
- “Last refreshed” indicator + manual refresh.
- Alert sound on new fraud (browser-permission dependent).

### F) Reports
- Fraud / Suspicious / Normal report pages with pagination/sorting.
- Excel download buttons for all report types (admin protected).

### G) Sidebar Improvements
- Grouped navigation:
  - Monitoring
  - Reports
  - Session
- Active page highlighting.

### H) Account Number Standardization
- Existing and new account numbers are normalized to 8 chars for consistency.
- Helps keep same-account relationships consistent across old/new entries.

---

## 6) Fraud Rules Used

### Core Rules
1. `HIGH_AMOUNT`:
   - amount > `50000`
   - score `+50`
2. `RAPID_TRANSACTION`:
   - 3+ transactions for same account in 2 minutes
   - score `+30`
3. `LOCATION_MISMATCH`:
   - location differs from previous transaction for same account
   - score `+20`

### Optional ML Rules (`fraud.ml.enabled=true`)
- amount > `60000` -> `+40`
- location = `UNKNOWN` -> `+30`
- logged as `ML_RULE`

---

## 7) APIs (Important)

- `GET /api/admin/analytics` (ADMIN)
- `GET /api/admin/alerts` (ADMIN)
- `GET /api/fraud/logs` (ADMIN)
- `GET /api/fraud/logs/{transactionId}` (ADMIN/USER)
- `POST /api/gateway/transaction`
- `GET /api/gateway/transactions`
- `POST /api/gateway/simulate/{count}`

---

## 8) Files Added for Ops/DB

- `db-indexing-suggestions.sql`
  - index suggestions for `status`, `transaction_time`, `account_number`
- `db-account-normalization.sql`
  - one-time SQL normalization for account numbers (8-char format)

---

## 9) Current Limitations (Prototype)

- Uses in-memory alert storage (latest 10), not persistent queueing.
- No formal automated test suite yet.
- Some admin actions are still demo-focused (simulation endpoint behavior).
- For production, move DB secrets to environment variables/secret manager.

---

## 10) Troubleshooting

- If login page shows error:
  - check MySQL is running
  - check DB credentials in `application.properties`
  - confirm database `frauddb` exists
- If no alert sound:
  - browser may block autoplay audio until user interaction

