package com.app.dao_implementation;

import com.app.dao.GroupOrderDAO;
import com.app.models.GroupCartItem;
import com.app.models.GroupMember;
import com.app.models.GroupOrder;
import com.app.utility.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the GroupOrderDAO interface providing complete database access
 * for group ordering features, collaborative carts, member management, and checkout idempotency.
 */
public class GroupOrderDAOImpl implements GroupOrderDAO {

    private static final String INSERT_GROUP_ORDER =
        "INSERT INTO group_orders (creator_id, restaurant_id, invite_code, deadline, status) VALUES (?, ?, ?, ?, 'open')";

    private static final String SELECT_GROUP_BY_ID =
        "SELECT * FROM group_orders WHERE group_order_id = ?";

    private static final String SELECT_GROUP_BY_INVITE =
        "SELECT * FROM group_orders WHERE invite_code = ?";

    private static final String INSERT_MEMBER =
        "INSERT IGNORE INTO group_members (group_order_id, user_id) VALUES (?, ?)";

    private static final String SELECT_MEMBERS =
        "SELECT * FROM group_members WHERE group_order_id = ? ORDER BY joined_at";

    private static final String CHECK_MEMBER =
        "SELECT COUNT(*) FROM group_members WHERE group_order_id = ? AND user_id = ?";

    private static final String UPSERT_CART_ITEM =
        "INSERT INTO group_cart_items (group_order_id, user_id, menu_item_id, quantity) VALUES (?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), updated_at = CURRENT_TIMESTAMP";

    private static final String DELETE_CART_ITEM =
        "DELETE FROM group_cart_items WHERE group_order_id = ? AND user_id = ? AND menu_item_id = ?";

    private static final String SELECT_CART_ITEMS =
        "SELECT * FROM group_cart_items WHERE group_order_id = ? ORDER BY user_id, added_at";

    private static final String SELECT_CART_ITEMS_BY_USER =
        "SELECT * FROM group_cart_items WHERE group_order_id = ? AND user_id = ? ORDER BY added_at";

    private static final String UPDATE_STATUS =
        "UPDATE group_orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE group_order_id = ?";

    private static final String FINALIZE_GROUP =
        "UPDATE group_orders SET status = 'checked_out', result_order_id = ?, updated_at = CURRENT_TIMESTAMP WHERE group_order_id = ?";

    private static final String CLAIM_CHECKOUT_KEY =
        "UPDATE group_orders SET checkout_idempotency_key = ? WHERE group_order_id = ? AND checkout_idempotency_key IS NULL";

    private static final String GET_CHECKOUT_KEY =
        "SELECT checkout_idempotency_key FROM group_orders WHERE group_order_id = ?";

    private static final String SELECT_OPEN_GROUPS =
        "SELECT * FROM group_orders WHERE status = 'open' AND deadline > NOW() ORDER BY created_at DESC";

