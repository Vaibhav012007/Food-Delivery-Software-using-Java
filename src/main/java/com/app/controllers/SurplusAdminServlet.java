package com.app.controllers;

import com.app.dao.MenuItemDAO;
import com.app.dao.RestaurantDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.exceptions.AppException;
import com.app.models.MenuItem;
import com.app.models.Restaurant;
import com.app.models.SurplusListing;
import com.app.models.User;
import com.app.service.SurplusService;
import com.app.service.SurplusServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

@WebServlet("/adminSurplus")
public class SurplusAdminServlet extends HttpServlet {

    private SurplusService surplusService;
    private RestaurantDAO restaurantDAO;
    private MenuItemDAO menuItemDAO;

    @Override
    public void init() throws ServletException {
        surplusService = new SurplusServiceImpl();
        restaurantDAO = new RestaurantDAOImpl();
        menuItemDAO = new MenuItemDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req, resp)) return;

        List<SurplusListing> listings = surplusService.getActiveListings();
        List<Restaurant> restaurants = restaurantDAO.getAllRestaurants();

        Set<Integer> menuItemIds = new HashSet<>();
        for (SurplusListing l : listings) {
            menuItemIds.add(l.getMenuItemId());
        }
        Map<Integer, MenuItem> menuItems = menuItemDAO.getMenuItemsAsMap(menuItemIds);

        Map<Integer, Restaurant> restaurantMap = new HashMap<>();
        for (Restaurant r : restaurants) {
            restaurantMap.put(r.getRestaurantId(), r);
        }

        req.setAttribute("listings", listings);
        req.setAttribute("restaurants", restaurants);
        req.setAttribute("menuItems", menuItems);
        req.setAttribute("restaurantMap", restaurantMap);
        req.getRequestDispatcher("/jsp/admin/surplusManage.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req, resp)) return;

        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                int restaurantId = Integer.parseInt(req.getParameter("restaurantId"));
                int menuItemId = Integer.parseInt(req.getParameter("menuItemId"));
                int quantity = Integer.parseInt(req.getParameter("quantity"));
                double originalPrice = Double.parseDouble(req.getParameter("originalPrice"));
                double discountedPrice = Double.parseDouble(req.getParameter("discountedPrice"));
                int expiryMinutes = Integer.parseInt(req.getParameter("expiryMinutes"));

                Timestamp expiryTime = new Timestamp(System.currentTimeMillis() + (long) expiryMinutes * 60 * 1000);
                int listingId = surplusService.createListing(restaurantId, menuItemId, quantity,
                        originalPrice, discountedPrice, expiryTime);

                req.setAttribute("message", "Surplus listing #" + listingId + " created successfully.");
            }
        } catch (AppException e) {
            req.setAttribute("error", e.getMessage());
        } catch (Exception e) {
            req.setAttribute("error", "Error processing request: " + e.getMessage());
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
