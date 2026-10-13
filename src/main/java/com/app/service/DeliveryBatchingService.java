package com.app.service;

import com.app.dao.DeliveryBatchDAO;
import com.app.dao_implementation.DeliveryBatchDAOImpl;
import com.app.models.DeliveryBatch;
import com.app.models.Order;
import com.app.utility.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Intelligent Delivery Batching Service.
 *
 * Algorithm:
 * 1. Fetch orders with status 'ready_for_pickup' not yet in any batch (using FOR UPDATE SKIP LOCKED).
 * 2. Group by restaurant_id.
 * 3. Within each restaurant group, batch up to MAX_BATCH_SIZE orders.
 * 4. Create a DeliveryBatch record and assign orders to it.
 * 5. Return total orders batched in this cycle.
 *
 * Safety:
 * - FOR UPDATE SKIP LOCKED: two competing workers won't batch the same order.
 * - UNIQUE constraint on batch_assignments.order_id prevents double-assignment at the DB level.
 * - Each batching cycle runs in its own transaction; failure rolls back cleanly.
 */
public class DeliveryBatchingService {

    private static final int MAX_BATCH_SIZE = 5;
    private final DeliveryBatchDAO batchDAO = new DeliveryBatchDAOImpl();

    /**
     * Runs one batching cycle. Returns the number of orders batched.
     */
    public int runBatchingCycle() {
        int totalBatched = 0;
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Fetch eligible orders (locks them for this transaction)
            List<Order> eligible = batchDAO.getOrdersEligibleForBatching(conn);
            if (eligible.isEmpty()) {
                conn.rollback();
                return 0;
            }

            // Group by restaurantId
            Map<Integer, List<Order>> byRestaurant = new LinkedHashMap<>();
            for (Order o : eligible) {
                List<Order> list = byRestaurant.get(o.getRestaurantId());
                if (list == null) {
                    list = new ArrayList<>();
                    byRestaurant.put(o.getRestaurantId(), list);
                }
                list.add(o);
            }

            // Create batches
            for (Map.Entry<Integer, List<Order>> entry : byRestaurant.entrySet()) {
                int restaurantId = entry.getKey();
                List<Order> orders = entry.getValue();

                // Split into batches of MAX_BATCH_SIZE
                for (int i = 0; i < orders.size(); i += MAX_BATCH_SIZE) {
                    List<Order> batchOrders = orders.subList(i, Math.min(i + MAX_BATCH_SIZE, orders.size()));

                    DeliveryBatch batch = new DeliveryBatch();
                    batch.setRestaurantId(restaurantId);
                    batch.setStatus("open");
                    batch.setMaxSize(MAX_BATCH_SIZE);

                    int batchId = batchDAO.createBatch(batch, conn);
                    if (batchId <= 0) continue;

                    for (Order order : batchOrders) {
                        boolean assigned = batchDAO.assignOrderToBatch(batchId, order.getOrderId(), conn);
                        if (assigned) totalBatched++;
                    }
                }
            }

            conn.commit();
            System.out.println("[DeliveryBatching] Cycle complete. Orders assigned to batches: " + totalBatched);

        } catch (SQLException e) {
            System.err.println("[DeliveryBatching] Error in batching cycle: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rb) { rb.printStackTrace(); }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return totalBatched;
    }

    /**
     * Returns all batches for display in the dashboard.
     */
    public List<DeliveryBatch> getAllBatches(int limit) {
        return batchDAO.getAllBatches(limit);
    }

    public List<DeliveryBatch> getBatchesByStatus(String status) {
        return batchDAO.getBatchesByStatus(status);
    }

    public void dispatchBatch(int batchId) {
        batchDAO.updateBatchStatus(batchId, "dispatched");
    }
}
