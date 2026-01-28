## 1. Authentication & Security Module

### Feature 1.1: Refresh Token Flow

**Goal:** Securely maintain user sessions without eternal access tokens.

- **New Entities:** `RefreshToken` (id, token, user_id, expiryDate).
- **New Routes:** `POST /api/auth/refresh-token`

**Implementation Flow:**

1. **Login Update:** Modify `AuthService.login` to generate both an `accessToken` (short-lived, e.g., 15 mins) and a `refreshToken` (long-lived, e.g., 7 days).
2. **Persistence:** Save the `refreshToken` in the database linked to the `User`.
3. **Refresh Request:**

- Client sends `POST /api/auth/refresh-token` with the refresh token.
- **Validation:** Check if token exists in DB and is not expired.
- **Rotation:** Verify user is active (not banned).
- **Issue:** Generate _new_ `accessToken` and _new_ `refreshToken`.
- **Cleanup:** Delete the old refresh token (Prevent reuse/replay attacks).

**Technical Strategy:**

- **Async/Kafka:** No.
- **Caching:** **Avoid.** Refresh tokens must be checked against the DB for revocation (e.g., if a user is banned, their refresh token in cache would keep them logged in).
- **Scheduling:** Add a `@Scheduled` task to delete expired tokens from the DB daily.

### Feature 1.2: Forgot & Reset Password

**Goal:** Account recovery.

- **New Routes:**
- `POST /api/auth/forgot-password` (Input: email)
- `POST /api/auth/reset-password` (Input: token, newPassword)

**Implementation Flow:**

1. **Request:** User submits email.
2. **Token Gen:** `AuthService` generates a random UUID or JWT with 15-min expiry.
3. **Save:** Store token in `User` entity or a `PasswordResetToken` table.
4. **Email:** Call `NotificationService.sendResetEmail(email, token)`.
5. **Reset:** User submits token + new password. Service verifies token validity -> Encodes new password -> Updates User -> Invalidates token.

**Technical Strategy:**

- **Async:** **YES.** Sending the email must be `@Async` to prevent the UI from hanging.
- **Kafka:** Optional. You can publish a `PASSWORD_RESET_REQUEST` event if you want a separate notification microservice to handle emails later.

---

## 2. Order Lifecycle & Returns

### Feature 2.1: Return Request & Refund

**Goal:** Handle post-delivery lifecycle.

- **New Statuses:** `RETURN_REQUESTED`, `RETURN_APPROVED`, `REFUNDED`.
- **New Routes:**
- `POST /api/orders/{id}/return` (User)
- `PUT /api/admin/orders/{id}/return-approve` (Admin)

**Implementation Flow:**

1. **Request:** User requests return. Service checks if `Order.status == DELIVERED`. Updates status to `RETURN_REQUESTED`.
2. **Approval:** Admin calls approve endpoint.
3. **Transaction (Critical):**

- Update Order status to `REFUNDED`.
- Update Payment status to `REFUNDED`.
- **Stock Restoration:** Iterate `OrderItems` and increment `Product.stockQuantity`.
- **Refund Gateway:** Call `RazorpayClient` to process actual refund (if real mode).

4. **Notification:** Notify user of refund.

**Technical Strategy:**

- **Transactions:** **Strictly Synchronous.** Use `@Transactional`. Do not make stock restoration async; it must be atomic with the order status update.
- **Async:** Use for sending the "Refund Processed" email.
- **Caching:** **Evict** the specific order cache (`key = "#userId + '_' + #orderId"`) and the product stock caches (`key = "#productId"`).

---

## 3. Product Reviews & Ratings

### Feature 3.1: Product Reviews

**Goal:** Social proof.

- **New Entity:** `Review` (id, user, product, rating (1-5), comment, createdAt).
- **New Routes:** `POST /api/products/{id}/reviews`, `GET /api/products/{id}/reviews`.

**Implementation Flow:**

1. **Submission:** Authenticated user submits rating/comment.
2. **Validation:** Check if User has actually purchased this product (Verified Purchase logic).
3. **Persistence:** Save `Review`.
4. **Aggregation:** Recalculate the Product's "Average Rating". _Option: Store `averageRating` and `reviewCount` directly on the `Product` table to avoid summing 1000s of rows on every read._

