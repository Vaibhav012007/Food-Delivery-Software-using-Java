package com.app.models;

import java.sql.Timestamp;

/**
 * Represents an item added by a member into a group order's shared cart.
 * Mapped to the 'group_cart_items' table.
 */
public class GroupCartItem {

    private int itemId;
    private int groupOrderId;
    private int userId; // which member added this
    private int menuItemId;
    private int quantity;
    private Timestamp addedAt;
    private Timestamp updatedAt;

    /**
     * Default constructor.
     */
    public GroupCartItem() {
    }

    /**
     * Parameterized constructor.
     *
     * @param itemId       unique identifier for this group cart item
     * @param groupOrderId group order reference
     * @param userId       user identifier of member who added this item
     * @param menuItemId   menu item reference
     * @param quantity     quantity ordered
     * @param addedAt      timestamp when item was added
     * @param updatedAt      timestamp when item was last updated
     */
    public GroupCartItem(int itemId, int groupOrderId, int userId, int menuItemId,
                         int quantity, Timestamp addedAt, Timestamp updatedAt) {
        this.itemId = itemId;
        this.groupOrderId = groupOrderId;
        this.userId = userId;
        this.menuItemId = menuItemId;
        this.quantity = quantity;
        this.addedAt = addedAt;
        this.updatedAt = updatedAt;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getGroupOrderId() {
        return groupOrderId;
    }

    public void setGroupOrderId(int groupOrderId) {
        this.groupOrderId = groupOrderId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(int menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Timestamp getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Timestamp addedAt) {
        this.addedAt = addedAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "GroupCartItem [itemId=" + itemId + ", groupOrderId=" + groupOrderId
                + ", userId=" + userId + ", menuItemId=" + menuItemId
                + ", quantity=" + quantity + ", addedAt=" + addedAt
                + ", updatedAt=" + updatedAt + "]";
    }
}
