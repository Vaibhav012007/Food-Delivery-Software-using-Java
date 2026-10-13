package com.app.service;

import com.app.dao.SurplusListingDAO;
import com.app.dao_implementation.SurplusListingDAOImpl;
import com.app.exceptions.AppException;
import com.app.exceptions.InsufficientStockException;
import com.app.exceptions.UnauthorizedAccessException;
import com.app.models.SurplusListing;
import com.app.models.SurplusReservation;

import java.sql.Timestamp;
import java.util.List;

/**
 * Implementation of SurplusService.
 * Handles excess food inventory listings, atomic reservations, and lifecycle management.
 */
public class SurplusServiceImpl implements SurplusService {

    private final SurplusListingDAO dao = new SurplusListingDAOImpl();

    @Override
    public int createListing(int restaurantId, int menuItemId, int quantity,
                             double originalPrice, double discountedPrice, Timestamp expiryTime) {
        if (discountedPrice >= originalPrice) {
            throw new AppException("Discounted price must be less than original price");
        }
        if (quantity <= 0) {
            throw new AppException("Quantity must be positive");
        }
        if (expiryTime.before(new java.util.Date())) {
            throw new AppException("Expiry time must be in the future");
        }
        SurplusListing listing = new SurplusListing();
        listing.setRestaurantId(restaurantId);
        listing.setMenuItemId(menuItemId);
        listing.setAvailableQuantity(quantity);
        listing.setOriginalPrice(originalPrice);
        listing.setDiscountedPrice(discountedPrice);
        listing.setExpiryTime(expiryTime);
        listing.setStatus("active");
        int id = dao.createListing(listing);
        System.out.println("[SurplusService] Listing created: id=" + id);
        return id;
    }

    @Override
    public SurplusListing getListingById(int listingId) {
        return dao.getListingById(listingId);
    }

    @Override
    public List<SurplusListing> getActiveListings() {
        return dao.getActiveListings();
    }

    @Override
    public List<SurplusListing> getListingsByRestaurant(int restaurantId) {
        return dao.getListingsByRestaurant(restaurantId);
    }

    @Override
    public int reserveSurplus(int listingId, int userId, int quantity) {
        SurplusListing listing = dao.getListingById(listingId);
        if (listing == null) {
            throw new AppException("Surplus listing not found: " + listingId);
        }
        if (!listing.isActive()) {
            throw new AppException("Surplus listing is not active or has expired");
        }
        if (quantity <= 0) {
            throw new AppException("Quantity must be positive");
        }

        // Atomic decrement - prevents overselling in concurrent scenarios
        boolean decremented = dao.decrementStock(listingId, quantity);
        if (!decremented) {
            SurplusListing fresh = dao.getListingById(listingId);
            int available = fresh != null ? fresh.getAvailableQuantity() : 0;
            throw new InsufficientStockException(listing.getMenuItemId(), quantity, available);
        }

        // Create reservation (30-minute collection window)
        SurplusReservation reservation = new SurplusReservation();
        reservation.setListingId(listingId);
        reservation.setUserId(userId);
        reservation.setQuantity(quantity);
        reservation.setTotalPrice(listing.getDiscountedPrice() * quantity);
        reservation.setStatus("reserved");
        reservation.setExpiresAt(new Timestamp(System.currentTimeMillis() + 30L * 60 * 1000));

        int reservationId = dao.createReservation(reservation);
        System.out.println("[SurplusService] Reservation created: id=" + reservationId + ", listing=" + listingId + ", qty=" + quantity);

        SurplusListing updated = dao.getListingById(listingId);
        if (updated != null && updated.getAvailableQuantity() <= 0) {
            dao.updateListingStatus(listingId, "sold_out");
        }

        return reservationId;
    }

    @Override
    public void cancelReservation(int reservationId, int userId) {
        SurplusReservation reservation = dao.getReservationById(reservationId);
        if (reservation == null) {
            throw new AppException("Reservation not found: " + reservationId);
        }
        if (reservation.getUserId() != userId) {
            throw new UnauthorizedAccessException(userId, "cancel reservation " + reservationId);
        }
        if (!"reserved".equals(reservation.getStatus())) {
            throw new AppException("Cannot cancel reservation in status: " + reservation.getStatus());
        }
        dao.incrementStock(reservation.getListingId(), reservation.getQuantity());
        dao.updateReservationStatus(reservationId, "cancelled");

        SurplusListing listing = dao.getListingById(reservation.getListingId());
        if (listing != null && "sold_out".equals(listing.getStatus()) && listing.getExpiryTime().after(new java.util.Date())) {
            dao.updateListingStatus(reservation.getListingId(), "active");
        }
        System.out.println("[SurplusService] Reservation " + reservationId + " cancelled by user " + userId);
    }

    @Override
    public List<SurplusReservation> getMyReservations(int userId) {
        return dao.getReservationsByUser(userId);
    }

    @Override
    public int expireListings() {
        int count = dao.expireOverdueListings();
        return count;
    }
}
