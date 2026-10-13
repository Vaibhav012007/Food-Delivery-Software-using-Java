package com.app.controllers;

import com.app.dao.DeliveryBatchDAO;
import com.app.dao.OrderDAO;
import com.app.dao_implementation.DeliveryBatchDAOImpl;
import com.app.dao_implementation.OrderDAOImpl;
import com.app.models.*;
import com.app.service.DeliveryBatchingService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/adminBatch")
public class DeliveryBatchServlet extends HttpServlet {

    private DeliveryBatchDAO batchDAO;
    private OrderDAO orderDAO;
    private DeliveryBatchingService batchingService;

    @Override
    public void init() throws ServletException {
        batchDAO = new DeliveryBatchDAOImpl();
        orderDAO = new OrderDAOImpl();
        batchingService = new DeliveryBatchingService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req, resp)) return;

        List<DeliveryBatch> batches = batchDAO.getAllBatches(50);
        Map<Integer, List<Order>> batchOrders = new LinkedHashMap<>();
        for (DeliveryBatch b : batches) {
            batchOrders.put(b.getBatchId(), batchDAO.getOrdersInBatch(b.getBatchId()));
        }

        req.setAttribute("batches", batches);
        req.setAttribute("batchOrders", batchOrders);
        req.getRequestDispatcher("/jsp/admin/batches.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!isAdmin(req, resp)) return;

        String action = req.getParameter("action");
        if ("trigger".equals(action)) {
            int count = batchingService.runBatchingCycle();
            req.setAttribute("message", "Batching cycle run. Orders batched: " + count);
        } else if ("dispatch".equals(action)) {
            int batchId = Integer.parseInt(req.getParameter("batchId"));
            batchingService.dispatchBatch(batchId);
            req.setAttribute("message", "Batch #" + batchId + " dispatched.");
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
