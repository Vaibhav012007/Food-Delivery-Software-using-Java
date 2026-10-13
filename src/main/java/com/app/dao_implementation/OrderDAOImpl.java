package com.app.dao_implementation;

import com.app.dao.OrderDAO;
import com.app.models.Order;
import com.app.models.OrderItem;
import com.app.utility.DBConnection;
import com.app.exceptions.InsufficientStockException;
import com.app.exceptions.AppException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAOImpl
 * -------------
 * Implements all database operations defined in OrderDAO interface.
 * Manages order placement, retrieval, updates, and cancellation.
 */
public class OrderDAOImpl implements OrderDAO {

    private static final String INSERT_ORDER_SQL =
            "INSERT INTO orders (user_id, restaurant_id, address_id, total_amount, status, payment_method, payment_status, delivery_instructions, idempotency_key) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ORDER_ITEM_SQL =
            "INSERT INTO order_items (order_id, menu_item_id, quantity, price_at_order) VALUES (?, ?, ?, ?)";

    private static final String DECREMENT_STOCK_SQL =
            "UPDATE menu_items SET stock_quantity = CASE WHEN stock_quantity = -1 THEN -1 ELSE stock_quantity - ? END "
          + "WHERE menu_item_id = ? AND restaurant_id = ? AND is_available = TRUE "
          + "AND (stock_quantity = -1 OR stock_quantity >= ?)";

    private static final String SELECT_ORDER_BY_ID_SQL =
            "SELECT * FROM orders WHERE order_id = ?";

    private static final String SELECT_ORDER_BY_IDEMPOTENCY_KEY_SQL =
            "SELECT * FROM orders WHERE idempotency_key = ?";

    private static final String SELECT_ORDERS_BY_USER_SQL =
            "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";

    private static final String SELECT_ORDERS_BY_RESTAURANT_SQL =
            "SELECT * FROM orders WHERE restaurant_id = ? ORDER BY created_at DESC";

    private static final String UPDATE_ORDER_STATUS_SQL =
            "UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";

    private static final String UPDATE_PAYMENT_STATUS_SQL =
            "UPDATE orders SET payment_status = ?, updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";

    private static final String SELECT_ORDER_ITEMS_SQL =
            "SELECT * FROM order_items WHERE order_id = ?";

    private static final String CANCEL_ORDER_SQL =
            "UPDATE orders SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";

    private static final String SELECT_ORDERS_BY_STATUS_SQL =
            "SELECT * FROM orders WHERE status = ? ORDER BY created_at DESC";


