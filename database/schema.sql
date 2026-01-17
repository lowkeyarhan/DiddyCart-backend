-- ========================================
-- DiddyCart Database Schema
-- PostgreSQL DDL for Supabase
-- ========================================

-- Drop existing tables (in reverse order of dependencies)
DROP TABLE IF EXISTS payment CASCADE;
DROP TABLE IF EXISTS orderitems CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS cartitem CASCADE;
DROP TABLE IF EXISTS cart CASCADE;
DROP TABLE IF EXISTS product_image CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS address CASCADE;
DROP TABLE IF EXISTS vendors CASCADE;
DROP TABLE IF EXISTS category CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Drop existing types
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS order_status CASCADE;
DROP TYPE IF EXISTS payment_status CASCADE;
DROP TYPE IF EXISTS payment_mode CASCADE;
DROP TYPE IF EXISTS address_label CASCADE;

-- ========================================
-- ENUM TYPES
-- ========================================

CREATE TYPE user_role AS ENUM ('USER', 'ADMIN', 'VENDOR');
CREATE TYPE order_status AS ENUM ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED');
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');
CREATE TYPE payment_mode AS ENUM ('UPI', 'CARD', 'NET_BANKING', 'CASH_ON_DELIVERY');
CREATE TYPE address_label AS ENUM ('HOME', 'WORK', 'OTHER');

-- ========================================
-- TABLE: users
-- ========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for faster email lookups
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- ========================================
-- TABLE: vendors
-- ========================================
CREATE TABLE vendors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    store_name VARCHAR(255) NOT NULL,
    gstin VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    CONSTRAINT fk_vendor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_vendors_user_id ON vendors(user_id);

-- ========================================
-- TABLE: category
-- ========================================
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    description TEXT
);

-- ========================================
-- TABLE: products
-- ========================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    category_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_product_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
);

CREATE INDEX idx_products_vendor_id ON products(vendor_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_price ON products(price);

-- ========================================
-- TABLE: product_image
-- ========================================
CREATE TABLE product_image (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_image_product_id ON product_image(product_id);

-- ========================================
-- TABLE: address
-- ========================================
CREATE TABLE address (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label address_label,
    street VARCHAR(255) NOT NULL,
    landmark VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    phone VARCHAR(50),
    alternate_phone VARCHAR(50),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_address_user_id ON address(user_id);

-- ========================================
-- TABLE: cart
-- ========================================
CREATE TABLE cart (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_user_id ON cart(user_id);

-- ========================================
-- TABLE: cartitem
-- ========================================
CREATE TABLE cartitem (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_cartitem_cart FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_cartitem_cart_id ON cartitem(cart_id);
CREATE INDEX idx_cartitem_product_id ON cartitem(product_id);

-- ========================================
-- TABLE: orders
-- ========================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total NUMERIC(10, 2) NOT NULL,
    status order_status NOT NULL DEFAULT 'PENDING',
    payment_status payment_status NOT NULL DEFAULT 'PENDING',
    street VARCHAR(255),
    landmark VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- ========================================
-- TABLE: orderitems
-- ========================================
CREATE TABLE orderitems (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    price NUMERIC(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
);

CREATE INDEX idx_orderitems_order_id ON orderitems(order_id);
CREATE INDEX idx_orderitems_product_id ON orderitems(product_id);

-- ========================================
-- TABLE: payment
-- ========================================
CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    mode payment_mode NOT NULL,
    status payment_status NOT NULL,
    transaction_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_order_id ON payment(order_id);
CREATE INDEX idx_payment_transaction_id ON payment(transaction_id);

-- ========================================
-- SAMPLE DATA (Optional - for testing)
-- ========================================

-- Insert sample categories
INSERT INTO category (type, description) VALUES
('Electronics', 'Electronic devices and accessories'),
('Fashion', 'Clothing, shoes, and accessories'),
('Home & Kitchen', 'Furniture and home appliances'),
('Books', 'Physical and digital books'),
('Sports', 'Sports equipment and fitness gear');

-- Note: To insert users, you need to hash passwords first.
-- This is just an example structure:
-- INSERT INTO users (name, email, phone, password, role) VALUES
-- ('Admin User', 'admin@diddycart.com', '9876543210', '<bcrypt-hash>', 'ADMIN'),
-- ('Test Vendor', 'vendor@test.com', '9876543211', '<bcrypt-hash>', 'VENDOR'),
-- ('Test User', 'user@test.com', '9876543212', '<bcrypt-hash>', 'USER');

-- ========================================
-- COMPLETION MESSAGE
-- ========================================
-- All tables created successfully!
-- Total tables: 11
-- - users, vendors, category, products, product_image
-- - address, cart, cartitem, orders, orderitems, payment
