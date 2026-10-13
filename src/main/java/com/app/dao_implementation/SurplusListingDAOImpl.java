package com.app.dao_implementation;

import com.app.dao.SurplusListingDAO;
import com.app.models.SurplusListing;
import com.app.models.SurplusReservation;
import com.app.utility.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the SurplusListingDAO interface providing complete database access
 * for surplus food listings, atomic stock deductions, automatic expiry, and reservations.
 */
public class SurplusListingDAOImpl implements SurplusListingDAO {

    private static final String INSERT_LISTING =
        "INSERT INTO surplus_listings (restaurant_id, menu_item_id, original_price, discounted_price, " +
        "available_quantity, expiry_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_LISTING_BY_ID =
        "SELECT * FROM surplus_listings WHERE listing_id = ?";

    private static final String SELECT_ACTIVE_LISTINGS =
        "SELECT * FROM surplus_listings WHERE status = 'active' AND expiry_time > NOW() AND available_quantity > 0 " +
        "ORDER BY expiry_time ASC";

    private static final String SELECT_LISTINGS_BY_RESTAURANT =
        "SELECT * FROM surplus_listings WHERE restaurant_id = ? ORDER BY created_at DESC";

    private static final String UPDATE_LISTING_STATUS =
        "UPDATE surplus_listings SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE listing_id = ?";

    private static final String DECREMENT_STOCK =
        "UPDATE surplus_listings SET available_quantity = available_quantity - ?, updated_at = CURRENT_TIMESTAMP " +
        "WHERE listing_id = ? AND available_quantity >= ? AND status = 'active' AND expiry_time > NOW()";

    private static final String INCREMENT_STOCK =
        "UPDATE surplus_listings SET available_quantity = available_quantity + ?, updated_at = CURRENT_TIMESTAMP " +
        "WHERE listing_id = ?";

    private static final String EXPIRE_OVERDUE =
        "UPDATE surplus_listings SET status = 'expired', updated_at = CURRENT_TIMESTAMP " +
        "WHERE status = 'active' AND expiry_time <= NOW()";

    private static final String INSERT_RESERVATION =
        "INSERT INTO surplus_reservations (listing_id, user_id, quantity, total_price, status, expires_at) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_RESERVATION_BY_ID =
        "SELECT * FROM surplus_reservations WHERE reservation_id = ?";

    private static final String SELECT_RESERVATIONS_BY_USER =
        "SELECT * FROM surplus_reservations WHERE user_id = ? ORDER BY reserved_at DESC";

    private static final String SELECT_RESERVATIONS_BY_LISTING =
        "SELECT * FROM surplus_reservations WHERE listing_id = ? ORDER BY reserved_at DESC";

    private static final String UPDATE_RESERVATION_STATUS =
        "UPDATE surplus_reservations SET status = ? WHERE reservation_id = ?";

