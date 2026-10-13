package com.app.controllers;

import java.io.IOException;
import java.util.List;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.app.dao.MenuItemDAO;
import com.app.dao.RestaurantDAO;
import com.app.dao_implementation.MenuItemDAOImpl;
import com.app.dao_implementation.RestaurantDAOImpl;
import com.app.models.MenuItem;
import com.app.models.Restaurant;


@WebServlet("/menu")
public class MenuServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MenuItemDAO menuItemDAO = new MenuItemDAOImpl();
    private RestaurantDAO restaurantDAO = new RestaurantDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Validate restaurantId
        String idParam = request.getParameter("restaurantId");
        if (idParam == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing restaurantId");
            return;
        }

        int restaurantId;
        try {
            restaurantId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid restaurantId");
            return;
        }

        // Fetch restaurant details
        Restaurant restaurant = restaurantDAO.getRestaurantById(restaurantId);
        if (restaurant == null || !restaurant.isActive()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Restaurant not found");
            return;
        }

        // Fetch menu items for this restaurant
        List<MenuItem> menuItems = menuItemDAO.getMenuItemsByRestaurantId(restaurantId);

        // Pass data to JSP
        request.setAttribute("restaurant", restaurant);
        request.setAttribute("menuItems", menuItems);

        RequestDispatcher rd = request.getRequestDispatcher(
                "/jsp/customer/restaurantDetails.jsp");
        rd.forward(request, response);
    }
}

/*
===========================================================
 MenuServlet – JSP Mappings (Customer-Facing)
===========================================================

CUSTOMER URL:
----------------------------------------------
GET /menu?restaurantId={id}
    → /WEB-INF/jsp/customer/menu.jsp


REQUIRED JSP FILES:
----------------------------------------------
📁 /WEB-INF/jsp/customer/menu.jsp


DATA PASSED TO JSP:
----------------------------------------------
restaurant       → Restaurant object (name, rating, etc.)
menuItems        → List<MenuItem> for that restaurant


NOTES:
----------------------------------------------
✔ This servlet is public → no login required.
✔ Only fetches & displays data (NO CRUD).
✔ Uses MenuItemDAO → getMenuItemsByRestaurantId()
✔ Uses RestaurantDAO → getRestaurantById()
✔ MenuItem POJO fields correctly mapped:
      name, description, price, isVeg, isAvailable, imageUrl
===========================================================
*/

