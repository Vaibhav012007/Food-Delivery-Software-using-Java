package com.app.controllers;

import com.app.dao.MenuItemDAO;
import com.app.dao.RestaurantDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.exceptions.AppException;
import com.app.models.*;
import com.app.service.GroupOrderService;
import com.app.service.GroupOrderServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/groupOrder")
public class GroupOrderServlet extends HttpServlet {

    private GroupOrderService groupOrderService;
    private RestaurantDAO restaurantDAO;
    private MenuItemDAO menuItemDAO;

    @Override
    public void init() throws ServletException {
        groupOrderService = new GroupOrderServiceImpl();
        restaurantDAO = new RestaurantDAOImpl();
        menuItemDAO = new MenuItemDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getLoggedUser(req, resp);
        if (user == null) return;

        String action = req.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "create":
                req.setAttribute("restaurants", restaurantDAO.getAllRestaurants());
                req.setAttribute("formAction", "create");
                req.getRequestDispatcher("/jsp/customer/groupOrder.jsp").forward(req, resp);
                break;
            case "view":
                showGroupView(req, resp, user);
                break;
            case "join":
                String code = req.getParameter("code");
                if (code != null && !code.isEmpty()) {
                    try {
                        int gid = groupOrderService.joinGroup(code.trim().toUpperCase(), user.getUserId());
                        resp.sendRedirect(req.getContextPath() + "/groupOrder?action=view&id=" + gid);
                    } catch (AppException e) {
                        req.setAttribute("error", e.getMessage());
                        req.setAttribute("formAction", "join");
                        req.getRequestDispatcher("/jsp/customer/groupOrder.jsp").forward(req, resp);
                    }
                } else {
                    req.setAttribute("formAction", "join");
                    req.getRequestDispatcher("/jsp/customer/groupOrder.jsp").forward(req, resp);
                }
                break;
            default:
                req.setAttribute("openGroups", groupOrderService.getOpenGroups());
                req.setAttribute("formAction", "list");
                req.getRequestDispatcher("/jsp/customer/groupOrder.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getLoggedUser(req, resp);
        if (user == null) return;

        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            switch (action) {
                case "create": {
                    int restaurantId = Integer.parseInt(req.getParameter("restaurantId"));
                    int deadlineMinutes = 60;
                    String dm = req.getParameter("deadlineMinutes");
                    if (dm != null && !dm.isEmpty()) deadlineMinutes = Integer.parseInt(dm);
                    int groupId = groupOrderService.createGroup(user.getUserId(), restaurantId, deadlineMinutes);
                    resp.sendRedirect(req.getContextPath() + "/groupOrder?action=view&id=" + groupId);
                    break;
                }
                case "join": {
                    String inviteCode = req.getParameter("inviteCode");
                    int groupId = groupOrderService.joinGroup(inviteCode.trim().toUpperCase(), user.getUserId());
                    resp.sendRedirect(req.getContextPath() + "/groupOrder?action=view&id=" + groupId);
                    break;
                }
                case "addItem": {
                    int groupOrderId = Integer.parseInt(req.getParameter("groupOrderId"));
                    int menuItemId = Integer.parseInt(req.getParameter("menuItemId"));
                    int quantity = Integer.parseInt(req.getParameter("quantity"));
                    groupOrderService.addItemToGroup(groupOrderId, user.getUserId(), menuItemId, quantity);
                    resp.sendRedirect(req.getContextPath() + "/groupOrder?action=view&id=" + groupOrderId);
                    break;
                }
                case "removeItem": {
                    int groupOrderId = Integer.parseInt(req.getParameter("groupOrderId"));
                    int menuItemId = Integer.parseInt(req.getParameter("menuItemId"));
                    groupOrderService.removeItemFromGroup(groupOrderId, user.getUserId(), menuItemId);
                    resp.sendRedirect(req.getContextPath() + "/groupOrder?action=view&id=" + groupOrderId);
                    break;
                }
                case "checkout": {
                    int groupOrderId = Integer.parseInt(req.getParameter("groupOrderId"));
                    String paymentMethod = req.getParameter("paymentMethod");
                    String addrStr = req.getParameter("addressId");
                    Integer addressId = (addrStr != null && !addrStr.isEmpty()) ? Integer.parseInt(addrStr) : null;
                    int orderId = groupOrderService.checkoutGroup(groupOrderId, user.getUserId(), paymentMethod, addressId);
                    resp.sendRedirect(req.getContextPath() + "/orderSuccess?orderId=" + orderId);
                    break;
                }
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (AppException e) {
            req.setAttribute("error", e.getMessage());
            String groupIdStr = req.getParameter("groupOrderId");
            if (groupIdStr != null) {
                try {
                    showGroupView(req, resp, user);
                } catch (Exception ex) {
                    req.getRequestDispatcher("/jsp/customer/groupOrder.jsp").forward(req, resp);
                }
            } else {
                req.getRequestDispatcher("/jsp/customer/groupOrder.jsp").forward(req, resp);
            }
        }
    }

    private void showGroupView(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null) {
            resp.sendRedirect(req.getContextPath() + "/groupOrder");
            return;
        }
        int groupOrderId = Integer.parseInt(idStr);
        GroupOrder group = groupOrderService.getGroupById(groupOrderId);
        if (group == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Group order not found");
            return;
        }

        List<GroupCartItem> cartItems = groupOrderService.getGroupCart(groupOrderId);
        List<GroupMember> members = groupOrderService.getGroupMembers(groupOrderId);
        Map<Integer, Double> memberTotals = groupOrderService.getMemberTotals(groupOrderId);

        Set<Integer> menuItemIds = new HashSet<>();
        for (GroupCartItem ci : cartItems) menuItemIds.add(ci.getMenuItemId());
        Map<Integer, MenuItem> menuItemsMap = menuItemDAO.getMenuItemsAsMap(menuItemIds);

        double grandTotal = 0.0;
        for (Double subtotal : memberTotals.values()) grandTotal += subtotal;

        boolean isCreator = group.getCreatorId() == user.getUserId();
        boolean isMember = false;
        for (GroupMember m : members) {
            if (m.getUserId() == user.getUserId()) { isMember = true; break; }
        }

        List<MenuItem> restaurantMenu = menuItemDAO.getMenuItemsByRestaurantId(group.getRestaurantId());

        req.setAttribute("group", group);
        req.setAttribute("cartItems", cartItems);
        req.setAttribute("members", members);
        req.setAttribute("memberTotals", memberTotals);
        req.setAttribute("menuItemsMap", menuItemsMap);
        req.setAttribute("grandTotal", grandTotal);
        req.setAttribute("isCreator", isCreator);
        req.setAttribute("isMember", isMember);
        req.setAttribute("restaurantMenu", restaurantMenu);
        req.setAttribute("formAction", "view");

        req.getRequestDispatcher("/jsp/customer/groupOrder.jsp").forward(req, resp);
    }

    private User getLoggedUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("loggedUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/user?action=login");
            return null;
        }
        return user;
    }
}
