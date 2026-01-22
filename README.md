# 🛒 DiddyCart — Production-Grade E-commerce Backend

DiddyCart is a production-oriented REST API backend for e-commerce workflows. It provides secure authentication, vendor onboarding, product management, cart operations, order lifecycle handling, and payment simulation with an explicit caching layer for performance.

---

## ✅ Project Overview

- **Type:** RESTful API Backend
- **Language:** Java 25
- **Framework:** Spring Boot 4.0.1
- **Build Tool:** Maven
- **Database:** PostgreSQL
- **Caching:** Redis via Spring Cache
- **Auth:** Stateless JWT (HS256)
- **Containerization:** Docker + Compose

---

## 📦 Tech Stack

**Core**

- Spring Boot (WebMVC)
- Spring Data JPA (Hibernate)
- Spring Security
- Jakarta Validation

**Infrastructure**

- PostgreSQL (Primary datastore)
- Redis (Cache layer)
- Docker (Local infra + containerized app)

**Utilities**

- jjwt (token creation/validation)
- Lombok
- SpringDoc OpenAPI (Swagger UI)

---

## ⚡ Caching Strategy (Redis)

**Provider**: Spring Cache Abstraction + Redis

### Configuration

- **Serializer**: `GenericJackson2JsonRedisSerializer` (JSON, not Java binary)
- **TTL**: 1 hour default for all caches
- **Logging**: `LoggingCacheManager` prints HIT/MISS/PUT/EVICT

### Cache Keys & Patterns

| Cache Name        | Key                | Strategy                   | Notes                             |
| ----------------- | ------------------ | -------------------------- | --------------------------------- |
| `products`        | `productId`        | `@Cacheable`               | Evicted on update/delete          |
| `cart`            | `userId`           | `@Cacheable` / `@CachePut` | Write-heavy updates               |
| `orders`          | `userId_orderId`   | `@Cacheable` / `@CachePut` | Prevents cross-user cache leakage |
| `payments`        | `orderId`          | `@Cacheable`               | Payment lookup                    |
| `user_profile`    | `userId`           | `@Cacheable` / `@CachePut` | Evicted on vendor role change     |
| `vendors_by_user` | `userId`           | `@Cacheable` / `@CachePut` | Vendor profile by user            |
| `vendors`         | `vendorId`         | `@Cacheable` / `@CachePut` | Vendor profile by vendor ID       |
| `address`         | `userId_addressId` | `@Cacheable` / `@CachePut` | Prevents cross-user leakage       |
| `user_addresses`  | `userId`           | `@Cacheable`               | Evicted on address changes        |

### Consistency Notes

- Cart, address, vendor, and order updates are transactional.
- Order/payment updates evict or refresh caches to maintain consistency.

---

## 🧭 Architecture

DiddyCart follows a classic layered architecture:

```text
HTTP Request
  ↓
JwtAuthenticationFilter (token validation, role extraction)
  ↓
Controller (REST endpoints, validation)
  ↓
Service (business logic, caching, transactions)
  ↓
Repository (JPA/DB access)
  ↓
PostgreSQL
```

### Package Layout

