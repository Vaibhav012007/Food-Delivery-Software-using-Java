package com.app.controllers;

import com.app.dao.OrderDAO;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.models.Order;
import com.app.models.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/payment")
public class PaymentServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Ensure user logged in
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("loggedUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/user?action=login");
            return;
        }

        String orderIdParam = req.getParameter("orderId");
        if (orderIdParam == null) {
            resp.sendRedirect(req.getContextPath() + "/restaurant?action=list");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdParam);
            Order order = orderDAO.getOrderById(orderId);
            if (order == null || (order.getUserId() != user.getUserId()
                    && !"admin".equalsIgnoreCase(user.getRole()))) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        req.setAttribute("orderId", orderIdParam);

        req.getRequestDispatcher("/jsp/customer/payment.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("loggedUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/user?action=login");
            return;
        }

        String orderIdParam = req.getParameter("orderId");
        if (orderIdParam == null) {
            resp.sendRedirect(req.getContextPath() + "/restaurant?action=list");
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
        if (!"pending".equalsIgnoreCase(order.getPaymentStatus())
                || !"placed".equalsIgnoreCase(order.getStatus())) {
            resp.sendError(HttpServletResponse.SC_CONFLICT);
            return;
        }

        // Your DB supports: pending → completed
        orderDAO.updatePaymentStatus(orderId, "completed");

        // After completing payment → restaurant accepts automatically
        orderDAO.updateOrderStatus(orderId, "accepted");

        resp.sendRedirect(req.getContextPath() + "/orderSummary?orderId=" + orderId);
    }
}
