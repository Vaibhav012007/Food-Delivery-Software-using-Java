package com.app.service;

import com.app.models.SurplusListing;
import com.app.models.SurplusReservation;
import java.sql.Timestamp;
import java.util.List;

/**
 * Service interface for Surplus Food Marketplace.
 * Allows restaurants to list excess food at discounted prices and customers to reserve it.
 */
public interface SurplusService {
    int createListing(int restaurantId, int menuItemId, int quantity,
                      double originalPrice, double discountedPrice, Timestamp expiryTime);

    SurplusListing getListingById(int listingId);
    List<SurplusListing> getActiveListings();
    List<SurplusListing> getListingsByRestaurant(int restaurantId);

    /**
     * Reserves surplus items. Atomic stock decrement prevents overselling.
     * @return reservation ID
     * @throws com.app.exceptions.InsufficientStockException if stock is insufficient
     */
    int reserveSurplus(int listingId, int userId, int quantity);

    void cancelReservation(int reservationId, int userId);

    List<SurplusReservation> getMyReservations(int userId);

    /** Called by scheduler to expire overdue listings. Returns count of expired items. */
    int expireListings();
}
