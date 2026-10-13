package com.app.controllers;

import com.app.dao.MenuItemDAO;
import com.app.dao.RestaurantDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.exceptions.AppException;
import com.app.models.*;
import com.app.service.SurplusService;
import com.app.service.SurplusServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/surplus")
public class SurplusServlet extends HttpServlet {

    private SurplusService surplusService;
    private MenuItemDAO menuItemDAO;
    private RestaurantDAO restaurantDAO;

    @Override
    public void init() throws ServletException {
        surplusService = new SurplusServiceImpl();
        menuItemDAO = new MenuItemDAOImpl();
        restaurantDAO = new RestaurantDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) action = "browse";

        switch (action) {
            case "browse":
                browseSurplus(req, resp);
                break;
            case "myReservations":
                myReservations(req, resp);
                break;
            default:
                browseSurplus(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getLoggedUser(req, resp);
        if (user == null) return;

        String action = req.getParameter("action");
        try {
            switch (action != null ? action : "") {
                case "reserve": {
                    int listingId = Integer.parseInt(req.getParameter("listingId"));
                    int quantity = Integer.parseInt(req.getParameter("quantity"));
                    int reservationId = surplusService.reserveSurplus(listingId, user.getUserId(), quantity);
                    req.setAttribute("message", "Reservation confirmed! Reservation #" + reservationId);
                    browseSurplus(req, resp);
                    break;
                }
                case "cancel": {
                    int reservationId = Integer.parseInt(req.getParameter("reservationId"));
                    surplusService.cancelReservation(reservationId, user.getUserId());
                    req.setAttribute("message", "Reservation cancelled successfully.");
                    myReservations(req, resp);
                    break;
                }
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (AppException e) {
            req.setAttribute("error", e.getMessage());
            browseSurplus(req, resp);
        }
    }

    private void browseSurplus(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<SurplusListing> listings = surplusService.getActiveListings();
        Set<Integer> menuItemIds = new HashSet<>();
        Set<Integer> restaurantIds = new HashSet<>();
        for (SurplusListing l : listings) {
            menuItemIds.add(l.getMenuItemId());
            restaurantIds.add(l.getRestaurantId());
        }
        Map<Integer, MenuItem> menuItems = menuItemDAO.getMenuItemsAsMap(menuItemIds);
        Map<Integer, Restaurant> restaurants = new HashMap<>();
        for (int rid : restaurantIds) {
            Restaurant r = restaurantDAO.getRestaurantById(rid);
            if (r != null) restaurants.put(rid, r);
        }
        req.setAttribute("listings", listings);
        req.setAttribute("menuItems", menuItems);
        req.setAttribute("restaurants", restaurants);
        req.getRequestDispatcher("/jsp/customer/surplus.jsp").forward(req, resp);
    }

    private void myReservations(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getLoggedUser(req, resp);
        if (user == null) return;
        List<SurplusReservation> reservations = surplusService.getMyReservations(user.getUserId());
        req.setAttribute("reservations", reservations);
        req.getRequestDispatcher("/jsp/customer/surplus.jsp").forward(req, resp);
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