    /**
     * Creates a new surplus food listing and returns the generated primary key ID.
     *
     * @param listing details of the surplus listing to create
     * @return generated listing_id, or -1 on failure
     */
    @Override
    public int createListing(SurplusListing listing) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_LISTING, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, listing.getRestaurantId());
            ps.setInt(2, listing.getMenuItemId());
            ps.setDouble(3, listing.getOriginalPrice());
            ps.setDouble(4, listing.getDiscountedPrice());
            ps.setInt(5, listing.getAvailableQuantity());
            ps.setTimestamp(6, listing.getExpiryTime());
            ps.setString(7, listing.getStatus() != null ? listing.getStatus() : "active");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] createListing error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Retrieves a surplus listing by its primary key ID.
     *
     * @param listingId unique identifier of the surplus listing
     * @return SurplusListing if found, null otherwise
     */
    @Override
    public SurplusListing getListingById(int listingId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_LISTING_BY_ID)) {
            ps.setInt(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSurplusListing(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] getListingById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all active surplus listings that have not expired and have remaining stock.
     *
     * @return list of active SurplusListing objects ordered by upcoming expiry
     */
    @Override
    public List<SurplusListing> getActiveListings() {
        List<SurplusListing> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_LISTINGS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapSurplusListing(rs));
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] getActiveListings error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves all surplus listings created by a specific restaurant.
     *
     * @param restaurantId unique identifier of the restaurant
     * @return list of SurplusListing objects
     */
    @Override
    public List<SurplusListing> getListingsByRestaurant(int restaurantId) {
        List<SurplusListing> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_LISTINGS_BY_RESTAURANT)) {
            ps.setInt(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSurplusListing(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] getListingsByRestaurant error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates the status of a surplus listing (e.g., 'active', 'expired', 'sold_out', 'cancelled').
     *
     * @param listingId unique identifier of the surplus listing
     * @param status    new status string
     */
    @Override
    public void updateListingStatus(int listingId, String status) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_LISTING_STATUS)) {
            ps.setString(1, status);
            ps.setInt(2, listingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] updateListingStatus error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Atomically decrements stock using conditional UPDATE to prevent race conditions and overselling.
     * UPDATE surplus_listings SET available_quantity = available_quantity - ?
     * WHERE listing_id = ? AND available_quantity >= ? AND status = 'active' AND expiry_time > NOW()
     *
     * @param listingId unique identifier of the surplus listing
     * @param quantity  quantity to deduct
     * @return true if stock was sufficient and successfully decremented, false otherwise
     */
    @Override
    public boolean decrementStock(int listingId, int quantity) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DECREMENT_STOCK)) {
            ps.setInt(1, quantity);
            ps.setInt(2, listingId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] decrementStock error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Restores stock on reservation cancellation.
     *
     * @param listingId unique identifier of the surplus listing
     * @param quantity  quantity to restore
     */
    @Override
    public void incrementStock(int listingId, int quantity) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INCREMENT_STOCK)) {
            ps.setInt(1, quantity);
            ps.setInt(2, listingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] incrementStock error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Expires listings past their expiry_time. Returns count of expired listings.
     *
     * @return number of listings transitioned to 'expired'
     */
    @Override
    public int expireOverdueListings() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(EXPIRE_OVERDUE)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] expireOverdueListings error: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Creates a new reservation for surplus food and returns the generated reservation ID.
     *
     * @param reservation details of the surplus reservation
     * @return generated reservation_id, or -1 on failure
     */
    @Override
    public int createReservation(SurplusReservation reservation) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_RESERVATION, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reservation.getListingId());
            ps.setInt(2, reservation.getUserId());
            ps.setInt(3, reservation.getQuantity());
            ps.setDouble(4, reservation.getTotalPrice());
            ps.setString(5, reservation.getStatus() != null ? reservation.getStatus() : "reserved");
            ps.setTimestamp(6, reservation.getExpiresAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] createReservation error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Retrieves a surplus reservation by its primary key ID.
     *
     * @param reservationId unique identifier of the reservation
     * @return SurplusReservation if found, null otherwise
     */
    @Override
    public SurplusReservation getReservationById(int reservationId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RESERVATION_BY_ID)) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSurplusReservation(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] getReservationById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all surplus reservations placed by a specific user.
     *
     * @param userId unique identifier of the user
     * @return list of SurplusReservation objects for the user
     */
    @Override
    public List<SurplusReservation> getReservationsByUser(int userId) {
        List<SurplusReservation> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RESERVATIONS_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSurplusReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] getReservationsByUser error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Retrieves all surplus reservations made for a specific listing.
     *
     * @param listingId unique identifier of the surplus listing
     * @return list of SurplusReservation objects for the listing
     */
    @Override
    public List<SurplusReservation> getReservationsByListing(int listingId) {
        List<SurplusReservation> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RESERVATIONS_BY_LISTING)) {
            ps.setInt(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSurplusReservation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] getReservationsByListing error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Updates the status of a surplus reservation ('reserved', 'collected', 'cancelled', 'expired').
     *
     * @param reservationId unique identifier of the reservation
     * @param status        new status string
     */
    @Override
    public void updateReservationStatus(int reservationId, String status) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_RESERVATION_STATUS)) {
            ps.setString(1, status);
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SurplusListingDAO] updateReservationStatus error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Maps a ResultSet row to a SurplusListing entity.
     *
     * @param rs active ResultSet positioned at the desired row
     * @return populated SurplusListing instance
     * @throws SQLException if a column cannot be read
     */
    private SurplusListing mapSurplusListing(ResultSet rs) throws SQLException {
        SurplusListing listing = new SurplusListing();
        listing.setListingId(rs.getInt("listing_id"));
        listing.setRestaurantId(rs.getInt("restaurant_id"));
        listing.setMenuItemId(rs.getInt("menu_item_id"));
        listing.setOriginalPrice(rs.getDouble("original_price"));
        listing.setDiscountedPrice(rs.getDouble("discounted_price"));
        listing.setAvailableQuantity(rs.getInt("available_quantity"));
        listing.setExpiryTime(rs.getTimestamp("expiry_time"));
        listing.setStatus(rs.getString("status"));
        listing.setCreatedAt(rs.getTimestamp("created_at"));
        listing.setUpdatedAt(rs.getTimestamp("updated_at"));
        return listing;
    }

    /**
     * Maps a ResultSet row to a SurplusReservation entity.
     *
     * @param rs active ResultSet positioned at the desired row
     * @return populated SurplusReservation instance
     * @throws SQLException if a column cannot be read
     */
    private SurplusReservation mapSurplusReservation(ResultSet rs) throws SQLException {
        SurplusReservation r = new SurplusReservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setListingId(rs.getInt("listing_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setQuantity(rs.getInt("quantity"));
        r.setTotalPrice(rs.getDouble("total_price"));
        r.setStatus(rs.getString("status"));
        r.setReservedAt(rs.getTimestamp("reserved_at"));
        r.setExpiresAt(rs.getTimestamp("expires_at"));
        return r;
    }
}
