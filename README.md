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

## ⚙️ Advanced Concurrency & Data Integrity

DiddyCart handles high-concurrency scenarios (e.g., "Flash Sales") where thousands of users might attempt to purchase the same limited inventory simultaneously. We employ strict locking mechanisms and deadlock prevention strategies to ensure data consistency without compromising data integrity.

### 1. Handling Race Conditions (Pessimistic Locking)

To prevent "Overselling", "Lost Updates", and "Duplicate Payments," DiddyCart bypasses standard Optimistic Locking in favor of **Pessimistic Write Locks** (`SELECT ... FOR UPDATE`) for critical write operations.

- **Inventory Overselling:**
  - **Problem:** Two users try to buy the last unit of a product simultaneously. Both read stock=1, both decrement to 0, and the database records -1 stock.
  - **Solution:** In `OrderService.placeOrder`, we strictly acquire a **PESSIMISTIC_WRITE** lock on the `Product` row before checking stock.
  - **Result:** Transaction B is forced to wait until Transaction A commits. Transaction B then reads the updated stock (0) and fails gracefully with an "Out of Stock" exception.

- **Duplicate Payments:**
  - **Problem:** A payment gateway sends two webhooks (e.g., `payment_captured`) for the same order simultaneously due to network retries.
  - **Solution:** `PaymentService` locks the `Order` row using `findByIdForUpdate` before processing. It performs an **Idempotency Check** immediately after acquiring the lock: `if (order.status == COMPLETED) return;`.

- **Concurrent Reviews & Likes:**
  - **Problem:** Two users "like" a review at the exact same moment, causing a lost update on the counter.
  - **Solution:** `ReviewService` locks the `Review` entity row before incrementing the like count, ensuring accurate tallying.

### 2. Deadlock Prevention

When locking multiple resources in a single transaction (e.g., buying 5 different items), the order of locking is critical to prevent Database Deadlocks.

- **Strategy:** In `OrderService`, cart items are **programmatically sorted by Product ID** before processing.
- **Why?** This ensures that every transaction acquires locks in the exact same order (e.g., Lock Product 10 -> Lock Product 20 -> Lock Product 30), eliminating the circular dependency (A waits for B, B waits for A) that causes deadlocks.

### 3. Concurrency & Async Processing

DiddyCart decouples "blocking" business logic from "non-blocking" I/O tasks to maximize throughput.

- **Custom Thread Pool:** A dedicated `ThreadPoolTaskExecutor` (`kafkaWorkerPool`) handles background tasks.
  - **Config:** Core Pool: 5, Max Pool: 10, Queue: 100.
- **Kafka Offloading:**
  - The `EventConsumer` listens to Kafka topics but does _not_ process heavy tasks (like sending emails) on the listener thread.
  - Instead, it immediately offloads the work to the `kafkaWorkerPool` using `CompletableFuture.runAsync()`. This keeps the Kafka consumer lag near zero, even during traffic spikes.

### 4. Caching Strategy (Redis)

We use a **Look-Aside** caching pattern to reduce database load for high-traffic read operations.

- **Cart:** `CartService` uses `@Cacheable` on `getCart` and `@CachePut` on `addToCart`. This ensures the active shopping session is served entirely from Redis, hitting Postgres only for persistence.
- **Products:** Product details are cached (`@Cacheable "products"`) and evicted (`@CacheEvict`) only when an Admin updates the catalog, ensuring users always see up-to-date pricing without DB hits.

### 5. Transaction Management

All write operations are wrapped in Spring's `@Transactional` annotation. This adheres to the **ACID** properties:

- **Atomicity:** If an inventory deduction fails, the entire Order creation is rolled back.
- **Isolation:** The Pessimistic Locks ensure strictly serialized access to hot rows (Products/Orders).

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
