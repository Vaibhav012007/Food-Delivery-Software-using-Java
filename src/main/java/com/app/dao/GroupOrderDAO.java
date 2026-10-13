package com.app.dao;

import com.app.models.GroupCartItem;
import com.app.models.GroupMember;
import com.app.models.GroupOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object (DAO) interface defining database operations for
 * Group Orders, group membership, shared cart items, and checkout idempotency.
 */
public interface GroupOrderDAO {

    /**
     * Creates a new group order and returns the generated ID.
     *
     * @param groupOrder the GroupOrder object containing initial order configuration
     * @return the generated group_order_id, or -1 if the insertion fails
     */
    int createGroupOrder(GroupOrder groupOrder);

    /**
     * Retrieves a group order by its primary key ID.
     *
     * @param groupOrderId unique identifier of the group order
     * @return the GroupOrder matching the ID, or null if not found
     */
    GroupOrder getGroupOrderById(int groupOrderId);

    /**
     * Retrieves a group order by its unique invite code.
     *
     * @param inviteCode unique shareable invite code
     * @return the GroupOrder matching the code, or null if not found
     */
    GroupOrder getGroupOrderByInviteCode(String inviteCode);

    /**
     * Adds a member to the group order. Returns false if the user is already a member.
     *
     * @param groupOrderId unique identifier of the group order
     * @param userId       unique identifier of the user to join
     * @return true if the member was added, false if already joined or on error
     */
    boolean addMember(int groupOrderId, int userId);

    /**
     * Retrieves all members associated with a given group order.
     *
     * @param groupOrderId unique identifier of the group order
     * @return list of GroupMember objects belonging to this group order
     */
    List<GroupMember> getMembers(int groupOrderId);

    /**
     * Checks whether a specific user is currently a member of the group order.
     *
     * @param groupOrderId unique identifier of the group order
     * @param userId       unique identifier of the user
     * @return true if the user is a registered member of the group, false otherwise
     */
    boolean isMember(int groupOrderId, int userId);

    /**
     * Adds or updates an item in the group cart for a specific member.
     *
     * @param groupOrderId unique identifier of the group order
     * @param userId       unique identifier of the member contributing the item
     * @param menuItemId   unique identifier of the menu item
     * @param quantity     quantity of the item to set
     */
    void upsertGroupCartItem(int groupOrderId, int userId, int menuItemId, int quantity);

    /**
     * Removes an item from the group cart for a specific member.
     *
     * @param groupOrderId unique identifier of the group order
     * @param userId       unique identifier of the member
     * @param menuItemId   unique identifier of the menu item to remove
     */
    void removeGroupCartItem(int groupOrderId, int userId, int menuItemId);

    /**
     * Retrieves all items added across all members for a specific group order.
     *
     * @param groupOrderId unique identifier of the group order
     * @return list of GroupCartItem objects for the entire group
     */
    List<GroupCartItem> getGroupCartItems(int groupOrderId);

    /**
     * Retrieves all group cart items contributed by a specific user.
     *
     * @param groupOrderId unique identifier of the group order
     * @param userId       unique identifier of the contributing member
     * @return list of GroupCartItem objects added by the specified user
     */
    List<GroupCartItem> getGroupCartItemsByUser(int groupOrderId, int userId);

    /**
     * Updates the status of an existing group order.
     *
     * @param groupOrderId unique identifier of the group order
     * @param newStatus    new status string ('open', 'closed', 'checked_out', 'expired', 'failed')
     */
    void updateStatus(int groupOrderId, String newStatus);

    /**
     * Sets the resulting finalized order ID and transitions status to 'checked_out' atomically.
     *
     * @param groupOrderId  unique identifier of the group order
     * @param resultOrderId unique identifier of the resulting checkout order
     */
    void finalizeGroupOrder(int groupOrderId, int resultOrderId);

    /**
     * Claims a checkout idempotency key atomically. Returns false if already claimed.
     *
     * @param groupOrderId   unique identifier of the group order
     * @param idempotencyKey unique idempotency token preventing duplicate submissions
     * @param conn           active database connection within an ongoing transaction
     * @return true if the key was claimed successfully, false if already claimed
     * @throws SQLException if a database access error occurs
     */
    boolean claimCheckoutKey(int groupOrderId, String idempotencyKey, Connection conn) throws SQLException;

    /**
     * Retrieves the existing checkout idempotency key for a group order (to detect duplicates).
     *
     * @param groupOrderId unique identifier of the group order
     * @return idempotency key string if present, null otherwise
     */
    String getCheckoutIdempotencyKey(int groupOrderId);

    /**
     * Retrieves all currently open group orders whose deadline has not expired.
     *
     * @return list of open GroupOrder objects
     */
    List<GroupOrder> getOpenGroupOrders();
}
