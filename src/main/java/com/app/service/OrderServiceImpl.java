package com.app.service;

import com.app.dao.OrderDAO;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.exceptions.DuplicateCheckoutException;
import com.app.exceptions.InvalidOrderStateException;
import com.app.exceptions.UnauthorizedAccessException;
import com.app.models.Order;
import com.app.models.OrderItem;
import com.app.models.OrderStatus;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Default implementation of {@link OrderService}.
 * Provides transactional order creation with idempotency verification,
 * state machine enforcement on status transitions, and user permission checks.
 */
public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;

    public OrderServiceImpl() {
        this.orderDAO = new OrderDAOImpl();
    }

    public OrderServiceImpl(OrderDAO orderDAO) {
        this.orderDAO = orderDAO;
    }

    @Override
    public int placeOrder(int userId, int restaurantId, Integer addressId,
                          String paymentMethod, String deliveryInstructions,
                          List<OrderItem> orderItems, double totalAmount,
                          String idempotencyKey) {
        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurantId(restaurantId);
        order.setAddressId(addressId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PLACED.getDbValue());
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "cash_on_delivery");
        order.setPaymentStatus("pending");
        order.setDeliveryInstructions(deliveryInstructions);
        order.setIdempotencyKey(idempotencyKey);

        try {
            int orderId = orderDAO.placeOrder(order, orderItems);
            if (orderId <= 0) {
                throw new com.app.exceptions.AppException("Order placement failed: no order ID returned");
            }
            System.out.println("[OrderService] Order placed: orderId=" + orderId + ", userId=" + userId);
            return orderId;
        } catch (com.app.exceptions.AppException e) {
            if (isDuplicateIdempotencyKey(e)) {
                Order existing = orderDAO.getOrderByIdempotencyKey(idempotencyKey);
                throw new DuplicateCheckoutException(existing != null ? existing.getOrderId() : -1);
            }
            throw e;
        } catch (Exception e) {
            if (isDuplicateIdempotencyKey(e)) {
                // Find the existing order by idempotency key
                Order existing = orderDAO.getOrderByIdempotencyKey(idempotencyKey);
                int existingId = (existing != null) ? existing.getOrderId() : -1;
                throw new DuplicateCheckoutException(existingId);
            }
            throw new com.app.exceptions.AppException("Order placement failed: " + e.getMessage(), e);
        }
    }

    private boolean isDuplicateIdempotencyKey(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (current instanceof SQLIntegrityConstraintViolationException
                    || (message != null && message.contains("Duplicate entry")
                    && message.contains("idempotency_key"))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Order getOrderById(int orderId) {
        return orderDAO.getOrderById(orderId);
    }

    @Override
    public List<Order> getOrdersByUser(int userId) {
        return orderDAO.getOrdersByUserId(userId);
    }

    @Override
    public List<Order> getOrdersByRestaurant(int restaurantId) {
        return orderDAO.getOrdersByRestaurantId(restaurantId);
    }

    @Override
    public void updateStatus(int orderId, String newStatus, int requesterId, String requesterRole) {
        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new com.app.exceptions.AppException("Order not found: " + orderId);
        }

        // Authorization: only admin or the order owner can update
        if (!"admin".equalsIgnoreCase(requesterRole) && order.getUserId() != requesterId) {
            throw new UnauthorizedAccessException(requesterId, "update order " + orderId);
        }

        // State machine validation
        OrderStatus current = OrderStatus.fromDbValue(order.getStatus());
        OrderStatus next;
        try {
            next = OrderStatus.fromDbValue(newStatus);
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderStateException(orderId, order.getStatus(), newStatus);
        }

        if (!current.canTransitionTo(next)) {
            throw new InvalidOrderStateException(orderId, current.getDbValue(), next.getDbValue());
        }

        orderDAO.updateOrderStatus(orderId, newStatus);
        System.out.println("[OrderService] Order " + orderId + " status: " + current.getDbValue() + " -> " + next.getDbValue());
    }

    @Override
    public void cancelOrder(int orderId, int userId) {
        Order order = orderDAO.getOrderById(orderId);
        if (order == null) {
            throw new com.app.exceptions.AppException("Order not found: " + orderId);
        }
        if (order.getUserId() != userId) {
            throw new UnauthorizedAccessException(userId, "cancel order " + orderId);
        }
        OrderStatus current = OrderStatus.fromDbValue(order.getStatus());
        if (!current.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new InvalidOrderStateException(orderId, current.getDbValue(), "cancelled");
        }
        orderDAO.cancelOrder(orderId);
        System.out.println("[OrderService] Order " + orderId + " cancelled by user " + userId);
    }

    @Override
    public List<OrderItem> getOrderItems(int orderId) {
        return orderDAO.getOrderItems(orderId);
    }
}
