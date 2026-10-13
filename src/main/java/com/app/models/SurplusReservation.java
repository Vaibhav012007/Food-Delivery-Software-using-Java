package com.app.models;

import java.sql.Timestamp;

/**
 * Represents a user reservation for a surplus food listing.
 * Mapped to the 'surplus_reservations' table.
 */
public class SurplusReservation {

    private int reservationId;
    private int listingId;
    private int userId;
    private int quantity;
    private double totalPrice;
    private String status; // 'reserved', 'collected', 'cancelled', 'expired'
    private Timestamp reservedAt;
    private Timestamp expiresAt;

    /**
     * Default constructor.
     */
    public SurplusReservation() {
    }

    /**
     * Parameterized constructor initializing all fields.
     *
     * @param reservationId unique identifier for the reservation
     * @param listingId     surplus listing reference
     * @param userId        user making the reservation
     * @param quantity      quantity reserved
     * @param totalPrice    total discounted price for the reservation
     * @param status        reservation status ('reserved', 'collected', 'cancelled', 'expired')
     * @param reservedAt    timestamp when reservation was created
     * @param expiresAt     timestamp when reservation expires if not collected
     */
    public SurplusReservation(int reservationId, int listingId, int userId, int quantity,
                              double totalPrice, String status, Timestamp reservedAt, Timestamp expiresAt) {
        this.reservationId = reservationId;
        this.listingId = listingId;
        this.userId = userId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public int getListingId() {
        return listingId;
    }

    public void setListingId(int listingId) {
        this.listingId = listingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Timestamp reservedAt) {
        this.reservedAt = reservedAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "SurplusReservation [reservationId=" + reservationId + ", listingId=" + listingId
                + ", userId=" + userId + ", quantity=" + quantity + ", totalPrice=" + totalPrice
                + ", status=" + status + ", reservedAt=" + reservedAt
                + ", expiresAt=" + expiresAt + "]";
    }
}
