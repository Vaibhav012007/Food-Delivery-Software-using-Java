package com.app.models;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Represents a group order where multiple users can contribute items before checkout.
 * Mapped to the 'group_orders' table.
 */
public class GroupOrder {

    private int groupOrderId;
    private int creatorId;
    private int restaurantId;
    private String inviteCode;
    private Timestamp deadline;
    private String status; // 'open', 'closed', 'checked_out', 'expired', 'failed'
    private Integer resultOrderId; // nullable
    private String checkoutIdempotencyKey; // nullable
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Default constructor.
     */
    public GroupOrder() {
    }

    /**
     * Parameterized constructor initializing all fields.
     *
     * @param groupOrderId           unique identifier for the group order
     * @param creatorId              user ID of the creator/host
     * @param restaurantId           restaurant ID for the order
     * @param inviteCode             shareable invite code for joining
     * @param deadline               expiration deadline timestamp for adding items
     * @param status                 order status ('open', 'closed', 'checked_out', 'expired', 'failed')
     * @param resultOrderId          resulting finalized order ID once checked out (nullable)
     * @param checkoutIdempotencyKey key to prevent duplicate checkout submissions (nullable)
     * @param createdAt              record creation timestamp
     * @param updatedAt              record last update timestamp
     */
    public GroupOrder(int groupOrderId, int creatorId, int restaurantId, String inviteCode,
                      Timestamp deadline, String status, Integer resultOrderId,
                      String checkoutIdempotencyKey, Timestamp createdAt, Timestamp updatedAt) {
        this.groupOrderId = groupOrderId;
        this.creatorId = creatorId;
        this.restaurantId = restaurantId;
        this.inviteCode = inviteCode;
        this.deadline = deadline;
        this.status = status;
        this.resultOrderId = resultOrderId;
        this.checkoutIdempotencyKey = checkoutIdempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Convenience method to check if the group order is open for participation.
     *
     * @return true if status is 'open' and deadline has not passed; false otherwise
     */
    public boolean isOpen() {
        return "open".equals(status) && deadline != null && deadline.after(new Date());
    }

    public int getGroupOrderId() {
        return groupOrderId;
    }

    public void setGroupOrderId(int groupOrderId) {
        this.groupOrderId = groupOrderId;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getResultOrderId() {
        return resultOrderId;
    }

    public void setResultOrderId(Integer resultOrderId) {
        this.resultOrderId = resultOrderId;
    }

    public String getCheckoutIdempotencyKey() {
        return checkoutIdempotencyKey;
    }

    public void setCheckoutIdempotencyKey(String checkoutIdempotencyKey) {
        this.checkoutIdempotencyKey = checkoutIdempotencyKey;
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
        return "GroupOrder [groupOrderId=" + groupOrderId + ", creatorId=" + creatorId
                + ", restaurantId=" + restaurantId + ", inviteCode=" + inviteCode
                + ", deadline=" + deadline + ", status=" + status
                + ", resultOrderId=" + resultOrderId + ", checkoutIdempotencyKey=" + checkoutIdempotencyKey
                + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
