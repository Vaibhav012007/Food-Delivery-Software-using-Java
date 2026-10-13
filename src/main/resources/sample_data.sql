-- FoodXpress Sample Data Script
-- MySQL 8.0+
-- Run with: mysql -u root -p < sample_data.sql
-- Idempotent: uses INSERT IGNORE to prevent duplicate key errors

USE foodxpress;

-- ============================================================================
-- 1. USERS
-- ============================================================================
-- NOTE: Passwords are BCrypt-hashed values:
--   - admin     -> 'admin123' (hash: $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4tbQK0/Uei)
--   - customer1 -> 'pass123'  (hash: $2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq)
--   - customer2 -> 'pass123'  (hash: $2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq)
-- If passwords need to be modified in production, regenerate using BCrypt.hashpw(plainText, BCrypt.gensalt(12)).

INSERT IGNORE INTO users (user_id, name, username, password, email, phone, address, role) VALUES
(1, 'System Administrator', 'admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4tbQK0/Uei', 'admin@foodxpress.com', '+91 9876543210', 'Admin Block, Room 101, Campus Main Building', 'admin'),
(2, 'Rahul Sharma', 'customer1', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'customer1@foodxpress.com', '+91 9876543211', 'Hostel 4, Room 204, Campus North Zone', 'customer'),
(3, 'Priya Patel', 'customer2', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWq', 'customer2@foodxpress.com', '+91 9876543212', 'Hostel 7, Room 112, Campus South Zone', 'customer');

-- ============================================================================
-- 2. ADDRESSES (Sample delivery addresses for customers)
-- ============================================================================
INSERT IGNORE INTO addresses (address_id, user_id, street, city, state, zip, landmark) VALUES
(1, 2, 'Hostel 4, Room 204', 'University Campus', 'Delhi', '110007', 'Near North Mess'),
(2, 3, 'Hostel 7, Room 112', 'University Campus', 'Delhi', '110007', 'Near Basketball Court');

-- ============================================================================
-- 3. RESTAURANTS
-- ============================================================================
INSERT IGNORE INTO restaurants (restaurant_id, name, description, cuisine_type, address, city, state, zip, phone, email, rating, delivery_time, image_url, is_active) VALUES
(1, 'Spice Garden', 'Authentic North Indian curries, fresh tandoori breads, rich gravies, and aromatic biryanis prepared with traditional spices.', 'North Indian', 'Student Activity Center, Food Court Stall 1', 'University Campus', 'Delhi', '110007', '+91 9876543220', 'contact@spicegarden.com', 4.50, 25, 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=600&q=80', TRUE),
(2, 'Burger Factory', 'Juicy flame-grilled burgers, crispy golden fries, thick milkshakes, and delicious quick bites made fresh to order.', 'American Fast Food', 'Commercial Center, Block C, Near North Gate', 'University Campus', 'Delhi', '110007', '+91 9876543221', 'orders@burgerfactory.com', 4.30, 20, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=600&q=80', TRUE);

-- ============================================================================
-- 4. MENU ITEMS (4 per restaurant, mix of veg/non-veg, stock_quantity=50)
-- ============================================================================
-- Spice Garden (Restaurant 1)
INSERT IGNORE INTO menu_items (menu_item_id, restaurant_id, name, description, price, category, is_veg, is_available, stock_quantity, image_url) VALUES
(1, 1, 'Paneer Butter Masala', 'Soft cottage cheese cubes cooked in a rich, buttery tomato-cashew gravy with aromatic spices.', 180.00, 'Main Course', TRUE, TRUE, 50, 'https://images.unsplash.com/photo-1631452180519-c014fe946bc7?auto=format&fit=crop&w=600&q=80'),
(2, 1, 'Chicken Biryani', 'Fragrant long-grain basmati rice layered with spiced marinated chicken, caramelized onions, and saffron.', 220.00, 'Biryani', FALSE, TRUE, 50, 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=600&q=80'),
(3, 1, 'Garlic Butter Naan', 'Freshly baked tandoori flatbread topped with minced roasted garlic, coriander, and melted butter.', 45.00, 'Breads', TRUE, TRUE, 50, 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=600&q=80'),
(4, 1, 'Butter Chicken', 'Tender boneless chicken pieces cooked in a velvety spiced makhani gravy with fenugreek leaves.', 240.00, 'Main Course', FALSE, TRUE, 50, 'https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=600&q=80');

-- Burger Factory (Restaurant 2)
INSERT IGNORE INTO menu_items (menu_item_id, restaurant_id, name, description, price, category, is_veg, is_available, stock_quantity, image_url) VALUES
(5, 2, 'Classic Veggie Burger', 'Crisp herb-seasoned vegetable patty, crisp iceberg lettuce, sliced tomatoes, and house herb mayo on a toasted sesame bun.', 120.00, 'Burgers', TRUE, TRUE, 50, 'https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=600&q=80'),
(6, 2, 'Crispy Chicken Burger', 'Crispy spiced golden chicken breast fillet with zesty pickled gherkins, shredded lettuce, and creamy garlic mayonnaise.', 160.00, 'Burgers', FALSE, TRUE, 50, 'https://images.unsplash.com/photo-1625813506062-0aeb1d7a094b?auto=format&fit=crop&w=600&q=80'),
(7, 2, 'Loaded Cheesy Fries', 'Crispy skin-on french fries drenched in warm cheddar cheese sauce, topped with jalapeño peppers and smoked paprika.', 99.00, 'Sides', TRUE, TRUE, 50, 'https://images.unsplash.com/photo-1576107232684-1279f3908594?auto=format&fit=crop&w=600&q=80'),
(8, 2, 'BBQ Bacon Cheeseburger', 'Juicy grilled beef patty layered with smoky barbecue glaze, crispy strips, cheddar cheese, and caramelized onions.', 199.00, 'Burgers', FALSE, TRUE, 50, 'https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5?auto=format&fit=crop&w=600&q=80');
