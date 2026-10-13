package com.app.controllers;

import com.app.dao.MenuItemDAO;
import com.app.dao.OrderDAO;
import com.app.dao.RestaurantDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.exceptions.AppException;
import com.app.models.*;
import com.app.service.OrderService;
import com.app.service.OrderServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/adminOrder")
public class AdminOrderServlet extends HttpServlet {

    private OrderDAO orderDAO;
    private RestaurantDAO restaurantDAO;
    private OrderService orderService;

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAOImpl();
        restaurantDAO = new RestaurantDAOImpl();
        orderService = new OrderServiceImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req, resp)) return;

        String statusFilter = req.getParameter("status");
        List<Order> orders;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            orders = orderDAO.getOrdersByStatus(statusFilter);
        } else {
            orders = orderDAO.getOrdersByStatus("placed");
            orders.addAll(orderDAO.getOrdersByStatus("accepted"));
            orders.addAll(orderDAO.getOrdersByStatus("preparing"));
            orders.addAll(orderDAO.getOrdersByStatus("ready_for_pickup"));
        }

        Map<Integer, String> restaurantNames = new HashMap<>();
        for (Order o : orders) {
            if (!restaurantNames.containsKey(o.getRestaurantId())) {
                Restaurant r = restaurantDAO.getRestaurantById(o.getRestaurantId());
                restaurantNames.put(o.getRestaurantId(), r != null ? r.getName() : "Unknown");
            }
        }

        req.setAttribute("orders", orders);
        req.setAttribute("restaurantNames", restaurantNames);
        req.setAttribute("statusFilter", statusFilter);
        req.getRequestDispatcher("/jsp/admin/orders.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req, resp)) return;

        String action = req.getParameter("action");
        if ("updateStatus".equals(action)) {
            try {
                int orderId = Integer.parseInt(req.getParameter("orderId"));
                String newStatus = req.getParameter("newStatus");
                User admin = (User) req.getSession(false).getAttribute("loggedUser");
                orderService.updateStatus(orderId, newStatus, admin.getUserId(), "admin");
                req.setAttribute("message", "Order #" + orderId + " updated to " + newStatus);
            } catch (AppException e) {
                req.setAttribute("error", e.getMessage());
            }
        }
        doGet(req, resp);
    }

    private boolean isAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("loggedUser");
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
