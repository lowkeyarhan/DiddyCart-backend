# 🛒 DiddyCart – Online Shopping Backend

A scalable, production-style backend for an **online shopping platform**, built to feel like something that could actually survive real users instead of dying after demo day.

This project focuses on **clean architecture, security, scalability, and real-world backend workflows** using SQL in a cloud environment.

---

## 📌 Project Overview

DiddyCart is a backend system that powers an online shopping application where:

- Users can browse products and place orders
- Vendors can manage products and inventory
- Admins can monitor analytics and system health
- Payments, emails, file uploads, and external APIs are integrated

The backend is **API-driven**, **stateless**, and follows **RESTful principles** with proper authentication and authorization.

---

## ⚙️ Core Features

### 🔐 Authentication & Authorization

- User registration & login
- Password hashing using bcrypt
- JWT-based authentication
- Role-based access control (USER, VENDOR, ADMIN)
- Protected routes using middleware

---

### 🏪 Vendor Management

- Vendor onboarding
- Each product is linked to a vendor
- Vendor-only product creation & updates
- Vendor analytics access

---

### 📦 Product Management

- Product CRUD operations
- Product images upload
- Category-based filtering
- Stock and price management

---

### 🛒 Order & Payment Flow

- Cart-to-order workflow
- Order status tracking
- Payment gateway integration (Stripe / Razorpay)
- Secure webhook handling

---

### ☁️ File Uploads

- Product image uploads
- Cloud storage using Supabase Storage
- Signed URLs for secure access

---

### 📧 Email System

- SMTP-based email sending
- Order confirmation emails
- Vendor notifications

---

### 📊 Analytics

- Order analytics
- Revenue tracking
- User activity tracking
- Admin-only analytics APIs

---

### 🛡️ Security

- Rate limiting
- SQL injection prevention
- Secure HTTP headers (Helmet)
- Centralized error handling

---

## 🧠 Application Flow

### User Authentication

1. User registers/logs in
2. Password is hashed
3. JWT token is generated
4. Token is sent in headers for protected APIs

### Product Browsing

1. Public APIs fetch products
2. Pagination and filters applied
3. Vendor details attached

### Order Placement

1. User adds products to cart
2. Order is created
3. Payment intent generated
4. Payment verified via webhook
5. Order status updated

---

## 🗄️ Database Schema (PostgreSQL)

This project uses Spring Data JPA entities (see `src/main/java/com/diddycart/models`) and is designed for PostgreSQL. Check out at https://drawsql.app/teams/arhan-das/diagrams/diddycart

### ✅ Tables (as implemented by entities)

> Note: IDs are `BIGINT` (`@GeneratedValue(strategy = IDENTITY)`) in the current codebase.

#### `users`

| Column     | Type           | Constraints                          |
| ---------- | -------------- | ------------------------------------ |
| id         | BIGINT         | PK                                   |
| name       | VARCHAR        | NOT NULL                             |
| email      | VARCHAR        | NOT NULL, UNIQUE                     |
| phone      | VARCHAR        | NULL                                 |
| password   | VARCHAR/TEXT   | NOT NULL (hashed)                    |
| role       | VARCHAR (enum) | NOT NULL (`USER`, `ADMIN`, `VENDOR`) |
| created_at | TIMESTAMP      | NOT NULL                             |
| updated_at | TIMESTAMP      | NULL                                 |

#### `vendors`

| Column      | Type    | Constraints                              |
| ----------- | ------- | ---------------------------------------- |
| id          | BIGINT  | PK                                       |
| user_id     | BIGINT  | FK → `users(id)`, NOT NULL, UNIQUE (1:1) |
| store_name  | VARCHAR | NOT NULL                                 |
| gstin       | VARCHAR | NOT NULL, UNIQUE                         |
| description | TEXT    | NULL                                     |

#### `category`

| Column      | Type    | Constraints |
| ----------- | ------- | ----------- |
| id          | BIGINT  | PK          |
| type        | VARCHAR | NOT NULL    |
| description | TEXT    | NULL        |

#### `products`

| Column         | Type            | Constraints                  |
| -------------- | --------------- | ---------------------------- |
| id             | BIGINT          | PK                           |
| vendor_id      | BIGINT          | FK → `vendors(id)`, NOT NULL |
| category_id    | BIGINT          | FK → `category(id)`, NULL    |
| name           | VARCHAR         | NOT NULL                     |
| description    | TEXT            | NULL                         |
| price          | DECIMAL/NUMERIC | NOT NULL                     |
| stock_quantity | INT             | NOT NULL                     |
| added_at       | TIMESTAMP       | NULL                         |

#### `product_image`

| Column     | Type   | Constraints                   |
| ---------- | ------ | ----------------------------- |
| id         | BIGINT | PK                            |
| product_id | BIGINT | FK → `products(id)`, NOT NULL |
| image_url  | TEXT   | NOT NULL                      |

#### `cart`

| Column  | Type   | Constraints                                 |
| ------- | ------ | ------------------------------------------- |
| id      | BIGINT | PK                                          |
| user_id | BIGINT | FK → `users(id)`, NULL (guest cart allowed) |

#### `cartitem`

