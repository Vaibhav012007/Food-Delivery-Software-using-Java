package com.app.controllers;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

import com.app.dao.OrderDAO;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.models.Order;
import com.app.models.User;

@WebServlet("/orderSuccess")
public class OrderSuccessServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("loggedUser");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/user?action=login");
            return;
        }
        try {
            int orderId = Integer.parseInt(req.getParameter("orderId"));
            Order order = orderDAO.getOrderById(orderId);
            if (order == null || (order.getUserId() != user.getUserId()
                    && !"admin".equalsIgnoreCase(user.getRole()))) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("order", order);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        req.getRequestDispatcher("/jsp/customer/orderSuccess.jsp")
           .forward(req, resp);
    }
}
