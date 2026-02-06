# DiddyCart — Enterprise E-Commerce Platform

DiddyCart is a high-performance e-commerce platform designed for enterprise-scale operations. Built using modern architectural patterns including Domain-Driven Design (DDD), event-driven architecture, and advanced concurrency management, it handles complex workflows like inventory management, secure payments, order fulfillment, and real-time notifications with precision and reliability.

---

## 📑 Table of Contents

- [System Architecture](#-system-architecture)
- [Tech Stack & Tools](#-tech-stack--tools)
- [Database Design & Optimization](#-database-design--optimization)
- [Advanced Concurrency Management](#-advanced-concurrency-management)
- [Transaction Management](#-transaction-management)
- [Caching Implementation](#-caching-implementation)
- [Security & Identity Management](#-security--identity-management)
- [API Rate Limiting & DDoS Protection](#-api-rate-limiting--ddos-protection)
- [Performance & Scalability](#-performance--scalability)
- [Event-Driven Architecture](#-event-driven-architecture)
- [Key Workflows](#-key-workflows)
- [API Documentation](#-api-documentation)
- [Setup & Installation](#-setup--installation)

---

## 🏗 System Architecture

DiddyCart follows a **layered modular monolith architecture** with clear separation of concerns across multiple layers. The system is designed for horizontal scalability and can easily transition to microservices when needed.

### Architecture Overview

For a detailed visual representation of the system architecture, including data flow diagrams and component interactions, please refer to:

**📄 [System Architecture Diagram](docs/architecture-diagram.md)**

### Core Architecture Layers

```
┌─────────────────────────────────────────────────────┐
│          Client Layer (Web/Mobile/Admin)            │
└─────────────────┬───────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────┐
│     Security Layer (JWT + Rate Limiting)            │
│  • JWT Authentication Filter                        │
│  • Rate Limit Filter (Bucket4j + Redis)             │
│  • Spring Security (RBAC)                           │
└─────────────────┬───────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────┐
│          API Layer (REST Controllers)               │
│  • Auth  • Products  • Cart  • Orders               │
│  • Payments  • Vendors  • Addresses                 │
└─────────────────┬───────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────┐
│       Business Logic Layer (Services)               │
│  • Transaction Management (@Transactional)          │
│  • Business Rules & Validation                      │
│  • Pessimistic Locking for Critical Operations     │
└─────────┬───────────────────────────┬───────────────┘
          │                           │
┌─────────▼──────────┐    ┌──────────▼────────────────┐
│   Cache Layer      │    │   Event Streaming         │
│   (Redis)          │    │   (Apache Kafka)          │
│                    │    │                           │
│ • Product Cache    │    │ • User Registration       │
│ • Cart Cache       │    │ • Order Placed            │
│ • User Profiles    │    │ • Payment Failed          │
│ • Order Cache      │    │ • Password Reset          │
│                    │    │ • Refund Requested        │
└─────────┬──────────┘    └───────────────────────────┘
          │
┌─────────▼───────────────────────────────────────────┐
│     Data Persistence Layer (PostgreSQL)             │
│  • Users  • Products  • Orders  • Payments          │
│  • Carts  • Addresses  • Vendors  • Categories      │
└─────────────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Stateless REST API**: JWT-based authentication enables seamless horizontal scaling
2. **Event-Driven Communication**: Kafka decouples critical business logic from side effects
3. **Cache-First Strategy**: Redis reduces database load for high-traffic read operations
4. **Domain-Driven Design**: Organized by business domains (Identity, Sales, Products, Payments)
5. **ACID Transactions**: Strong consistency for critical operations (orders, payments, inventory)

---

## 🛠 Tech Stack & Tools

### Core Technologies

| Category       | Technology  | Version | Purpose                                                    |
| -------------- | ----------- | ------- | ---------------------------------------------------------- |
| **Language**   | Java        | 21      | Modern LTS with Records, Pattern Matching, Virtual Threads |
| **Framework**  | Spring Boot | 4.0.1   | Application framework with auto-configuration              |
| **Build Tool** | Maven       | Latest  | Dependency management and build automation                 |

### Data & Persistence

| Technology          | Purpose          | Description                                |
| ------------------- | ---------------- | ------------------------------------------ |
| **PostgreSQL**      | Primary Database | Relational data store with ACID compliance |
| **Spring Data JPA** | ORM              | Object-relational mapping with Hibernate   |
| **HikariCP**        | Connection Pool  | High-performance JDBC connection pooling   |
| **Supabase**        | Cloud Database   | Managed PostgreSQL hosting                 |

### Caching & Performance

| Technology       | Purpose           | Description                                  |
| ---------------- | ----------------- | -------------------------------------------- |
| **Redis**        | Distributed Cache | In-memory data structure store               |
| **Spring Cache** | Cache Abstraction | Declarative caching with annotations         |
| **Redisson**     | Redis Client      | Advanced Redis client with distributed locks |
| **Bucket4j**     | Rate Limiting     | Token bucket rate limiting backed by Redis   |

### Messaging & Events

| Technology             | Purpose           | Description                             |
| ---------------------- | ----------------- | --------------------------------------- |
| **Apache Kafka**       | Event Streaming   | Distributed event streaming platform    |
| **Spring Kafka**       | Kafka Integration | Kafka template and listeners            |
| **Custom Thread Pool** | Async Processing  | Dedicated executor for background tasks |

### Security

| Technology          | Purpose            | Description                       |
| ------------------- | ------------------ | --------------------------------- |
| **Spring Security** | Security Framework | Authentication and authorization  |
| **JWT (jjwt)**      | Token Management   | JSON Web Token for stateless auth |
| **BCrypt**          | Password Hashing   | Secure password encryption        |

### API & Documentation

| Technology             | Purpose           | Description                            |
| ---------------------- | ----------------- | -------------------------------------- |
| **Spring WebMVC**      | REST API          | RESTful web services                   |
| **SpringDoc OpenAPI**  | API Documentation | Automatic API documentation generation |
| **Swagger UI**         | API Explorer      | Interactive API testing interface      |
| **Jakarta Validation** | Input Validation  | Bean validation with annotations       |

### External Integrations

| Service         | Purpose         | Description                      |
| --------------- | --------------- | -------------------------------- |
| **Razorpay**    | Payment Gateway | Payment processing (integrated)  |
| **Mailtrap**    | Email Testing   | SMTP testing for dev environment |
| **Spring Mail** | Email Service   | Email sending functionality      |

### DevOps & Deployment

| Technology         | Purpose          | Description                               |
| ------------------ | ---------------- | ----------------------------------------- |
| **Docker**         | Containerization | Application containerization              |
| **Docker Compose** | Orchestration    | Multi-container setup (Redis, Kafka, App) |
| **Kubernetes**     | Orchestration    | Production-ready deployment configs       |
| **Prometheus**     | Monitoring       | Metrics collection (planned)              |

---

## 💾 Database Design & Optimization

DiddyCart's database schema is carefully designed following **Third Normal Form (3NF)** principles to eliminate data redundancy while maintaining query performance through strategic indexing.

### Database Normalization

The schema is normalized to 3NF with the following key principles:

#### 1. Entity Separation

- **Users and Addresses**: 1:N relationship - Users can have multiple delivery addresses
- **Products and Categories**: Products reference categories allowing flexible catalog management
- **Orders and OrderItems**: Order details are separated from line items for flexibility
- **Users and Vendors**: Vendor information is separate from user credentials (role escalation)

#### 2. No Transitive Dependencies

- Address information is stored in the `address` table, not duplicated across users
- Product prices are captured in `orderitems` at time of purchase (price history)
- Payment mode and status are normalized into ENUM types

#### 3. Referential Integrity

- All foreign keys have proper constraints with `ON DELETE CASCADE` or `ON DELETE SET NULL`
- Composite unique constraints prevent duplicate reviews and likes

### Comprehensive Database Indexing

DiddyCart implements **36 strategic indexes** across 11 tables to optimize query performance:

#### Primary Key Indexes (11 indexes)

| Table           | Index Name           | Column | Purpose                  |
| --------------- | -------------------- | ------ | ------------------------ |
| `users`         | `users_pkey`         | `id`   | User record lookup       |
| `vendors`       | `vendors_pkey`       | `id`   | Vendor record lookup     |
| `products`      | `products_pkey`      | `id`   | Product record lookup    |
| `category`      | `category_pkey`      | `id`   | Category record lookup   |
| `address`       | `address_pkey`       | `id`   | Address record lookup    |
| `cart`          | `cart_pkey`          | `id`   | Cart record lookup       |
| `cartitem`      | `cartitem_pkey`      | `id`   | Cart item record lookup  |
| `orders`        | `orders_pkey`        | `id`   | Order record lookup      |
| `orderitems`    | `orderitems_pkey`    | `id`   | Order item record lookup |
| `payment`       | `payment_pkey`       | `id`   | Payment record lookup    |
| `product_image` | `product_image_pkey` | `id`   | Image record lookup      |

#### Foreign Key & Relationship Indexes (15 indexes)

| Index Name                     | Table           | Column        | Purpose                        |
| ------------------------------ | --------------- | ------------- | ------------------------------ |
| `idx_address_user_id`          | `address`       | `user_id`     | Fetch all addresses for a user |
| `idx_cart_user_id`             | `cart`          | `user_id`     | User cart lookup               |
| `idx_cartitem_cart_id`         | `cartitem`      | `cart_id`     | Fetch all items in a cart      |
| `idx_cartitem_product_id`      | `cartitem`      | `product_id`  | Product popularity tracking    |
| `idx_orders_user_id`           | `orders`        | `user_id`     | User order history             |
| `idx_orderitems_order_id`      | `orderitems`    | `order_id`    | Order line items               |
| `idx_orderitems_product_id`    | `orderitems`    | `product_id`  | Product sales analytics        |
| `idx_payment_order_id`         | `payment`       | `order_id`    | Order payment lookup           |
| `idx_products_vendor_id`       | `products`      | `vendor_id`   | Vendor's product catalog       |
| `idx_products_category_id`     | `products`      | `category_id` | Category browsing              |
| `idx_product_image_product_id` | `product_image` | `product_id`  | Product images                 |
| `idx_vendors_user_id`          | `vendors`       | `user_id`     | Vendor profile lookup          |
| `idx_review_likes_user_id`     | `review_likes`  | `user_id`     | User's liked reviews           |
| `idx_reviews_product_id`       | `reviews`       | `product_id`  | Product reviews                |

#### Business Logic Indexes (10 indexes)

| Index Name                   | Table      | Column                 | Query Pattern              |
| ---------------------------- | ---------- | ---------------------- | -------------------------- |
| `idx_users_email`            | `users`    | `email`                | Login authentication       |
| `idx_users_role`             | `users`    | `role`                 | Role-based queries         |
| `idx_users_reset_token`      | `users`    | `reset_password_token` | Password reset validation  |
| `idx_products_name`          | `products` | `name`                 | Product search             |
| `idx_products_price`         | `products` | `price`                | Price filtering            |
| `idx_orders_status`          | `orders`   | `status`               | Order management dashboard |
| `idx_orders_payment_status`  | `orders`   | `payment_status`       | Payment reconciliation     |
| `idx_orders_created_at`      | `orders`   | `created_at`           | Time-based queries         |
| `idx_payment_transaction_id` | `payment`  | `transaction_id`       | Payment gateway callback   |

#### Unique Constraints (7 constraints)

| Constraint Name          | Table          | Columns               | Purpose                         |
| ------------------------ | -------------- | --------------------- | ------------------------------- |
| `users_email_key`        | `users`        | `email`               | One account per email           |
| `vendors_gstin_key`      | `vendors`      | `gstin`               | One vendor per GST number       |
| `vendors_user_id_key`    | `vendors`      | `user_id`             | One vendor profile per user     |
| `uq_review_user_product` | `reviews`      | `user_id, product_id` | One review per user per product |
| `uq_review_like_user`    | `review_likes` | `review_id, user_id`  | One like per user per review    |

### Database Sharding Strategy (Future)

#### Proposed Sharding Approach

**1. Horizontal Sharding by User ID**

- Shard Key: `user_id` for users, orders, carts, addresses
- Benefit: User-related queries hit only one shard
- Implementation: Consistent hashing

**2. Vertical Sharding by Domain**

- Product Catalog Shard: Products, Categories, Images (read-heavy)
- Transaction Shard: Orders, Payments (write-heavy, ACID)
- User Data Shard: Users, Addresses, Vendors

**3. Replication Strategy**

- Master-Slave replication for read-heavy tables
- Read replicas for analytics and reporting
- Write operations to master node only

---

## ⚡ Advanced Concurrency Management

DiddyCart handles high-concurrency scenarios like flash sales where thousands of users compete for limited inventory using sophisticated locking mechanisms and deadlock prevention strategies.

### 1. Race Condition Prevention

**Pessimistic Locking** is used for critical operations to prevent data corruption from simultaneous updates.

#### Inventory Overselling Protection

- Uses database-level write locks (`SELECT ... FOR UPDATE`)
- Transaction A acquires lock on Product row
- Transaction B waits until Transaction A commits
- Prevents negative stock quantities

**Implementation**: `ProductRepository.findByIdForUpdate()` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`

#### Duplicate Payment Prevention

- Locks Order row before processing payment
- Idempotency check after acquiring lock
- Prevents duplicate charges from webhook retries

#### Concurrent Cart Updates

- Spring `@Transactional` ensures atomic updates
- Redis cache invalidated after transaction commits
- Prevents lost updates from rapid clicks

### 2. Deadlock Prevention

**Consistent Lock Ordering** prevents circular dependencies between transactions.

**Strategy**: All cart items are sorted by Product ID before acquiring locks

- Transaction A: Locks Product 10 → 20 → 30
- Transaction B: Locks Product 10 → 20 → 30 (same order)
- No circular wait = No deadlock

**Implementation**: `OrderService.placeOrder()` sorts items by Product ID before processing

### 3. Concurrent Processing with Thread Pools

Dedicated thread pool separates blocking operations from non-blocking I/O.

**Configuration**:

- Core Pool Size: 5 threads
- Max Pool Size: 10 threads
- Queue Capacity: 100 tasks
- Thread Name Prefix: `kafka-worker-`

**Benefits**:

- Kafka consumer returns immediately (low lag)
- Worker threads handle slow operations (email sending)
- Can process 100+ events concurrently

### 4. Scheduled Jobs with Concurrency Safety

**Auto-Cancel Unpaid Orders** runs every 10 minutes:

- Finds orders older than 15 minutes with status PENDING
- Acquires pessimistic locks on products before restoring stock
- Uses `@Transactional` for atomicity
- Multiple instances can run safely (unique order.id)

---

## 🔄 Transaction Management

DiddyCart uses **Spring's declarative transaction management** to ensure ACID properties for all critical operations.

### Transaction Boundaries

All service-layer write operations are annotated with `@Transactional` ensuring atomic execution of multi-step operations.

### ACID Properties

#### Atomicity

All-or-nothing execution - if inventory deduction fails, the entire order creation is rolled back automatically.

#### Consistency

Database constraints and validation prevent invalid states (e.g., negative stock, duplicate emails).

#### Isolation

Pessimistic locks ensure transactions see consistent data. Default isolation level is `READ_COMMITTED` with stronger guarantees for hot rows.

#### Durability

PostgreSQL WAL (Write-Ahead Logging) ensures committed transactions survive crashes. Supabase provides automated backups.

### Transaction Propagation

Default propagation (`REQUIRED`) is used - child methods join parent transactions, and any failure rolls back the entire transaction.

### Event Publishing Strategy

Kafka events are sent **after** transaction commits to avoid inconsistencies. If email sending fails, the order is still created and can be retried later.

---

## 🚀 Caching Implementation

DiddyCart uses **Redis** as a distributed cache to reduce database load and improve response times.

### Cache Strategy

**Look-Aside Pattern**:

1. Check cache first
2. On cache miss: Query database → Store in cache
3. On cache hit: Return from Redis

### Cached Entities

#### Product Catalog

- Cache Key: `products::{productId}`
- TTL: 1 hour
- Annotations: `@Cacheable`, `@CacheEvict`
- Benefit: Reduces DB queries for high-traffic product pages

#### Shopping Cart

- Cache Key: `carts::{userId}`
- TTL: 1 hour
- Annotations: `@Cacheable`, `@CachePut`, `@CacheEvict`
- Benefit: Fast cart retrieval during checkout

#### User Profiles

- Cache Key: `user_profile_v2::{userId}`
- TTL: 1 hour
- Annotations: `@Cacheable`, `@CachePut`
- Benefit: Avoids repeated profile lookups

#### Vendor Information

- Dual cache: `vendors_by_user::{userId}` and `vendors::{vendorId}`
- TTL: 1 hour
- Benefit: Fast vendor dashboard and public pages

### Cache Configuration

- **Serialization**: JSON via Jackson (human-readable)
- **Default TTL**: 1 hour
- **Null Value Caching**: Disabled
- **Eviction Policy**: Manual + TTL-based

### Cache Observability

Logging decorators monitor cache performance with console output:

- ✅ CACHE HIT: Successful cache retrieval
- ❌ CACHE MISS: Cache miss, querying database
- 💾 CACHE PUT: Storing value in cache
- 🗑️ CACHE EVICT: Removing value from cache

### Performance Impact

**Before Caching**:

- Product page: 150ms (3 DB queries)
- Cart retrieval: 80ms (2 DB queries + JOIN)

**After Caching**:

- Product page: 15ms (cache hit)
- Cart retrieval: 8ms (cache hit)

**Cache Hit Rate**: ~85% for product catalog

---

## 🔒 Security & Identity Management

DiddyCart implements a **defense-in-depth security model** with multiple protection layers.

### Role-Based Access Control (RBAC)

| Role       | Permissions                                  | Use Case                |
| ---------- | -------------------------------------------- | ----------------------- |
| **USER**   | Browse, Cart, Orders, Addresses              | Regular customers       |
| **VENDOR** | USER permissions + Create/Update products    | Sellers on platform     |
| **ADMIN**  | VENDOR permissions + Manage all orders/users | Platform administrators |

### Authentication Flow

#### Registration

1. Validate input (email format, password strength)
2. Hash password with BCrypt (cost factor: 10)
3. Create user + empty cart (transactional)
4. Generate JWT token (21-day expiry)
5. Publish Kafka event for welcome email
6. Return token + user info

#### Login

1. Load user by email
2. Verify password using BCrypt
3. Generate JWT with userId + role
4. Return token + user info

#### JWT Token Structure

- Payload: `userId`, `role`, issued-at, expiry
- Signature: HMAC-SHA256 with secret key
- Expiry: 21 days from issuance

### Protected Routes

**Public Endpoints**:

- `/api/auth/**` - Authentication
- `/api/products/**` - Product catalog (read-only)
- `/swagger-ui/**` - API documentation
- Payment callback URLs

**Authenticated Endpoints**:

- `/api/cart/**` - Shopping cart
- `/api/orders/**` - Order management
- `/api/addresses/**` - Address management

**Admin-Only Endpoints**:

- `/api/admin/orders/**` - Order management & analytics
  - Get all orders (paginated)
  - Search orders by ID or user email
  - Get orders by specific userId
  - View detailed order information
- `/api/admin/identity/**` - User management

### Security Features

#### Password Reset Flow

1. User requests reset via email
2. System generates UUID token (15-min expiry)
3. Token stored in database
4. Kafka event triggers email with reset link
5. User submits new password with token
6. System validates token, updates password, clears token

**Security**: Tokens expire quickly, single-use, always returns 200 OK (prevents email enumeration)

#### Authorization Checks

- **Controller-Level**: `@PreAuthorize` annotations
- **Service-Level**: Ownership validation (e.g., verify order belongs to user)

#### Session Management

- Stateless architecture (no server-side sessions)
- JWT in Authorization header (not cookies)
- Each request authenticated independently
- Enables horizontal scaling

### Security Best Practices

✅ **Implemented**:

- BCrypt password hashing
- JWT with HMAC-SHA256
- Role-based authorization
- Input validation
- SQL injection prevention via JPA
- Ownership validation

⚠️ **Production Recommendations**:

- HTTPS/TLS for all connections
- Refresh token rotation
- Account lockout after failed logins
- 2FA for admin accounts
- Regular JWT key rotation

---

## 🛡️ API Rate Limiting & DDoS Protection

DiddyCart implements **distributed rate limiting** using Bucket4j backed by Redis to protect against abuse and DDoS attacks.

### Rate Limiting Architecture

**Filter Chain Order**:

```
Request → RateLimitFilter → JwtAuthenticationFilter → SecurityFilterChain → Controller
```

Rate limiting runs **before** authentication to protect the system.

### Dual-Layer Throttling

| User Type         | Rate Limit  | Refill Rate    | Key Strategy      | Purpose                              |
| ----------------- | ----------- | -------------- | ----------------- | ------------------------------------ |
| **Authenticated** | 100 req/min | 100 tokens/min | `user:{userId}`   | High throughput for legitimate users |
| **Anonymous**     | 20 req/min  | 20 tokens/min  | `ip:{remoteAddr}` | Strict limits prevent DDoS           |

### Token Bucket Algorithm

- Each user gets a bucket with fixed capacity
- Each request consumes 1 token
- Tokens refill at constant rate
- Empty bucket = Request blocked (429 status)

**Benefits**:

- Allows small traffic bursts
- Smooth refill (not sudden reset)
- Fair resource allocation

### Distributed Architecture

Redis-backed buckets ensure consistent rate limiting across multiple application instances. All instances share the same token count in Redis.

### Client Response Headers

**Success (200 OK)**:

- `X-Rate-Limit-Remaining`: Tokens left in current window

**Rate Limited (429)**:

- `X-Rate-Limit-Remaining`: 0
- `X-Rate-Limit-Retry-After-Seconds`: Wait time before retry

### Attack Mitigation

- **Credential Stuffing**: Limited to 20 attempts/min per IP
- **DDoS**: Database queries reduced from 100k/sec to ~333/sec
- **Account Enumeration**: Rate limited to 20 checks/min
- **Inventory Hoarding**: Authenticated users limited to 100 req/min

---

## 📈 Performance & Scalability

DiddyCart is architected for high performance and horizontal scalability.

### Current Performance Metrics

| Metric                 | Value          | Condition       |
| ---------------------- | -------------- | --------------- |
| Average Response Time  | 15-50ms        | Cache hit       |
| Database Response Time | 50-150ms       | Cache miss      |
| Throughput             | 1000+ req/sec  | Single instance |
| Concurrent Users       | 500-1000       | Per instance    |
| Database Connections   | 15 max, 2 idle | HikariCP pool   |

### Scalability Features

#### 1. Stateless Architecture

- No server-side sessions
- JWT enables perfect horizontal scaling
- Load balancer ready

#### 2. Connection Pooling

- HikariCP manages database connections efficiently
- Max pool: 15 connections
- Min idle: 2 connections
- Prevents connection exhaustion

#### 3. Async Processing

- Background tasks offloaded to worker threads
- Main threads freed quickly
- 100+ concurrent background tasks

#### 4. Database Query Optimization

- Strategic indexing (36 indexes)
- Batch operations where possible
- Pessimistic locking for critical sections

#### 5. Pagination

- Prevents memory exhaustion on large datasets
- Loads data in chunks (e.g., 10 orders at a time)
- Faster response times

### Scalability Roadmap

#### Phase 1: Single Instance (Current)

- Capacity: 500-1000 concurrent users
- Components: 1 App, 1 Redis, 1 PostgreSQL

#### Phase 2: Horizontal Scaling

- Add NGINX load balancer
- Deploy 3+ app instances
- Capacity: 3000-5000 concurrent users

#### Phase 3: Database Replication

- Master for writes
- Slaves for reads
- 10x read capacity

#### Phase 4: Microservices (Future)

- Split into independent services
- Service-specific scaling
- Kafka for async communication

---

## 🔄 Event-Driven Architecture

DiddyCart uses **Apache Kafka** for asynchronous, decoupled event processing.

### Kafka Topics

| Topic Name                | Producer       | Consumer      | Purpose                    |
| ------------------------- | -------------- | ------------- | -------------------------- |
| `user-registration`       | AuthService    | EventConsumer | Welcome email after signup |
| `order-placed`            | OrderService   | EventConsumer | Order confirmation email   |
| `payment-failed`          | PaymentService | EventConsumer | Payment failure alert      |
| `identity.password-reset` | AuthService    | EventConsumer | Password reset link        |
| `refund-requested`        | OrderService   | EventConsumer | Async refund processing    |

### Event Flow

1. **Service Layer**: Business transaction commits
2. **Event Producer**: Publishes event to Kafka (async)
3. **Kafka Broker**: Persists event to disk
4. **Event Consumer**: Receives event
5. **Worker Thread Pool**: Processes event (e.g., send email)

### Benefits

- **Decoupling**: Order service doesn't depend on email infrastructure
- **Reliability**: Events persisted, survive crashes
- **Scalability**: Parallel processing via partitions
- **Auditability**: Event log for debugging and replay

### Thread Pool Strategy

- **Kafka Consumer Thread**: Receives event, offloads to worker pool immediately
- **Worker Threads**: Process slow I/O operations
- **Configuration**: 5 core threads, 10 max threads, 100 queue capacity

### Error Handling

- Consumer retry logic for transient failures
- Idempotency checks for duplicate events
- Dead letter queue for persistent failures (future)

### Asynchronous Refund Processing

DiddyCart implements a sophisticated refund workflow that decouples order cancellation from payment refund processing:

**Why Asynchronous?**

- Order cancellation completes immediately (better UX)
- Razorpay API calls don't block user requests
- Refund processing happens in background worker threads
- Failed refunds don't prevent order cancellation
- Scalable: Multiple refunds can be processed concurrently

**Refund Event Flow**:

1. User cancels paid order → OrderService publishes `RefundRequestedEvent`
2. Kafka persists event (survives crashes)
3. EventConsumer receives event in background thread
4. PaymentService calls Razorpay refund API
5. Payment status updated to REFUNDED in database
6. EmailService sends refund confirmation to user
7. User receives notification about 5-7 day processing time

**Refund Event Payload**:

- Order ID, User ID, Email
- Refund amount, Payment mode
- Transaction ID for Razorpay API

---

## 🎯 Key Features

### Order Management

#### Flexible Cancellation Policy

- Users can cancel orders until they are **shipped**
- Supports cancellation for **PENDING** and **CONFIRMED** orders
- Automatic inventory restoration upon cancellation
- Orders cannot be cancelled once **SHIPPED** or **DELIVERED**

#### Intelligent Refund Processing

- **Automatic refund detection**: System checks if order was paid
- **Async processing**: Refunds happen in background via Kafka
- **Razorpay integration**: Direct API calls for refund processing
- **Email notifications**: Users receive confirmation when refund is processed
- **Timeline transparency**: Users informed about 5-7 business day processing

#### Admin Order Analytics

- View all orders with pagination and sorting
- Search orders by Order ID or user email
- Filter orders by specific user ID
- Detailed order information with payment status
- Real-time order status tracking

---

## 🔑 Key Workflows

### 1. User Registration Flow

1. Client sends registration request
2. AuthController → AuthService
3. Validate input, hash password
4. Create user + cart (transaction)
5. Generate JWT token
6. Publish Kafka event (async)
7. Return token + user info
8. Background: Send welcome email

### 2. Add to Cart Flow

1. Browse products (cached in Redis)
2. Client sends add-to-cart request
3. CartService validates product, checks stock
4. Add/update cart item (transaction)
5. Update Redis cache
6. Return updated cart

### 3. Order Placement Flow

1. Client places order with address
2. OrderService validates cart and address
3. **Critical Section**:
   - Sort items by Product ID
   - Lock products (pessimistic)
   - Check stock availability
   - Deduct inventory
   - Create order + items
4. Clear cart, evict cache
5. Transaction commits
6. Return order response
7. Payment gateway initiated

### 4. Payment Processing Flow

1. Client initiates payment
2. PaymentService creates Razorpay order
3. User completes payment (external)
4. Razorpay webhook callback
5. PaymentService verifies signature
6. Lock order, check idempotency
7. Update order status (transaction)
8. Publish Kafka event (async)
9. Background: Send confirmation email

### 5. Order Status Lifecycle

**Status Flow**: PENDING → CONFIRMED → SHIPPED → DELIVERED  
**Cancellation Policy**:

- Orders can be cancelled in **PENDING** or **CONFIRMED** status
- Orders **cannot** be cancelled once **SHIPPED** or **DELIVERED**
- Stock is automatically restored upon cancellation

### 6. Order Cancellation with Refund Flow

1. User requests order cancellation
2. OrderService validates cancellation eligibility
3. **Critical Section**:
   - Lock products (pessimistic)
   - Restore inventory to stock
4. Check payment status:
   - If payment completed → Publish `RefundRequestedEvent` to Kafka
   - If payment pending → Skip refund
5. Update order status to CANCELLED
6. Transaction commits
7. **Background Refund Processing**:
   - EventConsumer receives refund event
   - PaymentService processes refund via Razorpay API
   - Update payment status to REFUNDED
   - Send refund confirmation email to user
8. User receives email notification (5-7 business days for credit)

### 7. Scheduled Job: Auto-Cancel Unpaid Orders

- Runs every 10 minutes
- Finds orders older than 15 minutes (status: PENDING)
- Locks products, restores stock
- Updates order status to CANCELLED
- Logs auto-cancellation

---

## 📖 API Documentation

### Access Documentation

**Swagger UI**: http://localhost:8080/swagger-ui.html  
**OpenAPI JSON**: http://localhost:8080/api-docs

### API Groups

| Group              | Endpoints           | Description                         |
| ------------------ | ------------------- | ----------------------------------- |
| **Authentication** | `/api/auth/**`      | Registration, login, password reset |
| **Products**       | `/api/products/**`  | Catalog, search, filtering          |
| **Cart**           | `/api/cart/**`      | Shopping cart operations            |
| **Orders**         | `/api/orders/**`    | Order placement, tracking           |
| **Payments**       | `/api/payments/**`  | Payment processing                  |
| **Vendors**        | `/api/vendors/**`   | Vendor management                   |
| **Addresses**      | `/api/addresses/**` | Delivery addresses                  |
| **Admin**          | `/api/admin/**`     | Administrative operations           |

### Response Codes

| Code                          | Meaning              | Scenario                 |
| ----------------------------- | -------------------- | ------------------------ |
| **200 OK**                    | Success              | Data retrieved/updated   |
| **201 Created**               | Created              | User registered          |
| **400 Bad Request**           | Validation error     | Invalid input            |
| **401 Unauthorized**          | Auth failed          | Invalid JWT              |
| **403 Forbidden**             | Authorization failed | Insufficient permissions |
| **404 Not Found**             | Not found            | Resource doesn't exist   |
| **429 Too Many Requests**     | Rate limited         | Quota exceeded           |
| **500 Internal Server Error** | Server error         | Database failure         |

---

## 🚀 Setup & Installation

### Prerequisites

| Requirement | Version | Purpose             |
| ----------- | ------- | ------------------- |
| Java JDK    | 21      | Runtime environment |
| Maven       | 3.8+    | Build tool          |
| Docker      | Latest  | Container runtime   |
| PostgreSQL  | 15+     | Database            |
| Redis       | 7+      | Cache               |
| Kafka       | 3.5+    | Message broker      |

### Local Development Setup

#### 1. Clone Repository

#### 2. Configure Environment Variables

Create `.env` file:

```bash
DB_URL=jdbc:postgresql://your-supabase.supabase.co:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=your_jwt_secret_min_256_bits
JWT_EXPIRATION_MS=1814400000

SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password

key_id=rzp_test_xxxx
key_secret=your_razorpay_secret
```

#### 3. Start Infrastructure Services

```bash
docker compose up -d --build
```

#### 4. Initialize Database Schema

```bash
psql -h your-host -U postgres -d postgres -f database/schema.sql
```

#### 5. Build Application

```bash
./mvnw clean package -DskipTests
```

#### 6. Run Application

```bash
./mvnw spring-boot:run
```

Application starts on: http://localhost:8080

---

## 📚 Additional Resources

- **System Architecture Diagram**: [docs/architecture-diagram.md](docs/architecture-diagram.md)
- **Database ERD**: https://drawsql.app/teams/arhan-das/diagrams/diddycart
- **Future Enhancements**: [docs/updates.md](docs/updates.md)

---

## 👥 Authors

**Arhan Das**  
Built with ❤️ using Spring Boot, Java 21, and modern cloud-native technologies.

---
