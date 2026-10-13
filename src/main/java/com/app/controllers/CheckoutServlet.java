package com.app.controllers;

import java.io.IOException;
import java.util.*;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.app.concurrency.NotificationService;
import com.app.dao.CartDAO;
import com.app.dao.AddressDAO;
import com.app.dao.MenuItemDAO;
import com.app.dao_implementation.CartDAOImpl;
import com.app.dao_implementation.AddressDAOImpl;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.exceptions.AppException;
import com.app.exceptions.DuplicateCheckoutException;
import com.app.models.Cart;
import com.app.models.CartItem;
import com.app.models.MenuItem;
import com.app.models.OrderItem;
import com.app.models.User;
import com.app.service.OrderService;
import com.app.service.OrderServiceImpl;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private final CartDAO cartDAO = new CartDAOImpl();
    private final AddressDAO addressDAO = new AddressDAOImpl();
    private final MenuItemDAO menuItemDAO = new MenuItemDAOImpl();
    private final OrderService orderService = new OrderServiceImpl();

    /* ==========================================================
       GET  : SHOW CHECKOUT PAGE
       ========================================================== */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("loggedUser");

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/user?action=login");
            return;
        }

        int userId = user.getUserId();
        Cart cart = cartDAO.getCartByUserId(userId);

        if (cart == null) {
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
            return;
        }

        List<CartItem> cartItems = cartDAO.getCartItems(cart.getCartId());
        if (cartItems == null || cartItems.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
            return;
        }

        double totalAmount = cartDAO.getCartTotal(cart.getCartId());

        Map<Integer, MenuItem> menuItems = new HashMap<>();
        for (CartItem ci : cartItems) {
            menuItems.put(ci.getMenuItemId(), menuItemDAO.getMenuItemById(ci.getMenuItemId()));
        }

        // Generate and bind idempotency token to session for double-submit prevention
        String idempotencyKey = UUID.randomUUID().toString();
        session.setAttribute("checkoutIdempotencyKey", idempotencyKey);
        req.setAttribute("idempotencyKey", idempotencyKey);
        req.setAttribute("cartItems", cartItems);
        req.setAttribute("menuItems", menuItems);
        req.setAttribute("totalAmount", totalAmount);

        req.getRequestDispatcher("/jsp/customer/checkout.jsp").forward(req, resp);
    }

    /* ==========================================================
       POST : PLACE ORDER
       ========================================================== */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("loggedUser");

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/user?action=login");
            return;
        }

        int userId = user.getUserId();
        Cart cart = cartDAO.getCartByUserId(userId);

        if (cart == null) {
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
            return;
        }

        List<CartItem> cartItems = cartDAO.getCartItems(cart.getCartId());
        if (cartItems == null || cartItems.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/cart?action=view");
            return;
        }

        int restaurantId;
        try {
            restaurantId = Integer.parseInt(req.getParameter("restaurantId"));
        } catch (NumberFormatException e) {
            req.setAttribute("errorMessage", "Invalid restaurant.");
            doGet(req, resp);
            return;
        }

        // Normalize payment method to match DB ENUM: ('cash_on_delivery', 'card', 'upi')
        String rawPayment = req.getParameter("payment_method");
        String paymentMethod = "cash_on_delivery";
        if (rawPayment != null) {
            String lower = rawPayment.trim().toLowerCase();
            if (lower.contains("card")) paymentMethod = "card";
            else if (lower.contains("upi")) paymentMethod = "upi";
            else paymentMethod = "cash_on_delivery";
        }

        String deliveryInstructions = req.getParameter("instructions");

        String addressIdStr = req.getParameter("addressId");
        Integer addressId = null;
        if (addressIdStr != null && !addressIdStr.trim().isEmpty()) {
            try {
                addressId = Integer.parseInt(addressIdStr);
            } catch (NumberFormatException e) {
                req.setAttribute("errorMessage", "Invalid delivery address.");
                doGet(req, resp);
                return;
            }
            if (addressDAO.getAddressById(addressId, userId) == null) {
                req.setAttribute("errorMessage", "Select one of your saved delivery addresses.");
                doGet(req, resp);
                return;
            }
        }

        double totalAmount = cartDAO.getCartTotal(cart.getCartId());

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cartItems) {
            MenuItem mi = menuItemDAO.getMenuItemById(ci.getMenuItemId());
            if (mi == null || !mi.isAvailable() || mi.getRestaurantId() != restaurantId
                    || ci.getQuantity() <= 0) {
                req.setAttribute("errorMessage", "Your cart contains an unavailable or invalid item.");
                doGet(req, resp);
                return;
            }
            OrderItem oi = new OrderItem();
            oi.setMenuItemId(ci.getMenuItemId());
            oi.setQuantity(ci.getQuantity());
            oi.setPriceAtOrder(mi.getPrice());
            orderItems.add(oi);
        }

        if (orderItems.isEmpty() || totalAmount <= 0) {
            req.setAttribute("errorMessage", "Your cart cannot be checked out.");
            doGet(req, resp);
            return;
        }

        String idempotencyKey = req.getParameter("idempotencyKey");
        String sessionKey = (String) session.getAttribute("checkoutIdempotencyKey");
        if (idempotencyKey == null || !idempotencyKey.equals(sessionKey)) {
            req.setAttribute("errorMessage", "Your checkout session has expired. Please review the order and try again.");
            doGet(req, resp);
            return;
        }

        try {
            int orderId = orderService.placeOrder(
                    userId, restaurantId, addressId, paymentMethod,
                    deliveryInstructions, orderItems, totalAmount, idempotencyKey
            );

            // Cleanup user's cart
            cartDAO.clearCart(cart.getCartId());
            session.removeAttribute("checkoutIdempotencyKey");

            // Asynchronous notification delivery
            NotificationService.getInstance().notify(
                    userId,
                    "Order #" + orderId + " placed successfully! Total: ₹" + String.format("%.2f", totalAmount),
                    orderId
            );

            resp.sendRedirect(req.getContextPath() + "/orderSuccess?orderId=" + orderId);

        } catch (DuplicateCheckoutException e) {
            // Duplicate detected; redirect safely to existing order if available
            if (e.getExistingOrderId() > 0) {
                session.removeAttribute("checkoutIdempotencyKey");
                resp.sendRedirect(req.getContextPath() + "/orderSuccess?orderId=" + e.getExistingOrderId());
            } else {
                req.setAttribute("errorMessage", "Duplicate checkout detected.");
                doGet(req, resp);
            }
        } catch (AppException e) {
            req.setAttribute("errorMessage", e.getMessage());
            doGet(req, resp);
        }
    }
}
