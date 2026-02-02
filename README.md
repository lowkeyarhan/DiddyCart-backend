# 🛒 DiddyCart — Enterprise E-Commerce Backend

DiddyCart is a robust, production-ready e-commerce backend built with **Spring Boot 4** and **Java 21/25**. It is designed to handle high-concurrency workflows including user identity management, product cataloging, secure payment processing, and order fulfillment. The system follows a **Domain-Driven Design (DDD)** approach with a modular monolith structure, leveraging **Kafka** for event-driven asynchronous processing and **Redis** for high-performance caching. It is fully containerized with **Docker** and orchestration-ready for **Kubernetes**, ensuring scalability and resilience for enterprise environments.

---

## 🛠 Tech Stack & Tools

| Category             | Technology        | Description                                     |
| -------------------- | ----------------- | ----------------------------------------------- |
| **Core**             | Java 21 / 25      | Latest LTS features (Records, Pattern Matching) |
| **Framework**        | Spring Boot 4.0.1 | Modern application framework                    |
| **Database**         | PostgreSQL        | Primary relational data store                   |
| **Caching**          | Redis             | Session management & data caching               |
| **Messaging**        | Apache Kafka      | Event streaming & decoupling services           |
| **Security**         | Spring Security   | AuthZ/AuthN with JWT                            |
| **Containerization** | Docker, K8s       | Deployment & orchestration                      |
| **Monitoring**       | Prometheus        | Metrics collection                              |
| **Testing**          | JUnit 5           | Unit & Integration testing                      |

---

## 🚀 Features & Workflow

### Core Features

- **Identity Management:** Secure user registration & login (JWT).
- **Product Catalog:** Advanced search, categorization, and inventory tracking.
- **Cart Operations:** Persistent cart management using Redis.
- **Order Lifecycle:** State machine for order status (Placed -> Shipped -> Delivered).
- **Payment Processing:** Integrated mock payment gateway with failover logic.
- **Notifications:** Async email dispatch for account activities.

### End-to-End Workflow

1. **Browse:** User checks `GET /api/products` (Cached).
2. **Authentication:** User logs in via `POST /api/auth/login` -> receives JWT.
3. **Cart:** User adds items to cart `POST /api/cart`.
4. **Checkout:** `POST /api/orders` triggers order creation in PostgreSQL.
5. **Payment:** Order logic initiates payment; updates status via callback.
6. **Async Event:** strictly after transaction commit, `order-placed` event is pushed to Kafka.
7. **Post-Processing:** Consumers pick up the event to:
   - Deduct inventory.
   - Send order confirmation email (via Mailtrap).
   - Notify vendor dashboard.

---

## 🔒 Security Model

The system implements a **Zero Trust** inspired security model using `SecurityConfig.java`.

### Role-Based Access Control (RBAC)

- **ROLE_USER:** Can buy products, view own orders.
- **ROLE_VENDOR:** Can create products, view sales analytics.
- **ROLE_ADMIN:** Full system access.

### Protected Routes

All endpoints are secured by default, with specific exceptions:

- 🟢 **Public:** `/api/auth/**`, `/api/products/**` (Read-only), `/payment.html`.
- 🟡 **Authenticated:** `/api/orders/**`, `/api/cart/**`.
- 🔴 **Admin/Vendor Only:** `/api/admin/**`, `/api/products` (Create/Update).

_Security is enforced via `JwtAuthenticationFilter` which parses the `Authorization: Bearer <token>` header._

---

## ⚠️ Error Handling

Centralized exception handling is managed by `GlobalExceptionHandler.java`.

- **Validation Errors:** Returns `400 Bad Request` with a map of field names and error messages.
- **Business Exceptions:** Custom exceptions like `ResourceNotFoundException` or `InsufficientStockException` return structured JSON.
- **System Errors:** Generic 500 errors are masked to prevent leaking stack traces.

**Example Error Response:**

```json
{
  "timestamp": "2024-02-03T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product with ID 55 not found",
  "path": "/api/products/55"
}
```

---

## 📨 Kafka Messaging & Multithreading

### Event-Driven Logic

Kafka is used to decouple the "write-heavy" order path from "read-heavy" notification systems.

- **Topic:** `user-registration` (2 partitions) -> Welcome Email.
- **Topic:** `order-placed` (2 partitions) -> Inventory Update, Email, Analytics.
- **Topic:** `payment-failed` -> Retries or Admin Alert.

### Multithreading Operations

High-throughput tasks are handled by a custom `ThreadPoolTaskExecutor` (`kafkaWorkerPool`).

- **Core Pool:** 5 Threads
- **Max Pool:** 10 Threads
- **Queue Capacity:** 100
  This ensures that email sending or heavy calculations do not block the main HTTP request threads.

---

## 💾 Database Schema

The PostgreSQL schema is designed for 3rd Normal Form (3NF) compliance.

- **Normalization:**
  - `users` table handles creds; `address` table handles shipping info (1:N).
  - `products` and `category` are separated to allow flexible catalog updates.
- **Indexing:**
  - `idx_users_email` (Unique, B-Tree) for fast login lookups.
  - `idx_products_price` for efficient filtering during browsing.
  - `idx_orders_user_id` for quick order history retrieval.

---

## 🏗 How to Run

### 1. Build the Project

```bash
./mvnw clean install
./mvnw clean package -DskipTests
```

### 2. Run Tests

```bash
./mvnw test
```

### 3. Docker Deployment

Rebuild the image and start the entire stack:

```bash
# Build and start the application image
docker compose up -d --build
```

---

## 🌍 Environment Variables

Create a `.env` file in the root directory (or set via export):

```bash
# Database Configuration
DB_URL="Your_DB URL"
DB_USERNAME="Your_DB_username"
DB_PASSWORD="Your_DB_pass"

# JWT Configuration
JWT_SECRET="secret"
JWT_EXPIRATION_MS=0000

# Redis Configuration (for Docker)
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379

# mail Configuration
MAIL_USERNAME="username"
MAIL_PASSWORD="password"

# Razorpay Configuration
key_id="rzp_xxxx"
key_secret="secret"
```

---

## 📖 API Documentation

Overview of key endpoints (Swagger UI: `http://localhost:8080/swagger-ui.html`):

---

## 🏛 System Architecture

- **System Architecture Diagram**: [docs/architecture-diagram.md](docs/architecture-diagram.md)
- **Data Flow Diagram**: [docs/er-diagram.md](docs/er-diagram.md)
- **Database ERD**: https://drawsql.app/teams/arhan-das/diagrams/diddycart

The system is architected as a **Modular Monolith**:

1. **API Layer (Controllers):** REST entry points.
2. **Service Layer:** Business rules & Transaction boundaries (`@Transactional`).
3. **Data Layer (Repository):** JPA Interfaces interacting with Postgres.
4. **Infrastructure:** Redis (Cache) and Kafka (Events) integration.