**Technical Strategy:**

- **Caching:** **YES.**
- Cache the `GET` reviews page: `@Cacheable(value = "product_reviews", key = "#productId + '_' + #page")`.
- **Evict:** When a new review is posted, `@CacheEvict` the reviews cache _and_ the `product` cache (since average rating changed).

- **Async:** No, unless you want to use AI to auto-moderate reviews for bad language.

---

## 4. Admin Dashboard & Analytics

### Feature 4.1: Business Dashboard

**Goal:** High-level metrics.

- **New Route:** `GET /api/admin/dashboard/stats`

**Implementation Flow:**

1. **Query 1:** `OrderRepository.sumTotalByStatus(COMPLETED)` (Total Revenue).
2. **Query 2:** `OrderRepository.countByStatus(PENDING)` (Orders needing attention).
3. **Query 3:** `UserRepository.countByRole(USER)` (Total Customers).
4. **Query 4:** Top 5 selling products (Requires complex join/group by on `OrderItems`).

**Technical Strategy:**

- **Caching:** **CRITICAL.** These are expensive database queries.
- Use `@Cacheable(value = "admin_dashboard", ttl = 3600000)` (1 hour).
- Do not calculate this live on every page refresh.

- **Scheduled Refresh:** Alternatively, have a `@Scheduled` task run every hour to calculate these numbers and store them in a `DailyStats` table or Redis directly.

---

## 5. Marketing Engine

### Feature 5.1: Coupons

**Goal:** Promotions.

- **New Entity:** `Coupon` (code, discountPercent, maxDiscount, expiryDate, minOrderValue).
- **New Route:** `POST /api/cart/apply-coupon`

**Implementation Flow:**

1. **Input:** User sends `CODE10`.
2. **Validation:** Service checks DB: Is code valid? Is date < expiry? Is Cart Total > minOrderValue?
3. **Calculation:** Calculate discount amount.
4. **Response:** Return updated `CartResponse` with `discountAmount` and `finalTotal`. Note: Do not modify the database `Cart` entity structure yet, just the calculation response, _unless_ you want to persist the applied coupon.

**Technical Strategy:**

- **Caching:** Cache coupon lookups (`@Cacheable("coupons")`). Coupon configurations rarely change.

---

## 6. Notification System (Async & Kafka)

### Feature 6.1: Event-Driven Notifications

**Goal:** Decouple core logic from side effects like email/SMS.

**Architecture Flow:**

1. **Producer:** In `OrderService`, after a successful order:

- _Instead of calling EmailService directly:_
- `kafkaTemplate.send("orders-topic", new OrderPlacedEvent(orderId, email, amount));`

2. **Consumer:** Create a `NotificationListener` service:

- `@KafkaListener(topics = "orders-topic")`
- Receives the event.
- Calls `JavaMailSender` to send the email.

**Why?** If the email server is down, the Order transaction won't fail. Kafka will retry sending the email later.

**Technical Strategy:**

- **Kafka:** **YES.** Use for "Order Placed", "Order Shipped", "User Registered".
- **Async:** Use `@Async` for simple tasks if you don't want the complexity of setting up a Kafka broker immediately. Start with `@Async`, migrate to Kafka when scaling.

---

## Summary Checklist for Developer

| Domain     | Action Item                              | Technical Pattern            | Priority |
| ---------- | ---------------------------------------- | ---------------------------- | -------- |
| **Auth**   | Implement Refresh Token Table & Endpoint | Database / Scheduled Cleanup | 🔴 High  |
| **Auth**   | Implement Forgot Password Flow           | `@Async` Email               | 🔴 High  |
| **Order**  | Add Return/Refund logic & Transaction    | `@Transactional`             | 🔴 High  |
| **Perf**   | Enable Caching on Product Catalog        | `@Cacheable` (Redis)         | 🟠 Med   |
| **Perf**   | Implement Admin Dashboard Caching        | `@Cacheable` (Long TTL)      | 🟠 Med   |
| **Market** | Add Coupon Entity & Logic                | Standard CRUD                | 🟡 Low   |
| **System** | Setup Kafka Producer/Consumer            | Kafka                        | 🟡 Low   |
