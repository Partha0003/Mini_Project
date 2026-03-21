# Digital Banking Fraud Detection Simulation Engine (Spring Boot + Thymeleaf)

## 1. What this project is
This is a demo web application that simulates a digital-banking transaction stream and classifies each transaction as:

- `NORMAL`
- `SUSPICIOUS`
- `FRAUD`

It provides:

- A secured admin UI (Thymeleaf) to add/delete transactions and view reports.
- A rule-based fraud scoring engine with an optional ML plug-in hook (currently a dummy implementation).
- REST endpoints to ingest transactions, simulate synthetic transactions, export analytics CSV, and export fraud transactions as Excel.

## 2. Tech Stack
- Backend: Java 21, Spring Boot (MVC + WebMVC + Spring Security + Spring Data JPA)
- Templating/UI: Thymeleaf + Bootstrap (CDN)
- Database: MySQL (JPA/Hibernate)
- Reporting:
  - Excel export via Apache POI
  - CSV export via server-side file generation

## 3. Prerequisites
Install these first:
- Java 21
- Maven
- MySQL (local server)

Quick checks (PowerShell):

```powershell
java -version
mvn -version
```

If both commands print versions, you are ready.

## 4. Beginner-Friendly Step-by-Step: Run the App

### Step 1: Open a terminal in the project module
Go to:

`Digital-Banking-Fraud-Detection-Simulation-Engine-with-frontend--final`

In PowerShell:

```powershell
cd "d:\DL_MINI\Digital-Banking-Fraud-Detection-Simulation-Engine-with-frontend--final"
```

### Step 2: Start MySQL
Make sure your MySQL server is running locally on port `3306`.

### Step 3: Create the database
Create the DB once:

```sql
CREATE DATABASE frauddb;
```

### Step 4: Configure DB username/password
Open:

`src/main/resources/application.properties`

Set these to your local MySQL values:

- `spring.datasource.url=jdbc:mysql://localhost:3306/frauddb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- `spring.datasource.username=YOUR_USERNAME`
- `spring.datasource.password=YOUR_PASSWORD`

> Security note: avoid committing real passwords in this file for production projects.

### Step 5: Build and run the application

```powershell
mvn clean spring-boot:run
```

What should happen:
- Maven downloads dependencies (first run may take a few minutes).
- Spring Boot starts successfully.
- Server listens on port `8082`.

### Step 6: Open the app in browser
Go to:

- `http://localhost:8082/login`

Login with default credentials:
- username: `admin`
- password: `admin123`

After login, you should land on `/dashboard`.

### Step 7: Quick verification (recommended)
From the dashboard:
1. Add one transaction from the form.
2. Confirm it appears in the table with `status`, `riskScore`, and `fraudReason`.
3. Open the report pages from sidebar:
   - Fraud Report
   - Suspicious Report
   - Normal Report

### Step 8: Test one REST endpoint (optional)
If logged in in browser, open:

- `http://localhost:8082/api/gateway/transactions`

You should see JSON (or be prompted to authenticate if not logged in).

### Common startup issues
- **Port already in use**: change `server.port` in `application.properties`.
- **DB connection error**: verify MySQL is running and credentials are correct.
- **Access denied for user**: grant privileges to your MySQL user for `frauddb`.
- **Java version mismatch**: ensure Java 21 is active.

## 6. Authentication (Admin UI)
Spring Security is configured for an in-memory admin user:
- username: `admin`
- password: `admin123`

After login, the UI redirects to `/dashboard`.

### Secured routes
Anything not explicitly permitted requires authentication. The permitted pages include:
- `/`
- `/login`
- `/css/**`
- `/js/**`
- `/images/**`

### How to call REST endpoints (authentication)
Spring Security protects REST endpoints as well (because of `anyRequest().authenticated()`).
Practically, call them either:

- from the logged-in browser session (same cookies), or
- by performing the form login and reusing the returned session cookie in your client.

## 7. UI Routes (Thymeleaf pages)
### Login
- `GET /login`  
  Renders `templates/login.html`.

### Dashboard
- `GET /` and `GET /dashboard`  
  Renders `templates/dashboard.html`.
  Shows summary cards + “Add New Transaction” + full transactions table.

### Reports
- `GET /fraud-report` → `templates/fraud-report.html`
- `GET /suspicious-report` → `templates/suspicious-report.html`
- `GET /normal-report` → `templates/normal-report.html`

### Actions
- `POST /transaction/create`  
  Adds a new transaction from the dashboard form, then redirects to `/dashboard`.
- `GET /transaction/delete/{id}`  
  Deletes transaction by ID, then redirects to `/dashboard`.

### Additional MVC handler
- `POST /transactions/save`
  - This is an MVC form-submit handler (`TransactionController.saveTransaction(...)`) that binds `Transaction` fields from a submitted form.
  - On validation errors it returns the view named `transactions` (there is a `templates/transactions.html` file).
  - On success it persists and redirects to `/dashboard`.

### Excel download
- `GET /download-fraud-report`  
  Streams an Excel file download (`fraud_report.xlsx`) of all fraud transactions.

> Note: the dashboard sidebar uses a link to `/logout`. Spring Security logout is typically configured for `POST /logout`, so if GET logout doesn’t behave as expected, consider using the default Spring Security logout flow.

