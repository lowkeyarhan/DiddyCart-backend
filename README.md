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

## 📁 Project Structure

```
src/
│
├── config/
│   ├── db.js
│   ├── supabase.js
│   └── env.js
│
├── middlewares/
│   ├── auth.middleware.js
│   ├── role.middleware.js
│   ├── rateLimiter.js
│   └── error.middleware.js
│
├── controllers/
│   ├── auth.controller.js
│   ├── product.controller.js
│   ├── order.controller.js
│   ├── vendor.controller.js
│   └── analytics.controller.js
│
├── services/
│   ├── auth.service.js
│   ├── payment.service.js
│   ├── email.service.js
│   └── upload.service.js
│
├── routes/
│   ├── auth.routes.js
│   ├── product.routes.js
│   ├── order.routes.js
│   ├── vendor.routes.js
│   └── analytics.routes.js
│
├── models/
│   ├── user.model.js
│   ├── vendor.model.js
│   ├── product.model.js
│   ├── order.model.js
│   └── payment.model.js
│
├── utils/
│   ├── jwt.js
│   ├── logger.js
│   └── constants.js
│
└── app.js
```

---

## 🗄️ Database Schema (PostgreSQL)

### Users Table

| Column     | Type      | Constraints           |
| ---------- | --------- | --------------------- |
| id         | UUID      | PK                    |
| name       | VARCHAR   | NOT NULL              |
| email      | VARCHAR   | UNIQUE                |
| password   | TEXT      | HASHED                |
| role       | ENUM      | USER / VENDOR / ADMIN |
| created_at | TIMESTAMP | DEFAULT NOW           |

---

### Vendors Table

| Column      | Type      | Constraints    |
| ----------- | --------- | -------------- |
| id          | UUID      | PK             |
| user_id     | UUID      | FK → users(id) |
| shop_name   | VARCHAR   | NOT NULL       |
| is_verified | BOOLEAN   | DEFAULT FALSE  |
| created_at  | TIMESTAMP | DEFAULT NOW    |

---

### Products Table

| Column      | Type      | Constraints      |
| ----------- | --------- | ---------------- |
| id          | UUID      | PK               |
| vendor_id   | UUID      | FK → vendors(id) |
| name        | VARCHAR   | NOT NULL         |
| description | TEXT      |                  |
| price       | DECIMAL   | NOT NULL         |
| stock       | INTEGER   |                  |
| image_url   | TEXT      |                  |
| created_at  | TIMESTAMP | DEFAULT NOW      |

---

### Orders Table

| Column       | Type      | Constraints              |
| ------------ | --------- | ------------------------ |
| id           | UUID      | PK                       |
| user_id      | UUID      | FK → users(id)           |
| total_amount | DECIMAL   |                          |
| status       | ENUM      | PENDING / PAID / SHIPPED |
| created_at   | TIMESTAMP | DEFAULT NOW              |

---

### Payments Table

| Column         | Type      | Constraints       |
| -------------- | --------- | ----------------- |
| id             | UUID      | PK                |
| order_id       | UUID      | FK → orders(id)   |
| provider       | VARCHAR   | Stripe / Razorpay |
| transaction_id | TEXT      |                   |
| status         | VARCHAR   |                   |
| created_at     | TIMESTAMP | DEFAULT NOW       |

---

## 🔐 Protected APIs

| Route                | Access             |
| -------------------- | ------------------ |
| /api/orders          | Authenticated User |
| /api/vendor/products | Vendor             |
| /api/admin/analytics | Admin              |

JWT Header:

```
Authorization: Bearer <token>
```

---

## 🌐 External Integrations

- Payment Gateway: Stripe / Razorpay
- Email: SMTP (Nodemailer)
- File Storage: Supabase Storage
- Analytics APIs
- Rate Limiting

---

## 🧰 Tech Stack

**Backend**

- Node.js
- Express.js

**Database**

- PostgreSQL (Supabase)

**Authentication**

- JWT
- bcrypt

**Cloud & Services**

- Supabase
- Stripe / Razorpay
- Nodemailer

**Security**

- Helmet
- Express Rate Limit

---

## 🚀 Setup & Installation

```bash
git clone <repo>
cd backend
npm install
npm run dev
```

Create `.env`:

```
DATABASE_URL=
JWT_SECRET=
STRIPE_SECRET_KEY=
SMTP_HOST=
```

---

## ✅ Final Notes

This project is designed to reflect **industry-grade backend architecture** with emphasis on scalability, security, and clean code practices. It is suitable for academic evaluation, internships, and real-world backend demonstrations.
