# Split Bill API

Spring Boot (Java 21, Maven) REST API for managing shared group expenses and calculating settlements.

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

The API listens on port **4110**.
