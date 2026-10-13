package com.app.service;

import com.app.models.GroupCartItem;
import com.app.models.GroupOrder;
import com.app.models.GroupMember;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Group Ordering.
 * Allows multiple users to collaborate on a single order from the same restaurant.
 */
public interface GroupOrderService {
    /**
     * Creates a group order, returns generated group order ID.
     *
     * @param creatorId       user ID of the group creator/host
     * @param restaurantId    restaurant ID for the order
     * @param deadlineMinutes minutes until order is closed/expired
     * @return generated group order ID
     */
    int createGroup(int creatorId, int restaurantId, int deadlineMinutes);

    /**
     * Joins a group by invite code. Returns group order ID or throws if not joinable.
     *
     * @param inviteCode unique shareable invite code
     * @param userId     user ID of the member joining
     * @return group order ID
     */
    int joinGroup(String inviteCode, int userId);

    /**
     * Retrieves a group order by ID.
     *
     * @param groupOrderId group order ID
     * @return GroupOrder object, or null if not found
     */
    GroupOrder getGroupById(int groupOrderId);

    /**
     * Adds or updates item in shared cart for a member.
     *
     * @param groupOrderId group order ID
     * @param userId       user ID of the member
     * @param menuItemId   menu item ID
     * @param quantity     quantity (removes item if quantity <= 0)
     */
    void addItemToGroup(int groupOrderId, int userId, int menuItemId, int quantity);

    /**
     * Removes item. Only the item owner can remove it.
     *
     * @param groupOrderId group order ID
     * @param userId       user ID of the member who added it
     * @param menuItemId   menu item ID
     */
    void removeItemFromGroup(int groupOrderId, int userId, int menuItemId);

    /**
     * Retrieves all items in the group cart across all members.
     *
     * @param groupOrderId group order ID
     * @return list of group cart items
     */
    List<GroupCartItem> getGroupCart(int groupOrderId);

    /**
     * Retrieves cart items added by a specific member in the group order.
     *
     * @param groupOrderId group order ID
     * @param userId       user ID of the member
     * @return list of group cart items for that member
     */
    List<GroupCartItem> getMemberCart(int groupOrderId, int userId);

    /**
     * Retrieves all members participating in the group order.
     *
     * @param groupOrderId group order ID
     * @return list of group members
     */
    List<GroupMember> getGroupMembers(int groupOrderId);

    /**
     * Returns subtotal per user: userId -> subtotal.
     *
     * @param groupOrderId group order ID
     * @return map of user ID to their subtotal amount
     */
    Map<Integer, Double> getMemberTotals(int groupOrderId);

    /**
     * Checks out the group order. Only creator can initiate.
     * Thread-safe: uses GroupCheckoutGuard + DB idempotency key.
     *
     * @param groupOrderId  group order ID
     * @param creatorId     user ID of the group creator
     * @param paymentMethod payment method (e.g., cash_on_delivery, upi, card)
     * @param addressId     delivery address ID (nullable)
     * @return the created order ID
     */
    int checkoutGroup(int groupOrderId, int creatorId, String paymentMethod, Integer addressId);

    /**
     * Retrieves all active/open group orders.
     *
     * @return list of open group orders
     */
    List<GroupOrder> getOpenGroups();
}
