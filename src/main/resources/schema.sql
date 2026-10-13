-- FoodXpress Complete Database Schema
-- MySQL 8.0+
-- Run with: mysql -u root -p < schema.sql
-- Idempotent: safe to run multiple times

CREATE DATABASE IF NOT EXISTS foodxpress CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE foodxpress;

-- 1. users
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    role ENUM('customer', 'admin', 'delivery') NOT NULL DEFAULT 'customer',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL
) ENGINE=InnoDB;

-- 2. restaurants
CREATE TABLE IF NOT EXISTS restaurants (
    restaurant_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    cuisine_type VARCHAR(100),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    zip VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(100),
    rating DECIMAL(3,2) DEFAULT 0.00,
    delivery_time INT DEFAULT 30 COMMENT 'Estimated delivery time in minutes',
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. menu_items
CREATE TABLE IF NOT EXISTS menu_items (
    menu_item_id INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    is_veg BOOLEAN DEFAULT TRUE,
    is_available BOOLEAN DEFAULT TRUE,
    stock_quantity INT NOT NULL DEFAULT 100 COMMENT 'Inventory count; -1 = unlimited',
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    INDEX idx_menu_restaurant (restaurant_id)
) ENGINE=InnoDB;

-- 4. addresses
CREATE TABLE IF NOT EXISTS addresses (
    address_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    street TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    zip VARCHAR(20),
    landmark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_address_user (user_id)
) ENGINE=InnoDB;

-- 5. carts (one per user)
CREATE TABLE IF NOT EXISTS carts (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. cart_items
CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    cart_id INT NOT NULL,
    menu_item_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES carts(cart_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(menu_item_id) ON DELETE CASCADE,
    UNIQUE KEY uq_cart_item (cart_id, menu_item_id),
    INDEX idx_cart_items_cart (cart_id)
) ENGINE=InnoDB;

-- 7. delivery_batches
CREATE TABLE IF NOT EXISTS delivery_batches (
    batch_id INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id INT NOT NULL,
    delivery_zone VARCHAR(255) COMMENT 'Campus zone / hostel block',
    status ENUM('open', 'dispatched', 'delivered', 'cancelled') NOT NULL DEFAULT 'open',
    max_size INT NOT NULL DEFAULT 5,
    dispatched_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id)
) ENGINE=InnoDB;

-- 8. orders
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    restaurant_id INT NOT NULL,
    address_id INT NULL,
    batch_id INT NULL COMMENT 'FK to delivery_batches; null until batched',
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('placed','accepted','preparing','ready_for_pickup','out_for_delivery','delivered','cancelled','rejected') NOT NULL DEFAULT 'placed',
    payment_method ENUM('cash_on_delivery','upi','card') NOT NULL DEFAULT 'cash_on_delivery',
    payment_status ENUM('pending','completed','failed') NOT NULL DEFAULT 'pending',
    delivery_instructions TEXT,
    idempotency_key VARCHAR(64) UNIQUE COMMENT 'Prevents duplicate order submission',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id),
    FOREIGN KEY (address_id) REFERENCES addresses(address_id) ON DELETE SET NULL,
    FOREIGN KEY (batch_id) REFERENCES delivery_batches(batch_id) ON DELETE SET NULL,
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_restaurant (restaurant_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_batch (batch_id)
) ENGINE=InnoDB;

-- 9. order_items
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    menu_item_id INT NOT NULL,
    quantity INT NOT NULL,
    price_at_order DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(menu_item_id),
    INDEX idx_order_items_order (order_id)
) ENGINE=InnoDB;

-- 10. batch_assignments (prevents an order going into 2 batches)
CREATE TABLE IF NOT EXISTS batch_assignments (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    batch_id INT NOT NULL,
    order_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (batch_id) REFERENCES delivery_batches(batch_id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    UNIQUE KEY uq_order_assignment (order_id) COMMENT 'An order can be in at most one batch'
) ENGINE=InnoDB;

-- 11. group_orders
CREATE TABLE IF NOT EXISTS group_orders (
    group_order_id INT AUTO_INCREMENT PRIMARY KEY,
    creator_id INT NOT NULL,
    restaurant_id INT NOT NULL,
    invite_code VARCHAR(20) NOT NULL UNIQUE,
    deadline TIMESTAMP NOT NULL,
    status ENUM('open','closed','checked_out','expired','failed') NOT NULL DEFAULT 'open',
    result_order_id INT NULL COMMENT 'The order created after checkout',
    checkout_idempotency_key VARCHAR(64) UNIQUE COMMENT 'Prevents duplicate group checkout',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(user_id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id),
    FOREIGN KEY (result_order_id) REFERENCES orders(order_id) ON DELETE SET NULL,
    INDEX idx_group_invite (invite_code)
) ENGINE=InnoDB;

-- 12. group_members
CREATE TABLE IF NOT EXISTS group_members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    group_order_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_order_id) REFERENCES group_orders(group_order_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE KEY uq_group_member (group_order_id, user_id)
) ENGINE=InnoDB;

-- 13. group_cart_items
CREATE TABLE IF NOT EXISTS group_cart_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    group_order_id INT NOT NULL,
    user_id INT NOT NULL COMMENT 'Which member added this item',
    menu_item_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (group_order_id) REFERENCES group_orders(group_order_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(menu_item_id),
    UNIQUE KEY uq_group_cart_item (group_order_id, user_id, menu_item_id),
    INDEX idx_group_cart (group_order_id)
) ENGINE=InnoDB;

-- 14. surplus_listings
CREATE TABLE IF NOT EXISTS surplus_listings (
    listing_id INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id INT NOT NULL,
    menu_item_id INT NOT NULL,
    original_price DECIMAL(10,2) NOT NULL,
    discounted_price DECIMAL(10,2) NOT NULL,
    available_quantity INT NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    status ENUM('active','expired','sold_out','cancelled') NOT NULL DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(menu_item_id),
    INDEX idx_surplus_status (status),
    INDEX idx_surplus_expiry (expiry_time)
) ENGINE=InnoDB;

-- 15. surplus_reservations
CREATE TABLE IF NOT EXISTS surplus_reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    listing_id INT NOT NULL,
    user_id INT NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status ENUM('reserved','collected','cancelled','expired') NOT NULL DEFAULT 'reserved',
    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (listing_id) REFERENCES surplus_listings(listing_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_surplus_res_listing (listing_id),
    INDEX idx_surplus_res_user (user_id)
) ENGINE=InnoDB;

-- 16. notification_log
CREATE TABLE IF NOT EXISTS notification_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NULL,
    user_id INT NULL,
    message TEXT NOT NULL,
    status ENUM('pending','sent','failed') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    INDEX idx_notif_order (order_id)
) ENGINE=InnoDB;
