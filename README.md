# 🛒 DiddyCart – Online Shopping Backend

A scalable, production-ready e-commerce backend built with **Java** and **Spring Boot**. This project features a clean, layered architecture designed to handle real-world shopping scenarios, including role-based authentication, product management, cart operations, order lifecycle management, and payment simulation.

## 🚀 Project Overview

- **Type:** RESTful API Backend
- **Language:** Java 25
- **Framework:** Spring Boot 4.0.1
- **Database:** PostgreSQL
- **Security:** Spring Security with stateless JWT Authentication
- **Build Tool:** Maven

---

## 🛠️ Tech Stack

### Core Frameworks

- **Spring Boot Web:** REST API development.
- **Spring Data JPA:** ORM and database interactions using Hibernate.
- **Spring Security:** Authentication and Authorization.
- **Jakarta Validation:** Bean validation for request inputs.

### Infrastructure & Data

- **PostgreSQL:** Primary relational database (via Supabase).
- **Redis:** Used for caching/session management (via Docker).
- **Docker:** Containerization support for services.

### Utilities

- **JWT (jjwt):** Secure token generation and validation.
- **Lombok:** Boilerplate code reduction.
- **SpringDoc OpenAPI:** Automated API documentation and Swagger UI.

---

## 🏗️ System Architecture

The project follows a standard **Controller-Service-Repository** layered architecture:

1.  **Controller Layer (`/controller`):** Handles HTTP requests, validation, and responses.
2.  **Service Layer (`/service`):** Contains business logic (e.g., cart calculations, stock validation, payment processing).
3.  **Repository Layer (`/repository`):** Data access interfaces extending `JpaRepository`.
4.  **Security (`/config`, `/util`):** Custom `JwtAuthenticationFilter` intercepts requests to validate tokens.

---

## ✨ Key Features

### 🔐 Authentication & Security

- **User Registration & Login:** Supports auto-login upon registration.
- **JWT Auth:** Stateless authentication using Bearer tokens.
- **RBAC (Role-Based Access Control):**
  - `USER`: Browse, shop, and manage orders.
  - `VENDOR`: Manage own store and products.
  - `ADMIN`: Full system oversight.

### 🛍️ Product & Vendor Management

- **Catalog:** Pagination and search functionality for products.
- **Vendor Onboarding:** Users can register as vendors with GSTIN and Store Name.
- **Inventory Control:** Prevents ordering out-of-stock items.
- **Image Handling:** Supports product image uploads to local storage.

### 🛒 Cart & Orders

- **Persistent Cart:** Cart data is stored in the database.
- **Order Snapshotting:** Saves price and address at the moment of purchase to preserve history.
- **Order Lifecycle:** Tracks status from `PENDING` to `DELIVERED`.

### 💳 Payments

- **Processing:** Simulates payments via UPI, Card, or Net Banking.
- **Transaction Records:** Generates unique transaction IDs upon completion.

---

## 🗄️ Database Schema

The application uses a normalized PostgreSQL schema (approx. 3NF).  
Check out the detailed schema at [DrawSQl](https://drawsql.app/teams/arhan-das/diagrams/diddycart)

Key tables include:

- **Users:** Stores credentials and roles.
- **Vendors:** 1-to-1 relationship with Users.
- **Products:** Linked to Vendors and Categories.
- **Cart/CartItems:** Manages user shopping sessions.
- **Orders/OrderItems:** Stores purchase history with snapshot data.
- **Address:** User address book.

---

## ⚙️ Setup & Installation

### Prerequisites

- JDK 25 (or compatible)
- PostgreSQL
- Docker (optional, for Redis)

### 1. Configure Environment

Set the following environment variables or update `application.yaml`:

```yaml
DB_URL: jdbc:postgresql://localhost:5432/diddycart
DB_USERNAME: your_username
DB_PASSWORD: your_password
JWT_SECRET: your_secure_secret
```

### 2. Run Infrastructure

Start Redis using Docker Compose:

```bash
docker-compose up -d
```

### 3. Build & Run

Use the Maven Wrapper to build and start the application:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

### 4. Access API Documentation

Once running, explore the API via Swagger UI:

- URL: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
