# Split Bill API

Spring Boot (Java 21, Maven) REST API for managing shared group expenses and calculating settlements.

## Personalization

- **GitHub Username:** `hadisuria`
- **Calculated `service_charge_pct`:** `4`
  - Lowercase ASCII sum of `hadisuria`: `104 + 97 + 100 + 105 + 115 + 117 + 114 + 105 + 97 = 954`
  - `954 % 10 = 4` (4%)

---

## How to Build and Run

### Run locally with Maven Wrapper
```bash
./mvnw spring-boot:run
```

### Run tests
```bash
./mvnw test
```

### Build and run with Docker
```bash
docker build -t split-bill-api .
docker run -p 4110:4110 split-bill-api
```

- **Base URL:** `http://localhost:4110`
- **Swagger UI:** `http://localhost:4110/swagger-ui.html`
- **H2 Console:** `http://localhost:4110/h2-console` (JDBC URL: `jdbc:h2:mem:splitbilldb`, User: `sa`, Password: empty)

---

## Example `curl` Commands

### 1. List Bill Groups
```bash
curl http://localhost:4110/api/v1/groups
```

### 2. Create a Bill Group
```bash
curl -X POST http://localhost:4110/api/v1/groups \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Yogyakarta Roadtrip",
    "participants": ["Hadi", "Fajar", "Laras", "Dimas"]
  }'
```
*(Use the returned `id`, e.g. `grp_...`, as `{groupId}` in the requests below.)*

### 3. Get Bill Group Details
```bash
curl http://localhost:4110/api/v1/groups/{groupId}
```

### 4. Add an Expense (`EQUAL`, `PERCENTAGE`, or `EXACT`)

**Equal split:**
```bash
curl -X POST http://localhost:4110/api/v1/groups/{groupId}/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Car Rental",
    "amount": 500000.00,
    "paidBy": "Hadi",
    "splitType": "EQUAL",
    "splitAmong": ["Hadi", "Fajar", "Laras", "Dimas"]
  }'
```

**Percentage split:**
```bash
curl -X POST http://localhost:4110/api/v1/groups/{groupId}/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Homestay Kotagede",
    "amount": 400000.00,
    "paidBy": "Fajar",
    "splitType": "PERCENTAGE",
    "shares": {
      "Hadi": 40,
      "Fajar": 20,
      "Laras": 20,
      "Dimas": 20
    }
  }'
```

**Exact split:**
```bash
curl -X POST http://localhost:4110/api/v1/groups/{groupId}/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Gudeg Yu Djum",
    "amount": 165000.00,
    "paidBy": "Laras",
    "splitType": "EXACT",
    "shares": {
      "Hadi": 55000.00,
      "Fajar": 60000.00,
      "Laras": 50000.00
    }
  }'
```

### 5. Get Settlement Summary
```bash
curl http://localhost:4110/api/v1/groups/{groupId}/settlement
```

### 6. List Group Audit Logs *(Dev/Preview Only)*
```bash
curl http://localhost:4110/api/v1/groups/{groupId}/audit-logs
```

---

## Submission Question

> **"What was the hardest design decision you made while building this, and what trade-off did you accept?"**

The hardest challenge was deciding whether to compute each person's share when an expense is added or calculate all splits during settlement. I chose to calculate and save each person's `BigDecimal` share upfront, including any leftover cent from uneven splits for example 100.00 divided by 3, so the shares always add up to the exact total. That means storing more rows and not allowing edits to expenses or group members once they are saved. The upside is that settlement stays simple and reliable since it only sums up those saved shares.
