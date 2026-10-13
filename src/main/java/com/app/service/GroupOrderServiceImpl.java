package com.app.service;

import com.app.concurrency.GroupCheckoutGuard;
import com.app.concurrency.NotificationService;
import com.app.dao.GroupOrderDAO;
import com.app.dao.MenuItemDAO;
import com.app.dao.OrderDAO;
import com.app.dao_implementation.GroupOrderDAOImpl;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.exceptions.*;
import com.app.models.*;
import com.app.utility.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

/**
 * Implementation of GroupOrderService.
 * Coordinates campus group ordering, cart management, and concurrency-safe checkout.
 */
public class GroupOrderServiceImpl implements GroupOrderService {

    private final GroupOrderDAO groupOrderDAO = new GroupOrderDAOImpl();
    private final MenuItemDAO menuItemDAO = new MenuItemDAOImpl();
    private final OrderDAO orderDAO = new OrderDAOImpl();

    @Override
    public int createGroup(int creatorId, int restaurantId, int deadlineMinutes) {
        String inviteCode = generateInviteCode();
        Timestamp deadline = new Timestamp(System.currentTimeMillis() + (long) deadlineMinutes * 60 * 1000);

        GroupOrder group = new GroupOrder();
        group.setCreatorId(creatorId);
        group.setRestaurantId(restaurantId);
        group.setInviteCode(inviteCode);
        group.setDeadline(deadline);
        group.setStatus("open");

        int groupId = groupOrderDAO.createGroupOrder(group);
        if (groupId <= 0) {
            throw new AppException("Failed to create group order");
        }
        // Creator automatically becomes a member
        groupOrderDAO.addMember(groupId, creatorId);
        System.out.println("[GroupOrderService] Group created: id=" + groupId + ", invite=" + inviteCode);
        return groupId;
    }

    @Override
    public int joinGroup(String inviteCode, int userId) {
        GroupOrder group = groupOrderDAO.getGroupOrderByInviteCode(inviteCode);
        if (group == null) {
            throw new AppException("Group not found for invite code: " + inviteCode);
        }
        if (!group.isOpen()) {
            throw new GroupExpiredException(group.getGroupOrderId(), group.getStatus());
        }
        groupOrderDAO.addMember(group.getGroupOrderId(), userId);
        System.out.println("[GroupOrderService] User " + userId + " joined group " + group.getGroupOrderId());
        return group.getGroupOrderId();
    }

    @Override
    public GroupOrder getGroupById(int groupOrderId) {
        return groupOrderDAO.getGroupOrderById(groupOrderId);
    }

    @Override
    public void addItemToGroup(int groupOrderId, int userId, int menuItemId, int quantity) {
        GroupOrder group = groupOrderDAO.getGroupOrderById(groupOrderId);
        if (group == null || !group.isOpen()) {
            throw new GroupExpiredException(groupOrderId, group != null ? group.getStatus() : "not_found");
        }
        if (!groupOrderDAO.isMember(groupOrderId, userId)) {
            throw new UnauthorizedAccessException(userId, "add items to group " + groupOrderId);
        }
        if (quantity <= 0) {
            groupOrderDAO.removeGroupCartItem(groupOrderId, userId, menuItemId);
        } else {
            groupOrderDAO.upsertGroupCartItem(groupOrderId, userId, menuItemId, quantity);
        }
    }

    @Override
    public void removeItemFromGroup(int groupOrderId, int userId, int menuItemId) {
        if (!groupOrderDAO.isMember(groupOrderId, userId)) {
            throw new UnauthorizedAccessException(userId, "remove items from group " + groupOrderId);
        }
        groupOrderDAO.removeGroupCartItem(groupOrderId, userId, menuItemId);
    }

    @Override
    public List<GroupCartItem> getGroupCart(int groupOrderId) {
        return groupOrderDAO.getGroupCartItems(groupOrderId);
    }

    @Override
    public List<GroupCartItem> getMemberCart(int groupOrderId, int userId) {
        return groupOrderDAO.getGroupCartItemsByUser(groupOrderId, userId);
    }

    @Override
    public List<GroupMember> getGroupMembers(int groupOrderId) {
        return groupOrderDAO.getMembers(groupOrderId);
    }

    @Override
    public Map<Integer, Double> getMemberTotals(int groupOrderId) {
        List<GroupCartItem> items = groupOrderDAO.getGroupCartItems(groupOrderId);
        Set<Integer> menuItemIds = new HashSet<>();
        for (GroupCartItem item : items) {
            menuItemIds.add(item.getMenuItemId());
        }
        Map<Integer, MenuItem> menuItems = menuItemDAO.getMenuItemsAsMap(menuItemIds);

        Map<Integer, Double> totals = new HashMap<>();
        for (GroupCartItem item : items) {
            MenuItem mi = menuItems.get(item.getMenuItemId());
            if (mi != null) {
                double subtotal = mi.getPrice() * item.getQuantity();
                Double current = totals.get(item.getUserId());
                totals.put(item.getUserId(), (current != null ? current : 0.0) + subtotal);
            }
        }
        return totals;
    }

