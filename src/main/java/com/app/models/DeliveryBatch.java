package com.app.models;

import java.sql.Timestamp;

/**
 * Represents a delivery batch grouping orders together for batched fulfillment.
 * Mapped to the 'delivery_batches' table.
 */
public class DeliveryBatch {

    private int batchId;
    private int restaurantId;
    private String deliveryZone; // nullable
    private String status; // 'open', 'dispatched', 'delivered', 'cancelled'
    private int maxSize;
    private Timestamp dispatchedAt; // nullable
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Default constructor.
     */
    public DeliveryBatch() {
    }

    /**
     * Parameterized constructor initializing all fields.
     *
     * @param batchId      unique identifier for the batch
     * @param restaurantId restaurant originating the delivery
     * @param deliveryZone delivery zone/neighborhood (nullable)
     * @param status       batch status ('open', 'dispatched', 'delivered', 'cancelled')
     * @param maxSize      maximum number of orders allowed in this batch
     * @param dispatchedAt timestamp when batch was dispatched (nullable)
     * @param createdAt    timestamp when batch was created
     * @param updatedAt    timestamp when batch was last updated
     */
    public DeliveryBatch(int batchId, int restaurantId, String deliveryZone, String status,
                         int maxSize, Timestamp dispatchedAt, Timestamp createdAt, Timestamp updatedAt) {
        this.batchId = batchId;
        this.restaurantId = restaurantId;
        this.deliveryZone = deliveryZone;
        this.status = status;
        this.maxSize = maxSize;
        this.dispatchedAt = dispatchedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getDeliveryZone() {
        return deliveryZone;
    }

    public void setDeliveryZone(String deliveryZone) {
        this.deliveryZone = deliveryZone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public Timestamp getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Timestamp dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "DeliveryBatch [batchId=" + batchId + ", restaurantId=" + restaurantId
                + ", deliveryZone=" + deliveryZone + ", status=" + status
                + ", maxSize=" + maxSize + ", dispatchedAt=" + dispatchedAt
                + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
