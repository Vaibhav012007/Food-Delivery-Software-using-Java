package com.app.service;

import com.app.models.Order;
import com.app.models.OrderItem;
import java.util.List;

/**
 * Service interface for managing customer orders.
 * Defines business operations for placing, retrieving, transitioning,
 * and cancelling food delivery orders.
 */
public interface OrderService {
    /**
     * Places a new order atomically.
     * Validates cart, prices, idempotency key (prevents duplicate submissions).
     *
     * @param userId               ID of the user placing the order
     * @param restaurantId         ID of the restaurant
     * @param addressId            delivery address ID (nullable)
     * @param paymentMethod        method of payment (e.g., cash_on_delivery, upi, card)
     * @param deliveryInstructions optional notes for delivery
     * @param orderItems           list of items in the order
     * @param totalAmount          total amount of the order
     * @param idempotencyKey       session-bound UUID to prevent double submission
     * @return generated order ID
     * @throws com.app.exceptions.DuplicateCheckoutException if key already used
     */
    int placeOrder(int userId, int restaurantId, Integer addressId,
                   String paymentMethod, String deliveryInstructions,
                   List<OrderItem> orderItems, double totalAmount,
                   String idempotencyKey);

    /**
     * Retrieves an order by its unique ID.
     *
     * @param orderId ID of the order
     * @return Order object, or null if not found
     */
    Order getOrderById(int orderId);

    /**
     * Retrieves all orders placed by a specific user.
     *
     * @param userId ID of the user
     * @return list of orders belonging to the user
     */
    List<Order> getOrdersByUser(int userId);

    /**
     * Retrieves all orders placed for a specific restaurant.
     *
     * @param restaurantId ID of the restaurant
     * @return list of orders for the restaurant
     */
    List<Order> getOrdersByRestaurant(int restaurantId);

    /**
     * Updates order status, enforcing the state machine.
     *
     * @param orderId       ID of the order to update
     * @param newStatus     target status string
     * @param requesterId   ID of the user making the request
     * @param requesterRole role of the requester (e.g., admin, customer)
     * @throws com.app.exceptions.InvalidOrderStateException on invalid transition
     * @throws com.app.exceptions.UnauthorizedAccessException if requesterId not authorized
     */
    void updateStatus(int orderId, String newStatus, int requesterId, String requesterRole);

    /**
     * Cancels an order if within allowable states.
     *
     * @param orderId ID of the order to cancel
     * @param userId  ID of the user requesting cancellation
     */
    void cancelOrder(int orderId, int userId);

    /**
     * Retrieves all items associated with a given order.
     *
     * @param orderId ID of the order
     * @return list of order items
     */
    List<OrderItem> getOrderItems(int orderId);
}