| Column     | Type   | Constraints                   |
| ---------- | ------ | ----------------------------- |
| id         | BIGINT | PK                            |
| cart_id    | BIGINT | FK → `cart(id)`, NOT NULL     |
| product_id | BIGINT | FK → `products(id)`, NOT NULL |
| quantity   | INT    | NOT NULL                      |

#### `address`

| Column          | Type           | Constraints                    |
| --------------- | -------------- | ------------------------------ |
| id              | BIGINT         | PK                             |
| user_id         | BIGINT         | FK → `users(id)`, NOT NULL     |
| label           | VARCHAR (enum) | NULL (`HOME`, `WORK`, `OTHER`) |
| street          | VARCHAR        | NOT NULL                       |
| landmark        | VARCHAR        | NULL                           |
| city            | VARCHAR        | NOT NULL                       |
| state           | VARCHAR        | NOT NULL                       |
| country         | VARCHAR        | NOT NULL                       |
| pincode         | VARCHAR        | NOT NULL                       |
| phone           | VARCHAR        | NULL                           |
| alternate_phone | VARCHAR        | NULL                           |

#### `orders`

| Column         | Type            | Constraints                                                             |
| -------------- | --------------- | ----------------------------------------------------------------------- |
| id             | BIGINT          | PK                                                                      |
| user_id        | BIGINT          | FK → `users(id)`, NOT NULL                                              |
| total          | DECIMAL/NUMERIC | NOT NULL                                                                |
| status         | VARCHAR (enum)  | NOT NULL (`PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`) |
| payment_status | VARCHAR (enum)  | NOT NULL (`PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`)                 |
| street         | VARCHAR         | NULL (snapshot at checkout)                                             |
| landmark       | VARCHAR         | NULL (snapshot at checkout)                                             |
| city           | VARCHAR         | NULL (snapshot at checkout)                                             |
| pincode        | VARCHAR         | NULL (snapshot at checkout)                                             |
| created_at     | TIMESTAMP       | NOT NULL                                                                |

#### `orderitems`

| Column     | Type            | Constraints                                              |
| ---------- | --------------- | -------------------------------------------------------- |
| id         | BIGINT          | PK                                                       |
| order_id   | BIGINT          | FK → `orders(id)`, NOT NULL                              |
| product_id | BIGINT          | FK → `products(id)`, NULL (product may be deleted later) |
| price      | DECIMAL/NUMERIC | NOT NULL (snapshot price)                                |
| quantity   | INT             | NOT NULL                                                 |

#### `payment`

| Column         | Type            | Constraints                                             |
| -------------- | --------------- | ------------------------------------------------------- |
| id             | BIGINT          | PK                                                      |
| order_id       | BIGINT          | FK → `orders(id)`, NOT NULL                             |
| amount         | DECIMAL/NUMERIC | NOT NULL                                                |
| mode           | VARCHAR (enum)  | NOT NULL (`UPI`, `CARD`, `NET_BANKING`, `COD`)          |
| status         | VARCHAR (enum)  | NOT NULL (`PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`) |
| transaction_id | VARCHAR         | NULL                                                    |
| created_at     | TIMESTAMP       | NOT NULL                                                |

---

### 📌 Indexing (recommended)

PostgreSQL automatically creates indexes for `PRIMARY KEY` and `UNIQUE` constraints. In addition, these indexes are strongly recommended for query performance:

- Foreign-key lookup indexes:
  - `vendors(user_id)` (already UNIQUE, so indexed)
  - `products(vendor_id)`, `products(category_id)`
  - `product_image(product_id)`
  - `cart(user_id)`
  - `cartitem(cart_id)`, `cartitem(product_id)`
  - `address(user_id)`
  - `orders(user_id)`
  - `orderitems(order_id)`, `orderitems(product_id)`
  - `payment(order_id)`

- Common filter/sort indexes:
  - `products(name)` (or `GIN`/`GiST` full-text index if you add search)
  - `products(price)` if you sort/filter by price frequently
  - `orders(created_at)` for “recent orders” screens
  - `orders(status)`, `orders(payment_status)` for admin/vendor dashboards

- Uniqueness constraints:
  - `users(email)` (already UNIQUE)
  - `vendors(gstin)` (already UNIQUE)

---

### 🧩 Normalization

- The schema is mostly **3NF**: core entities (`users`, `vendors`, `products`, `category`) are separated, and many-to-one relationships are represented via foreign keys.
- Intentional denormalization exists in `orders` where address fields are stored as a **snapshot** at checkout time. This is a common pattern to preserve historical delivery info even if a user edits/deletes an address later.

---

### 💾 Space Complexity (data + indexes)

Let $U,V,C,P,I,CA,CI,A,O,OI,PA$ be the row counts of the tables above.

- Table storage grows linearly: $O(U+V+C+P+I+CA+CI+A+O+OI+PA)$ rows.
- Each B-tree index adds additional linear overhead: for an indexed column on a table with $N$ rows, index size is $O(N)$ (plus per-entry overhead). With $k$ indexes on that table, it’s $O(kN)$.
- Total database size is therefore approximately:
  - Data: $O(\text{total rows})$
  - Indexes: $O(\text{total indexed rows across all indexes})$

---