    /**
     * 🔹 Places a new order along with its order items.
     * Returns the generated order ID after successful insertion.
     */
    @Override
    public int placeOrder(Order order, List<OrderItem> orderItems) {
        int generatedOrderId = -1;
        Connection conn = null;

        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one item");
        }

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            try (PreparedStatement orderStmt = conn.prepareStatement(INSERT_ORDER_SQL, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement itemStmt = conn.prepareStatement(INSERT_ORDER_ITEM_SQL);
                 PreparedStatement stockStmt = conn.prepareStatement(DECREMENT_STOCK_SQL)) {

                // Conditional updates make finite stock reservation atomic.  Unlimited
                // inventory is represented by -1 and is intentionally unchanged.
                for (OrderItem item : orderItems) {
                    if (item.getQuantity() <= 0) {
                        throw new IllegalArgumentException("Order item quantity must be positive");
                    }
                    stockStmt.setInt(1, item.getQuantity());
                    stockStmt.setInt(2, item.getMenuItemId());
                    stockStmt.setInt(3, order.getRestaurantId());
                    stockStmt.setInt(4, item.getQuantity());
                    if (stockStmt.executeUpdate() != 1) {
                        throw new InsufficientStockException(item.getMenuItemId(), item.getQuantity(), 0);
                    }
                }

                // Insert into orders table
                orderStmt.setInt(1, order.getUserId());
                orderStmt.setInt(2, order.getRestaurantId());

                // Handle nullable addressId properly (Integer object type)
                if (order.getAddressId() != null) {
                    orderStmt.setInt(3, order.getAddressId());
                } else {
                    orderStmt.setNull(3, Types.INTEGER);
                }

                orderStmt.setDouble(4, order.getTotalAmount());
                orderStmt.setString(5, order.getStatus());
                orderStmt.setString(6, order.getPaymentMethod());
                orderStmt.setString(7, order.getPaymentStatus());
                orderStmt.setString(8, order.getDeliveryInstructions());

                // idempotency_key (nullable)
                if (order.getIdempotencyKey() != null) {
                    orderStmt.setString(9, order.getIdempotencyKey());
                } else {
                    orderStmt.setNull(9, Types.VARCHAR);
                }

                orderStmt.executeUpdate();

                // Fetch generated order_id
                try (ResultSet rs = orderStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedOrderId = rs.getInt(1);
                    }
                }

                // Insert each order item (use double for priceAtOrder)
                for (OrderItem item : orderItems) {
                    itemStmt.setInt(1, generatedOrderId);
                    itemStmt.setInt(2, item.getMenuItemId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getPriceAtOrder()); // double as in OrderItem
                    itemStmt.addBatch();
                }

                itemStmt.executeBatch(); // Batch insert
                conn.commit(); // Commit transaction
            } catch (SQLException ex) {
                // rollback if any failure within the transaction
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rbEx) {
                        rbEx.printStackTrace();
                    }
                }
                throw ex; // rethrow to be caught outer and printed
            } catch (RuntimeException ex) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rbEx) {
                        rbEx.printStackTrace();
                    }
                }
                throw ex;
            }

        } catch (SQLException e) {
            throw new AppException("Unable to place order", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }

        return generatedOrderId;
    }

    /**
     * 🔹 Fetch a single order by ID.
     */
    @Override
    public Order getOrderById(int orderId) {
        Order order = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ORDER_BY_ID_SQL)) {

            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    order = mapOrder(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return order;
    }

    /**
     * 🔹 Get all orders placed by a specific user.
     */
    @Override
    public List<Order> getOrdersByUserId(int userId) {
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ORDERS_BY_USER_SQL)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    /**
     * 🔹 Get all orders for a restaurant.
     */
    @Override
    public List<Order> getOrdersByRestaurantId(int restaurantId) {
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ORDERS_BY_RESTAURANT_SQL)) {

            stmt.setInt(1, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    /**
     * 🔹 Update order status.
     */
    @Override
    public void updateOrderStatus(int orderId, String status) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_ORDER_STATUS_SQL)) {

            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔹 Update payment status.
     */
    @Override
    public void updatePaymentStatus(int orderId, String paymentStatus) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_PAYMENT_STATUS_SQL)) {

            stmt.setString(1, paymentStatus);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔹 Get all order items for a given order.
     */
    @Override
    public List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ORDER_ITEMS_SQL)) {

            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setOrderItemId(rs.getInt("order_item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setMenuItemId(rs.getInt("menu_item_id"));
                    item.setQuantity(rs.getInt("quantity"));
                    // priceAtOrder is a double in OrderItem
                    item.setPriceAtOrder(rs.getDouble("price_at_order"));
                    item.setCreatedAt(rs.getTimestamp("created_at"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    /**
     * 🔹 Cancel an order.
     */
    @Override
    public void cancelOrder(int orderId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CANCEL_ORDER_SQL)) {

            stmt.setInt(1, orderId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔹 Get all orders filtered by a specific status.
     */
    @Override
    public List<Order> getOrdersByStatus(String status) {
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ORDERS_BY_STATUS_SQL)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    /**
     * 🔹 Fetch order details by its idempotency key (for duplicate detection).
     */
    @Override
    public Order getOrderByIdempotencyKey(String idempotencyKey) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ORDER_BY_IDEMPOTENCY_KEY_SQL)) {
            stmt.setString(1, idempotencyKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    // 🧩 Utility Method — Converts ResultSet row to Order object
    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setUserId(rs.getInt("user_id"));
        order.setRestaurantId(rs.getInt("restaurant_id"));

        int addr = rs.getInt("address_id");
        order.setAddressId(rs.wasNull() ? null : addr);

        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setDeliveryInstructions(rs.getString("delivery_instructions"));
        order.setIdempotencyKey(rs.getString("idempotency_key"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        order.setUpdatedAt(rs.getTimestamp("updated_at"));
        return order;
    }
}
