package com.app.dao_implementation;

import com.app.dao.DeliveryBatchDAO;
import com.app.models.BatchAssignment;
import com.app.models.DeliveryBatch;
import com.app.models.Order;
import com.app.utility.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the DeliveryBatchDAO interface handling delivery batch lifecycles,
 * order batch assignments, and concurrency-controlled batching queries.
 */
public class DeliveryBatchDAOImpl implements DeliveryBatchDAO {

    private static final String INSERT_BATCH =
        "INSERT INTO delivery_batches (restaurant_id, delivery_zone, status, max_size) VALUES (?, ?, ?, ?)";

    private static final String SELECT_BATCH_BY_ID =
        "SELECT * FROM delivery_batches WHERE batch_id = ?";

    private static final String SELECT_ALL_BATCHES =
        "SELECT * FROM delivery_batches ORDER BY created_at DESC LIMIT ?";

    private static final String SELECT_BATCHES_BY_STATUS =
        "SELECT * FROM delivery_batches WHERE status = ? ORDER BY created_at DESC";

    private static final String UPDATE_BATCH_STATUS =
        "UPDATE delivery_batches SET status = ?, " +
        "dispatched_at = CASE WHEN ? = 'dispatched' AND dispatched_at IS NULL THEN CURRENT_TIMESTAMP ELSE dispatched_at END, " +
        "updated_at = CURRENT_TIMESTAMP WHERE batch_id = ?";

    private static final String SELECT_ORDER_FOR_UPDATE =
        "SELECT batch_id FROM orders WHERE order_id = ? FOR UPDATE";

    private static final String INSERT_BATCH_ASSIGNMENT =
        "INSERT INTO batch_assignments (batch_id, order_id) VALUES (?, ?)";

    private static final String UPDATE_ORDER_BATCH =
        "UPDATE orders SET batch_id = ?, updated_at = CURRENT_TIMESTAMP WHERE order_id = ?";

    private static final String SELECT_ASSIGNMENTS_BY_BATCH =
        "SELECT * FROM batch_assignments WHERE batch_id = ? ORDER BY assigned_at";

    private static final String SELECT_ORDERS_IN_BATCH =
        "SELECT o.* FROM orders o JOIN batch_assignments ba ON o.order_id = ba.order_id WHERE ba.batch_id = ? ORDER BY ba.assigned_at";

    private static final String SELECT_ORDERS_ELIGIBLE_FOR_BATCHING =
        "SELECT o.* FROM orders o LEFT JOIN batch_assignments ba ON o.order_id = ba.order_id " +
        "WHERE o.status = 'ready_for_pickup' AND ba.order_id IS NULL FOR UPDATE SKIP LOCKED";

    private static final String COUNT_ASSIGNMENTS =
        "SELECT COUNT(*) FROM batch_assignments WHERE batch_id = ?";