## 8. Fraud Scoring (Core Logic)
Fraud classification is produced by `FraudDetectionService` when a transaction is created.

### 8.1 Rule-based risk
The engine computes a rule score from:

1. **High amount**
   - If `amount > 50000` → `+50`

2. **Rapid transactions**
   - Looks up the last **2 minutes** of transactions for the same `accountNumber`
   - If there are **>= 3** recent transactions → `+30`

3. **Unusual location**
   - Compares the current `location` with the last stored transaction location for the same account
   - If different → `+20`

### 8.2 Optional ML risk plug-in
`FraudDetectionService.calculateRisk()` adds ML risk when:
- `fraud.ml.enabled=true` (see `application.properties`)
- a `FraudMLPlugin` bean exists

Current implementation: `DummyFraudMLPlugin`:
- If `amount > 60000` → `+40`
- If `location == "UNKNOWN"` (case-insensitive) → `+30`
- capped at 100

### 8.3 Final risk + status
- Final risk = `min(ruleRisk + mlRisk, 100)`
- Status mapping:
  - `riskScore >= 70` → `FRAUD`
  - `riskScore >= 30` → `SUSPICIOUS`
  - else → `NORMAL`

### 8.4 Fraud reason
The stored `fraudReason` is a coarse message based on risk band:
- `>= 70`: `High risk: Large amount or unusual behavior detected`
- `>= 30`: `Moderate risk: Suspicious transaction pattern`
- else: `Normal transaction within safe limits`

## 9. Data Model (Database)
The schema is derived from JPA entities with `spring.jpa.hibernate.ddl-auto=update`.

### 9.1 `transactions` table
Entity: `com.bank.frauddetection.model.Transaction`

Columns (field names as in code):
- `id` (PK, auto-generated)
- `accountNumber` (string, required)
  - validated with regex: `^[A-Z]{4}[0-9A-F]{4}$`
  - setter uppercases the value
- `amount` (double, positive)
- `location` (string)
- `transactionTime` (LocalDateTime)
  - set automatically in `@PrePersist` if missing
- `status` (string: `NORMAL`, `SUSPICIOUS`, `FRAUD`)
- `riskScore` (integer)
- `fraudReason` (string)

### 9.2 `fraud_log` table
Entity: `com.bank.frauddetection.model.FraudLog`

Columns:
- `id` (PK, auto-generated)
- `transactionId` (Long)
- `ruleViolated` (string)
- `riskScore` (int)
- `loggedAt` (LocalDateTime; set in constructor)

> Important: this repo currently does not create/write `FraudLog` records during transaction scoring. The `/api/fraud/logs` endpoint can return empty results unless the table is seeded externally.

## 10. REST API Documentation
All REST endpoints are under Spring Security; non-permitted endpoints require authentication.

### Transaction ingestion
- `POST /api/gateway/transaction`
  - Request body (JSON):
    ```json
    {
      "accountNumber": "ABCD1A2F",
      "amount": 1234.56,
      "location": "Delhi"
    }
    ```
  - Response (JSON): the persisted `Transaction` including `id`, `transactionTime`, `status`, `riskScore`, `fraudReason`.

### List transactions
- `GET /api/gateway/transactions`
  - Response: JSON array of `Transaction`

- `GET /api/gateway/transactions/fraud`
  - Response: JSON array of `Transaction` where `status == "FRAUD"`

### Simulation
- `POST /api/gateway/simulate/{count}`
  - Path parameter:
    - `count` (int) number of synthetic transactions to generate
  - Behavior: generates `count` transactions and persists each via `TransactionService.createTransaction(...)`.
  - Response: plain text `Gateway simulated {count} transactions`

### Analytics CSV export
- `GET /api/analytics/export`
  - Response: `200 OK` with attachment:
    - filename: `fraud-training-data.csv`
    - content-type: `text/csv`
  - CSV columns:
    - `amount,location,riskScore,status`

### Fraud logs
- `GET /api/fraud/logs`
  - Response: JSON array of `FraudLog`

## 11. Known Limitations / Gaps
- `FraudLog` is not written anywhere in the current app logic, so `/api/fraud/logs` is likely empty.
- Dashboard sidebar `Logout` uses a link to `/logout`; Spring Security logout commonly expects `POST /logout`, so logout may not work reliably as-is.
- Some templates exist but are not wired to any controller routes:
  - `templates/transactions.html`
  - `templates/all-transactions.html`
- `templates/all-transactions.html` references a CSS file `@{/css/style.css}`, but `static/css/style.css` does not exist in this repo, which may affect styling.
- `spring.datasource.password` is hardcoded in `application.properties` (security risk).
- Analytics CSV export creates a server-side file `fraud-training-data.csv` in the working directory; concurrent requests can overwrite it.
- Transaction listing uses `findAll()` with no pagination/sorting controls.

## 12. Default URLs Summary
- Login: `GET /login`
- Dashboard: `GET /` or `GET /dashboard`
- Reports:
  - `GET /fraud-report`
  - `GET /suspicious-report`
  - `GET /normal-report`
- Excel download:
  - `GET /download-fraud-report`
- REST:
  - `POST /api/gateway/transaction`
  - `GET /api/gateway/transactions`
  - `GET /api/gateway/transactions/fraud`
  - `POST /api/gateway/simulate/{count}`
  - `GET /api/analytics/export`
  - `GET /api/fraud/logs`