- **controller/**: HTTP endpoints, request/response DTOs
- **service/**: business logic, validation, transactions, cache controls
- **repository/**: JPA repositories
- **models/**: entity definitions
- **config/**: security, cache, infrastructure
- **util/**: JWT handling & filters
- **exception/**: centralized error handling

---

## 🔐 Security Model

**Authentication**

- JWT Bearer tokens (HS256). Token subject = `userId` and includes a `role` claim.
- `JwtAuthenticationFilter` extracts the token, validates it, and sets a `UsernamePasswordAuthenticationToken` with the user ID as principal.

**Authorization**

- Configured in `SecurityConfig`:
  - `/api/auth/**` and `/api/products/**` are public
  - `/api/admin/**` is restricted to `ROLE_ADMIN`
  - everything else requires authentication

**Password Security**

- BCrypt hash (default strength)

---

## 🗂️ Domain Model (Core Entities)

- **User** ↔ **Vendor** (1:1) with role escalation to `VENDOR`
- **User** → **Address** (1:N)
- **Vendor** → **Product** (1:N)
- **Product** → **ProductImage** (1:N)
- **User** → **Cart** (1:1)
- **Cart** → **CartItem** (1:N)
- **Order** → **OrderItem** (1:N) with snapshotting of address and price
- **Order** → **Payment** (1:1)

---

## ⚙️ Core Business Flows

### 1) User Registration & Login

1. Validate unique email
2. Persist user with `USER` role
3. Create empty cart
4. Issue JWT (auto-login)

### 2) Vendor Registration

1. Validate GSTIN uniqueness
2. Create vendor profile
3. Promote role → `VENDOR`
4. Issue fresh JWT containing updated role
5. Evict cached user profile

### 3) Product Management

- Vendors can add/update/delete their own products
- Ownership enforced by vendor user ID
- Product images stored on local disk via `FileService`

### 4) Cart Operations

- Cart is persistent in DB
- Add/update/remove items with stock validation
- Cache is updated after write operations

### 5) Order Placement

1. Validate cart is non-empty
2. Verify address ownership
3. Snapshot address & prices
4. Deduct stock per line item
5. Persist order
6. Clear cart

### 6) Payment Processing (Simulated)

- Creates `Payment` with transaction ID
- Updates `Order.paymentStatus` to `COMPLETED`
- Evicts order cache so clients see updated status immediately

---

## 🧪 Error Handling & Validation

- Centralized exception handling via `GlobalExceptionHandler`
- Runtime errors return HTTP 400 with `{ "error": "message" }`
- Validation errors return a map of `field -> errorMessage`

---

## 📂 File Storage (Product Images)

- Uploaded images are stored locally at `./uploads/`
- DB stores a relative path like `/uploads/{filename}`
- On product updates, old images are deleted

---

## 🧩 Configuration

Key configuration values from `application.yaml`:

```
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.data.redis.host=${SPRING_DATA_REDIS_HOST:localhost}
spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}
diddycart.app.jwtSecret=${JWT_SECRET}
diddycart.app.jwtExpirationMs=86400000
```

**Important:** For HS256, `JWT_SECRET` should be at least 32 bytes.

---

## 🧪 Testing

Run test suite:

```
./mvnw test
```

---

## 🚀 Run Locally

### Prerequisites

- JDK 25+
- PostgreSQL
- Docker Desktop (for Redis)

### 1) Configure Environment

```
DB_URL=jdbc:postgresql://localhost:5432/diddycart
DB_USERNAME=your_username
DB_PASSWORD=your_password
JWT_SECRET=your_secure_secret
```

### 2) Start Infrastructure

```
docker compose up -d
```

### 3) Run Application

```
./mvnw clean install
./mvnw spring-boot:run
```

---

## 🐳 Containerized Run (Docker)

```
docker build -t diddycart:latest .
docker run -p 8080:8080 \
 -e DB_URL=... \
 -e DB_USERNAME=... \
 -e DB_PASSWORD=... \
 -e JWT_SECRET=... \
 -e SPRING_DATA_REDIS_HOST=... \
 -e SPRING_DATA_REDIS_PORT=6379 \
 diddycart:latest
```

---

## 📚 API Documentation

Swagger UI is available at:

- **http://localhost:8080/swagger-ui.html**

OpenAPI JSON:

- **http://localhost:8080/api-docs**

---

## 🧰 Operational Notes

- Connection pool configured via HikariCP in `application.yaml`
- `spring.jpa.hibernate.ddl-auto=update` is enabled for dev convenience
- Consider using Flyway or Liquibase for production migrations
- Redis cache TTL is 1 hour by default

---

## ✅ Feature Summary

- JWT-based authentication
- Role-based authorization (USER, VENDOR, ADMIN)
- Vendor onboarding with GSTIN validation
- Product catalog with pagination + search
- Product image upload & cleanup
- Persistent cart with stock checks
- Order placement with address & price snapshotting
- Payment simulation with transaction ID
- Redis-backed caching with explicit eviction/refresh patterns

---

## 🗺️ System Architecture & Database

Comprehensive architecture documentation:

- **System Architecture Diagram**: [docs/architecture-diagram.md](docs/architecture-diagram.md)
- **Data Flow Diagram**: [docs/er-diagram.md](docs/er-diagram.md)
- **Database ERD**: https://drawsql.app/teams/arhan-das/diagrams/diddycart

---

## 📌 Notes & Limitations

- Payment processing is simulated (no external payment gateway)
- Images are stored locally (not on object storage)
- Kafka and Mail dependencies are present but not wired to flows yet

```

```
