package com.app.models;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Represents a surplus food listing offered at a discount to reduce food waste.
 * Mapped to the 'surplus_listings' table.
 */
public class SurplusListing {

    private int listingId;
    private int restaurantId;
    private int menuItemId;
    private double originalPrice;
    private double discountedPrice;
    private int availableQuantity;
    private Timestamp expiryTime;
    private String status; // 'active', 'expired', 'sold_out', 'cancelled'
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Default constructor.
     */
    public SurplusListing() {
    }

    /**
     * Parameterized constructor initializing all fields.
     *
     * @param listingId         unique identifier for the surplus listing
     * @param restaurantId      restaurant offering the surplus item
     * @param menuItemId        menu item reference
     * @param originalPrice     regular price of the item
     * @param discountedPrice   discounted surplus price
     * @param availableQuantity number of units available
     * @param expiryTime        timestamp when the listing expires
     * @param status            status ('active', 'expired', 'sold_out', 'cancelled')
     * @param createdAt         timestamp when listing was created
     * @param updatedAt         timestamp when listing was last updated
     */
    public SurplusListing(int listingId, int restaurantId, int menuItemId, double originalPrice,
                          double discountedPrice, int availableQuantity, Timestamp expiryTime,
                          String status, Timestamp createdAt, Timestamp updatedAt) {
        this.listingId = listingId;
        this.restaurantId = restaurantId;
        this.menuItemId = menuItemId;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.availableQuantity = availableQuantity;
        this.expiryTime = expiryTime;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Checks if the surplus listing is currently active and within its expiry time.
     *
     * @return true if status is 'active' and expiry time is in the future; false otherwise
     */
    public boolean isActive() {
        return "active".equals(status) && expiryTime != null && expiryTime.after(new Date());
    }

    /**
     * Calculates the discount percentage based on original and discounted prices.
     *
     * @return discount percentage (0 to 100)
     */
    public double getDiscountPercentage() {
        return originalPrice > 0 ? (1.0 - (discountedPrice / originalPrice)) * 100.0 : 0.0;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(int menuItemId) {
        this.menuItemId = menuItemId;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Timestamp getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Timestamp expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return "SurplusListing [listingId=" + listingId + ", restaurantId=" + restaurantId
                + ", menuItemId=" + menuItemId + ", originalPrice=" + originalPrice
                + ", discountedPrice=" + discountedPrice + ", availableQuantity=" + availableQuantity
                + ", expiryTime=" + expiryTime + ", status=" + status
                + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}