    /**
     * Creates a new delivery batch using the provided transaction connection.
     *
     * @param batch the DeliveryBatch entity to persist
     * @param conn  active Connection instance
     * @return generated batch_id, or -1 on failure
     * @throws SQLException if a database access error occurs
     */
    @Override
    public int createBatch(DeliveryBatch batch, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_BATCH, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, batch.getRestaurantId());
            ps.setString(2, batch.getDeliveryZone());
            ps.setString(3, batch.getStatus() != null ? batch.getStatus() : "open");
            ps.setInt(4, batch.getMaxSize() > 0 ? batch.getMaxSize() : 5);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Retrieves a delivery batch record by its primary key ID.
     *
     * @param batchId unique identifier of the batch
     * @return DeliveryBatch object if found, null otherwise
     */
    @Override
    public DeliveryBatch getBatchById(int batchId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BATCH_BY_ID)) {
            ps.setInt(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBatch(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryBatchDAO] getBatchById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a list of delivery batches ordered by creation time descending, limited by count.
     *
     * @param limit maximum number of batches to retrieve
     * @return list of DeliveryBatch objects
     */
    @Override
    public List<DeliveryBatch> getAllBatches(int limit) {
        List<DeliveryBatch> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_BATCHES)) {
            ps.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryBatchDAO] getAllBatches error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves delivery batches filtered by their lifecycle status.
     *
     * @param status status filter string
     * @return list of DeliveryBatch objects
     */
    @Override
    public List<DeliveryBatch> getBatchesByStatus(String status) {
        List<DeliveryBatch> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BATCHES_BY_STATUS)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBatch(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryBatchDAO] getBatchesByStatus error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates the status of an existing delivery batch and sets dispatched_at if transitioning to dispatched.
     *
     * @param batchId unique identifier of the batch
     * @param status  new status string
     */
    @Override
    public void updateBatchStatus(int batchId, String status) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_BATCH_STATUS)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, batchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DeliveryBatchDAO] updateBatchStatus error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Assigns an order to a batch atomically using SELECT...FOR UPDATE.
     * Returns true if assignment succeeded; false if order is already assigned.
     *
     * @param batchId unique identifier of the target batch
     * @param orderId unique identifier of the order to assign
     * @param conn    active Connection instance within a transaction
     * @return true if successfully assigned, false if order already has an assigned batch
     * @throws SQLException if a database access error occurs
     */
    @Override
    public boolean assignOrderToBatch(int batchId, int orderId, Connection conn) throws SQLException {
        // Lock the order row and verify it has not already been assigned to a batch
        try (PreparedStatement checkStmt = conn.prepareStatement(SELECT_ORDER_FOR_UPDATE)) {
            checkStmt.setInt(1, orderId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // Order does not exist
                }
                int existingBatchId = rs.getInt("batch_id");
                if (!rs.wasNull() && existingBatchId > 0) {
                    return false; // Order already assigned to a batch
                }
            }
        }

        try {
            // Insert assignment record and update orders.batch_id
            try (PreparedStatement insertStmt = conn.prepareStatement(INSERT_BATCH_ASSIGNMENT);
                 PreparedStatement updateStmt = conn.prepareStatement(UPDATE_ORDER_BATCH)) {

                insertStmt.setInt(1, batchId);
                insertStmt.setInt(2, orderId);
                insertStmt.executeUpdate();

                updateStmt.setInt(1, batchId);
                updateStmt.setInt(2, orderId);
                updateStmt.executeUpdate();
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            // Unique constraint uq_order_assignment prevented duplicate assignment
            return false;
        }
    }

    /**
     * Retrieves all batch assignment records associated with a specific batch.
     *
     * @param batchId unique identifier of the batch
     * @return list of BatchAssignment objects
     */
    @Override
    public List<BatchAssignment> getAssignmentsForBatch(int batchId) {
        List<BatchAssignment> assignments = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ASSIGNMENTS_BY_BATCH)) {
            ps.setInt(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    assignments.add(mapAssignment(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryBatchDAO] getAssignmentsForBatch error: " + e.getMessage());
            e.printStackTrace();
        }
        return assignments;
    }

    /**
     * Retrieves all full Order entities grouped in the specified delivery batch.
     *
     * @param batchId unique identifier of the batch
     * @return list of Order objects in the batch
     */
    @Override
    public List<Order> getOrdersInBatch(int batchId) {
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ORDERS_IN_BATCH)) {
            ps.setInt(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryBatchDAO] getOrdersInBatch error: " + e.getMessage());
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Returns orders that are ready_for_pickup and not yet assigned to any batch.
     * Uses SELECT ... FOR UPDATE SKIP LOCKED to lock rows and prevent concurrent
     * batching workers from blocking or interfering with each other.
     *
     * @param conn active Connection instance provided by the transaction caller
     * @return list of eligible locked Order objects
     * @throws SQLException if a database access error occurs
     */
    @Override
    public List<Order> getOrdersEligibleForBatching(Connection conn) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ORDERS_ELIGIBLE_FOR_BATCHING);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        }
        return list;
    }

    /**
     * Returns the total number of orders assigned to a given delivery batch.
     *
     * @param batchId unique identifier of the batch
     * @return count of assigned orders
     */
    @Override
    public int getAssignmentCount(int batchId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_ASSIGNMENTS)) {
            ps.setInt(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DeliveryBatchDAO] getAssignmentCount error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Maps a ResultSet row to a DeliveryBatch entity.
     *
     * @param rs active ResultSet positioned at the desired row
     * @return populated DeliveryBatch instance
     * @throws SQLException if a column cannot be read
     */
    private DeliveryBatch mapBatch(ResultSet rs) throws SQLException {
        DeliveryBatch batch = new DeliveryBatch();
        batch.setBatchId(rs.getInt("batch_id"));
        batch.setRestaurantId(rs.getInt("restaurant_id"));
        batch.setDeliveryZone(rs.getString("delivery_zone"));
        batch.setStatus(rs.getString("status"));
        batch.setMaxSize(rs.getInt("max_size"));
        batch.setDispatchedAt(rs.getTimestamp("dispatched_at"));
        batch.setCreatedAt(rs.getTimestamp("created_at"));
        batch.setUpdatedAt(rs.getTimestamp("updated_at"));
        return batch;
    }

    /**
     * Maps a ResultSet row to a BatchAssignment entity.
     *
     * @param rs active ResultSet positioned at the desired row
     * @return populated BatchAssignment instance
     * @throws SQLException if a column cannot be read
     */
    private BatchAssignment mapAssignment(ResultSet rs) throws SQLException {
        BatchAssignment ba = new BatchAssignment();
        ba.setAssignmentId(rs.getInt("assignment_id"));
        ba.setBatchId(rs.getInt("batch_id"));
        ba.setOrderId(rs.getInt("order_id"));
        ba.setAssignedAt(rs.getTimestamp("assigned_at"));
        return ba;
    }

    /**
     * Maps a ResultSet row to an Order entity.
     *
     * @param rs active ResultSet positioned at the desired row
     * @return populated Order instance
     * @throws SQLException if a column cannot be read
     */
    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setUserId(rs.getInt("user_id"));
        order.setRestaurantId(rs.getInt("restaurant_id"));
        int addressId = rs.getInt("address_id");
        order.setAddressId(rs.wasNull() ? null : addressId);
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setDeliveryInstructions(rs.getString("delivery_instructions"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        order.setUpdatedAt(rs.getTimestamp("updated_at"));
        return order;
    }
}
