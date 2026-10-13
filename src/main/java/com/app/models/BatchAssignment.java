package com.app.models;

import java.sql.Timestamp;

/**
 * Represents the assignment of an order to a delivery batch.
 * Mapped to the 'batch_assignments' table.
 */
public class BatchAssignment {

    private int assignmentId;
    private int batchId;
    private int orderId;
    private Timestamp assignedAt;

    /**
     * Default constructor.
     */
    public BatchAssignment() {
    }

    /**
     * Parameterized constructor.
     *
     * @param assignmentId unique identifier for the assignment
     * @param batchId      delivery batch reference
     * @param orderId      order reference
     * @param assignedAt   timestamp when the order was assigned to the batch
     */
    public BatchAssignment(int assignmentId, int batchId, int orderId, Timestamp assignedAt) {
        this.assignmentId = assignmentId;
        this.batchId = batchId;
        this.orderId = orderId;
        this.assignedAt = assignedAt;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Timestamp getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Timestamp assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Override
    public String toString() {
        return "BatchAssignment [assignmentId=" + assignmentId + ", batchId=" + batchId
                + ", orderId=" + orderId + ", assignedAt=" + assignedAt + "]";
    }
}