    /**
     * Creates a new group order and returns the generated primary key ID.
     *
     * @param groupOrder details of the group order to create
     * @return generated group_order_id, or -1 on error
     */
    @Override
    public int createGroupOrder(GroupOrder groupOrder) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_GROUP_ORDER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, groupOrder.getCreatorId());
            ps.setInt(2, groupOrder.getRestaurantId());
            ps.setString(3, groupOrder.getInviteCode());
            ps.setTimestamp(4, groupOrder.getDeadline());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] createGroupOrder error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Retrieves a group order record by primary key ID.
     *
     * @param groupOrderId primary key ID
     * @return GroupOrder object if found, null otherwise
     */
    @Override
    public GroupOrder getGroupOrderById(int groupOrderId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_GROUP_BY_ID)) {
            ps.setInt(1, groupOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGroupOrder(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] getGroupOrderById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a group order record by unique invite code.
     *
     * @param inviteCode unique invite code
     * @return GroupOrder object if found, null otherwise
     */
    @Override
    public GroupOrder getGroupOrderByInviteCode(String inviteCode) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_GROUP_BY_INVITE)) {
            ps.setString(1, inviteCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGroupOrder(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] getGroupOrderByInviteCode error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds a user to the group members list. Uses INSERT IGNORE to prevent duplicate membership.
     *
     * @param groupOrderId ID of the group order
     * @param userId       ID of the user to join
     * @return true if member was inserted, false if already a member or on error
     */
    @Override
    public boolean addMember(int groupOrderId, int userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_MEMBER)) {
            ps.setInt(1, groupOrderId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] addMember error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all members participating in the specified group order.
     *
     * @param groupOrderId ID of the group order
     * @return list of GroupMember objects
     */
    @Override
    public List<GroupMember> getMembers(int groupOrderId) {
        List<GroupMember> members = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_MEMBERS)) {
            ps.setInt(1, groupOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupMember m = new GroupMember();
                    m.setMemberId(rs.getInt("member_id"));
                    m.setGroupOrderId(rs.getInt("group_order_id"));
                    m.setUserId(rs.getInt("user_id"));
                    m.setJoinedAt(rs.getTimestamp("joined_at"));
                    members.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] getMembers error: " + e.getMessage());
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Checks whether a given user is registered as a member of the group order.
     *
     * @param groupOrderId ID of the group order
     * @param userId       ID of the user
     * @return true if the user is a member, false otherwise
     */
    @Override
    public boolean isMember(int groupOrderId, int userId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(CHECK_MEMBER)) {
            ps.setInt(1, groupOrderId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] isMember error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds or updates a menu item in the collaborative group cart for a specific member.
     *
     * @param groupOrderId ID of the group order
     * @param userId       ID of the member
     * @param menuItemId   ID of the menu item
     * @param quantity     quantity ordered
     */
    @Override
    public void upsertGroupCartItem(int groupOrderId, int userId, int menuItemId, int quantity) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_CART_ITEM)) {
            ps.setInt(1, groupOrderId);
            ps.setInt(2, userId);
            ps.setInt(3, menuItemId);
            ps.setInt(4, quantity);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] upsertGroupCartItem error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes a specific item from a member's group cart.
     *
     * @param groupOrderId ID of the group order
     * @param userId       ID of the member
     * @param menuItemId   ID of the menu item to delete
     */
    @Override
    public void removeGroupCartItem(int groupOrderId, int userId, int menuItemId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_CART_ITEM)) {
            ps.setInt(1, groupOrderId);
            ps.setInt(2, userId);
            ps.setInt(3, menuItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] removeGroupCartItem error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all items added across all members for a group order.
     *
     * @param groupOrderId ID of the group order
     * @return list of all GroupCartItem records
     */
    @Override
    public List<GroupCartItem> getGroupCartItems(int groupOrderId) {
        List<GroupCartItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CART_ITEMS)) {
            ps.setInt(1, groupOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapCartItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] getGroupCartItems error: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Retrieves items added to the group cart by a specific member.
     *
     * @param groupOrderId ID of the group order
     * @param userId       ID of the member
     * @return list of GroupCartItem records for the member
     */
    @Override
    public List<GroupCartItem> getGroupCartItemsByUser(int groupOrderId, int userId) {
        List<GroupCartItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CART_ITEMS_BY_USER)) {
            ps.setInt(1, groupOrderId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapCartItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] getGroupCartItemsByUser error: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Updates the status of a group order.
     *
     * @param groupOrderId ID of the group order
     * @param newStatus    new status string
     */
    @Override
    public void updateStatus(int groupOrderId, String newStatus) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, newStatus);
            ps.setInt(2, groupOrderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] updateStatus error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the resulting finalized order ID and transitions status to 'checked_out' atomically.
     *
     * @param groupOrderId  ID of the group order
     * @param resultOrderId ID of the finalized checkout order
     */
    @Override
    public void finalizeGroupOrder(int groupOrderId, int resultOrderId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(FINALIZE_GROUP)) {
            ps.setInt(1, resultOrderId);
            ps.setInt(2, groupOrderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] finalizeGroupOrder error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Atomically claims a checkout idempotency key using the external Connection.
     *
     * @param groupOrderId   ID of the group order
     * @param idempotencyKey unique key
     * @param conn           active database connection
     * @return true if key was claimed, false if already claimed or failed
     * @throws SQLException if a database access error occurs
     */
    @Override
    public boolean claimCheckoutKey(int groupOrderId, String idempotencyKey, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(CLAIM_CHECKOUT_KEY)) {
            ps.setString(1, idempotencyKey);
            ps.setInt(2, groupOrderId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves the checkout idempotency key stored for the specified group order.
     *
     * @param groupOrderId ID of the group order
     * @return idempotency key string or null if not set
     */
    @Override
    public String getCheckoutIdempotencyKey(int groupOrderId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_CHECKOUT_KEY)) {
            ps.setInt(1, groupOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] getCheckoutIdempotencyKey error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all active open group orders whose deadline is in the future.
     *
     * @return list of open GroupOrder objects
     */
    @Override
    public List<GroupOrder> getOpenGroupOrders() {
        List<GroupOrder> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_OPEN_GROUPS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapGroupOrder(rs));
            }
        } catch (SQLException e) {
            System.err.println("[GroupOrderDAO] getOpenGroupOrders error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Maps a ResultSet row to a GroupOrder entity.
     *
     * @param rs active ResultSet positioned at the desired row
     * @return populated GroupOrder instance
     * @throws SQLException if a column cannot be read
     */
    private GroupOrder mapGroupOrder(ResultSet rs) throws SQLException {
        GroupOrder g = new GroupOrder();
        g.setGroupOrderId(rs.getInt("group_order_id"));
        g.setCreatorId(rs.getInt("creator_id"));
        g.setRestaurantId(rs.getInt("restaurant_id"));
        g.setInviteCode(rs.getString("invite_code"));
        g.setDeadline(rs.getTimestamp("deadline"));
        g.setStatus(rs.getString("status"));
        int roi = rs.getInt("result_order_id");
        g.setResultOrderId(rs.wasNull() ? null : roi);
        g.setCheckoutIdempotencyKey(rs.getString("checkout_idempotency_key"));
        g.setCreatedAt(rs.getTimestamp("created_at"));
        g.setUpdatedAt(rs.getTimestamp("updated_at"));
        return g;
    }

    /**
     * Maps a ResultSet row to a GroupCartItem entity.
     *
     * @param rs active ResultSet positioned at the desired row
     * @return populated GroupCartItem instance
     * @throws SQLException if a column cannot be read
     */
    private GroupCartItem mapCartItem(ResultSet rs) throws SQLException {
        GroupCartItem item = new GroupCartItem();
        item.setItemId(rs.getInt("item_id"));
        item.setGroupOrderId(rs.getInt("group_order_id"));
        item.setUserId(rs.getInt("user_id"));
        item.setMenuItemId(rs.getInt("menu_item_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setAddedAt(rs.getTimestamp("added_at"));
        item.setUpdatedAt(rs.getTimestamp("updated_at"));
        return item;
    }
}
