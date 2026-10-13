package com.app.dao;

import com.app.models.SurplusListing;
import com.app.models.SurplusReservation;
import java.util.List;

/**
 * Data Access Object (DAO) interface defining database operations for
 * surplus food listings, stock decrement/restoration, automated expiration,
 * and customer surplus reservations.
 */
public interface SurplusListingDAO {

    /**
     * Creates a new surplus food listing and returns the generated primary key ID.
     *
     * @param listing details of the surplus listing to create
     * @return generated listing_id, or -1 on failure
     */
    int createListing(SurplusListing listing);

    /**
     * Retrieves a surplus listing by its primary key ID.
     *
     * @param listingId unique identifier of the surplus listing
     * @return SurplusListing if found, null otherwise
     */
    SurplusListing getListingById(int listingId);

    /**
     * Retrieves all active surplus listings that have not expired and have remaining stock.
     *
     * @return list of active SurplusListing objects
     */
    List<SurplusListing> getActiveListings();

    /**
     * Retrieves all surplus listings created by a specific restaurant.
     *
     * @param restaurantId unique identifier of the restaurant
     * @return list of SurplusListing objects for the restaurant
     */
    List<SurplusListing> getListingsByRestaurant(int restaurantId);

    /**
     * Updates the status of a surplus listing (e.g., 'active', 'expired', 'sold_out', 'cancelled').
     *
     * @param listingId unique identifier of the surplus listing
     * @param status    new status string
     */
    void updateListingStatus(int listingId, String status);

    /**
     * Atomically decrements stock. Returns true if decrement succeeded
     * (stock was sufficient). Uses conditional UPDATE to prevent race conditions.
     * UPDATE surplus_listings SET available_quantity = available_quantity - ?
     * WHERE listing_id = ? AND available_quantity >= ? AND status = 'active' AND expiry_time > NOW()
     *
     * @param listingId unique identifier of the surplus listing
     * @param quantity  quantity to deduct
     * @return true if stock was sufficient and successfully decremented, false otherwise
     */
    boolean decrementStock(int listingId, int quantity);

    /**
     * Restores stock on reservation cancellation.
     *
     * @param listingId unique identifier of the surplus listing
     * @param quantity  quantity to restore
     */
    void incrementStock(int listingId, int quantity);

    /**
     * Expires listings past their expiry_time. Returns count of expired.
     *
     * @return number of listings transitioned to 'expired'
     */
    int expireOverdueListings();

    // Reservation methods

    /**
     * Creates a new reservation for surplus food and returns the generated reservation ID.
     *
     * @param reservation details of the surplus reservation
     * @return generated reservation_id, or -1 on failure
     */
    int createReservation(SurplusReservation reservation);

    /**
     * Retrieves a surplus reservation by its primary key ID.
     *
     * @param reservationId unique identifier of the reservation
     * @return SurplusReservation if found, null otherwise
     */
    SurplusReservation getReservationById(int reservationId);

    /**
     * Retrieves all surplus reservations placed by a specific user.
     *
     * @param userId unique identifier of the user
     * @return list of SurplusReservation objects for the user
     */
    List<SurplusReservation> getReservationsByUser(int userId);

    /**
     * Retrieves all surplus reservations made for a specific listing.
     *
     * @param listingId unique identifier of the surplus listing
     * @return list of SurplusReservation objects for the listing
     */
    List<SurplusReservation> getReservationsByListing(int listingId);

    /**
     * Updates the status of a surplus reservation ('reserved', 'collected', 'cancelled', 'expired').
     *
     * @param reservationId unique identifier of the reservation
     * @param status        new status string
     */
    void updateReservationStatus(int reservationId, String status);
}