    @Override
    public int checkoutGroup(int groupOrderId, int creatorId, String paymentMethod, Integer addressId) {
        GroupOrder group = groupOrderDAO.getGroupOrderById(groupOrderId);
        if (group == null) {
            throw new AppException("Group order not found: " + groupOrderId);
        }
        if (group.getCreatorId() != creatorId) {
            throw new UnauthorizedAccessException(creatorId, "checkout group " + groupOrderId);
        }
        if (!"open".equals(group.getStatus())) {
            throw new GroupExpiredException(groupOrderId, group.getStatus());
        }

        // In-memory guard (first line of defense)
        boolean locked = GroupCheckoutGuard.tryLock(groupOrderId);
        if (!locked) {
            throw new DuplicateCheckoutException(-1);
        }

        try {
            return doCheckout(group, creatorId, paymentMethod, addressId);
        } finally {
            GroupCheckoutGuard.unlock(groupOrderId);
        }
    }

    private int doCheckout(GroupOrder group, int creatorId, String paymentMethod, Integer addressId) {
        String idempotencyKey = UUID.randomUUID().toString();
        int groupOrderId = group.getGroupOrderId();

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // DB idempotency key claim (second line of defense, persistent)
            boolean claimed = groupOrderDAO.claimCheckoutKey(groupOrderId, idempotencyKey, conn);
            if (!claimed) {
                conn.rollback();
                throw new DuplicateCheckoutException(-1);
            }

            List<GroupCartItem> cartItems = groupOrderDAO.getGroupCartItems(groupOrderId);
            if (cartItems.isEmpty()) {
                conn.rollback();
                throw new AppException("Cannot checkout: group cart is empty");
            }

            Set<Integer> menuItemIds = new HashSet<>();
            for (GroupCartItem ci : cartItems) {
                menuItemIds.add(ci.getMenuItemId());
            }
            Map<Integer, MenuItem> menuItems = menuItemDAO.getMenuItemsAsMap(menuItemIds);

            List<OrderItem> orderItems = new ArrayList<>();
            double total = 0.0;
            for (GroupCartItem ci : cartItems) {
                MenuItem mi = menuItems.get(ci.getMenuItemId());
                if (mi == null) {
                    conn.rollback();
                    throw new AppException("Menu item not found: " + ci.getMenuItemId());
                }
                OrderItem oi = new OrderItem();
                oi.setMenuItemId(ci.getMenuItemId());
                oi.setQuantity(ci.getQuantity());
                oi.setPriceAtOrder(mi.getPrice());
                orderItems.add(oi);
                total += mi.getPrice() * ci.getQuantity();
            }

            Order order = new Order();
            order.setUserId(creatorId);
            order.setRestaurantId(group.getRestaurantId());
            order.setAddressId(addressId);
            order.setTotalAmount(total);
            order.setStatus(OrderStatus.PLACED.getDbValue());
            order.setPaymentMethod(paymentMethod != null ? paymentMethod : "cash_on_delivery");
            order.setPaymentStatus("pending");
            order.setDeliveryInstructions("Campus group order #" + groupOrderId);
            order.setIdempotencyKey(idempotencyKey + "_group");

            int orderId = placeOrderWithConnection(order, orderItems, conn);
            if (orderId <= 0) {
                conn.rollback();
                throw new AppException("Failed to create order for group checkout");
            }

            finalizeGroupWithConnection(groupOrderId, orderId, conn);

            conn.commit();
            System.out.println("[GroupOrderService] Group " + groupOrderId + " checked out. Order=" + orderId);

            List<GroupMember> members = groupOrderDAO.getMembers(groupOrderId);
            for (GroupMember m : members) {
                NotificationService.getInstance().notify(m.getUserId(),
                    "Your group order #" + groupOrderId + " has been placed! Order #" + orderId, orderId);
            }

            return orderId;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rb) { rb.printStackTrace(); }
            }
            throw new AppException("Group checkout failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    private int placeOrderWithConnection(Order order, List<OrderItem> items, Connection conn) throws SQLException {
        String insertOrder = "INSERT INTO orders (user_id, restaurant_id, address_id, total_amount, status, payment_method, payment_status, delivery_instructions, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItem = "INSERT INTO order_items (order_id, menu_item_id, quantity, price_at_order) VALUES (?, ?, ?, ?)";

        int orderId = -1;
        try (PreparedStatement ps = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getUserId());
            ps.setInt(2, order.getRestaurantId());
            if (order.getAddressId() != null) {
                ps.setInt(3, order.getAddressId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setDouble(4, order.getTotalAmount());
            ps.setString(5, order.getStatus());
            ps.setString(6, order.getPaymentMethod());
            ps.setString(7, order.getPaymentStatus());
            ps.setString(8, order.getDeliveryInstructions());
            ps.setString(9, order.getIdempotencyKey());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    orderId = rs.getInt(1);
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
            for (OrderItem item : items) {
                ps.setInt(1, orderId);
                ps.setInt(2, item.getMenuItemId());
                ps.setInt(3, item.getQuantity());
                ps.setDouble(4, item.getPriceAtOrder());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return orderId;
    }

    private void finalizeGroupWithConnection(int groupOrderId, int orderId, Connection conn) throws SQLException {
        String sql = "UPDATE group_orders SET status = 'checked_out', result_order_id = ?, updated_at = CURRENT_TIMESTAMP WHERE group_order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, groupOrderId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<GroupOrder> getOpenGroups() {
        return groupOrderDAO.getOpenGroupOrders();
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        Random rng = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
