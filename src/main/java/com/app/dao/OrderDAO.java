package com.app.dao;

import java.util.List;
import com.app.models.Order;
import com.app.models.OrderItem;

/**
 * OrderDAO Interface
 * ------------------
 * Handles all database operations related to order placement, 
 * retrieval, status updates, and cancellation.
 */
public interface OrderDAO {

    // 🔹 Place a new order and return the generated order ID
    int placeOrder(Order order, List<OrderItem> orderItems);

    // 🔹 Fetch order details by its unique order ID
    Order getOrderById(int orderId);

    // 🔹 Get all orders placed by a specific user
    List<Order> getOrdersByUserId(int userId);

    // 🔹 Retrieve all orders associated with a particular restaurant
    List<Order> getOrdersByRestaurantId(int restaurantId);

    // 🔹 Update the current status of an order (e.g., PLACED → PREPARING)
    void updateOrderStatus(int orderId, String status);

    // 🔹 Update the payment status (e.g., PENDING → COMPLETED)
    void updatePaymentStatus(int orderId, String paymentStatus);

    // 🔹 Get all items associated with a specific order
    List<OrderItem> getOrderItems(int orderId);

    // 🔹 Cancel an order (within the allowed cancellation window)
    void cancelOrder(int orderId);

    // 🔹 Get all orders filtered by a specific status (e.g., DELIVERED, CANCELLED)
    List<Order> getOrdersByStatus(String status);

    // 🔹 Get order by its idempotency key (for duplicate detection)
    Order getOrderByIdempotencyKey(String idempotencyKey);
}

