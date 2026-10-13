package com.app.dao;

import com.app.models.BatchAssignment;
import com.app.models.DeliveryBatch;
import com.app.models.Order;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object (DAO) interface defining database operations for
 * delivery batches, order grouping/assignments, and concurrent batching queries.
 */
public interface DeliveryBatchDAO {

    /**
     * Creates a new delivery batch record within an active database transaction.
     *
     * @param batch the DeliveryBatch entity containing batch details
     * @param conn  active Connection instance provided by the transaction caller
     * @return generated batch_id, or -1 on failure
     * @throws SQLException if a database access error occurs
     */
    int createBatch(DeliveryBatch batch, Connection conn) throws SQLException;

    /**
     * Retrieves a delivery batch by its unique primary key ID.
     *
     * @param batchId unique identifier of the batch
     * @return DeliveryBatch if found, null otherwise
     */
    DeliveryBatch getBatchById(int batchId);

    /**
     * Retrieves all delivery batches ordered by creation timestamp descending, limited by count.
     *
     * @param limit maximum number of batches to retrieve
     * @return list of DeliveryBatch objects
     */
    List<DeliveryBatch> getAllBatches(int limit);

    /**
     * Retrieves all delivery batches matching a given status ('open', 'dispatched', 'delivered', 'cancelled').
     *
     * @param status status filter string
     * @return list of DeliveryBatch objects matching the specified status
     */
    List<DeliveryBatch> getBatchesByStatus(String status);

    /**
     * Updates the status of an existing delivery batch.
     *
     * @param batchId unique identifier of the batch
     * @param status  new status string
     */
    void updateBatchStatus(int batchId, String status);

    /**
     * Assigns an order to a batch atomically using SELECT...FOR UPDATE.
     * Returns true if assignment succeeded; false if order already assigned.
     *
     * @param batchId unique identifier of the target batch
     * @param orderId unique identifier of the order to assign
     * @param conn    active Connection instance provided by the transaction caller
     * @return true if assignment succeeded; false if order is already assigned
     * @throws SQLException if a database access error occurs
     */
    boolean assignOrderToBatch(int batchId, int orderId, Connection conn) throws SQLException;

    /**
     * Retrieves all batch assignment records for a given batch ID.
     *
     * @param batchId unique identifier of the batch
     * @return list of BatchAssignment records
     */
    List<BatchAssignment> getAssignmentsForBatch(int batchId);

    /**
     * Retrieves full Order details for all orders currently assigned to a given batch.
     *
     * @param batchId unique identifier of the batch
     * @return list of Order objects belonging to this batch
     */
    List<Order> getOrdersInBatch(int batchId);

    /**
     * Returns orders that are READY_FOR_PICKUP and not yet in any batch.
     * Uses SELECT ... FOR UPDATE to lock rows during batching cycle.
     *
     * @param conn active Connection instance provided by the transaction caller
     * @return list of eligible Order objects
     * @throws SQLException if a database access error occurs
     */
    List<Order> getOrdersEligibleForBatching(Connection conn) throws SQLException;

    /**
     * Returns the total count of orders assigned to a given batch.
     *
     * @param batchId unique identifier of the batch
     * @return number of orders currently in the batch
     */
    int getAssignmentCount(int batchId);
}
