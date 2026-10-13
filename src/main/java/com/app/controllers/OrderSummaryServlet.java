package com.app.controllers;

import com.app.dao.OrderDAO;
import com.app.dao.MenuItemDAO;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.models.MenuItem;
import com.app.models.Order;
import com.app.models.OrderItem;
import com.app.models.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WebServlet("/orderSummary")
public class OrderSummaryServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final MenuItemDAO menuItemDAO = new MenuItemDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("loggedUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/user?action=login");
            return;
        }

        String orderIdParam = req.getParameter("orderId");
        if (orderIdParam == null) {
            resp.sendRedirect(req.getContextPath() + "/orderHistory");
            return;
        }

        int orderId;
        try {
            orderId = Integer.parseInt(orderIdParam);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Order order = orderDAO.getOrderById(orderId);
        if (order == null || (order.getUserId() != user.getUserId()
                && !"admin".equalsIgnoreCase(user.getRole()))) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<OrderItem> items = orderDAO.getOrderItems(orderId);
        Set<Integer> menuItemIds = new HashSet<>();
        for (OrderItem item : items) {
            menuItemIds.add(item.getMenuItemId());
        }
        Map<Integer, MenuItem> menuItemsMap = menuItemDAO.getMenuItemsAsMap(menuItemIds);

        req.setAttribute("order", order);
        req.setAttribute("items", items);
        req.setAttribute("menuItemsMap", menuItemsMap);

        req.getRequestDispatcher("/jsp/customer/order_summary.jsp")
                .forward(req, resp);
    }
}
