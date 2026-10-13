<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Manage Orders" />
<style>
.tab-btn {
    padding: 8px 16px;
    border-radius: 6px;
    border: 1px solid var(--border);
    background: white;
    text-decoration: none;
    color: var(--dark-text);
    font-size: 0.9rem;
}
.tab-btn.active {
    background: var(--primary);
    color: white;
    border-color: var(--primary);
}
.order-table {
    width: 100%;
    border-collapse: collapse;
    background: white;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.order-table th, .order-table td {
    padding: 12px;
    text-align: left;
    border-bottom: 1px solid var(--border);
}
.order-table th {
    background: var(--light-bg);
    font-weight: 600;
}
.status-pill {
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 0.8rem;
    font-weight: 600;
    text-transform: uppercase;
}
.status-placed { background: #e0f2fe; color: #0369a1; }
.status-accepted { background: #fef3c7; color: #b45309; }
.status-preparing { background: #fed7aa; color: #c2410c; }
.status-ready_for_pickup { background: #dcfce7; color: #15803d; }
.status-out_for_delivery { background: #e0e7ff; color: #4338ca; }
.status-delivered { background: #d1fae5; color: #047857; }
.status-cancelled { background: #fee2e2; color: #b91c1c; }
</style>
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <h1 class="page-title">Manage Orders</h1>

  <c:if test="${not empty error}">
    <div class="alert alert--error mb-3">${error}</div>
  </c:if>
  <c:if test="${not empty message}">
    <div class="alert alert--success mb-3">${message}</div>
  </c:if>

  <div style="display:flex;gap:8px;margin-bottom:20px;flex-wrap:wrap">
    <a href="${pageContext.request.contextPath}/adminOrder" class="tab-btn ${empty statusFilter ? 'active' : ''}">All Active</a>
    <a href="${pageContext.request.contextPath}/adminOrder?status=placed" class="tab-btn ${statusFilter eq 'placed' ? 'active' : ''}">Placed</a>
    <a href="${pageContext.request.contextPath}/adminOrder?status=accepted" class="tab-btn ${statusFilter eq 'accepted' ? 'active' : ''}">Accepted</a>
    <a href="${pageContext.request.contextPath}/adminOrder?status=preparing" class="tab-btn ${statusFilter eq 'preparing' ? 'active' : ''}">Preparing</a>
    <a href="${pageContext.request.contextPath}/adminOrder?status=ready_for_pickup" class="tab-btn ${statusFilter eq 'ready_for_pickup' ? 'active' : ''}">Ready For Pickup</a>
    <a href="${pageContext.request.contextPath}/adminOrder?status=delivered" class="tab-btn ${statusFilter eq 'delivered' ? 'active' : ''}">Delivered</a>
    <a href="${pageContext.request.contextPath}/adminOrder?status=cancelled" class="tab-btn ${statusFilter eq 'cancelled' ? 'active' : ''}">Cancelled</a>
  </div>

  <c:choose>
    <c:when test="${empty orders}">
      <div class="card" style="text-align:center;padding:40px">
        <p>No orders found for this filter.</p>
      </div>
    </c:when>
    <c:otherwise>
      <table class="order-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Restaurant</th>
            <th>User</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Date</th>
            <th>Update Status</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="o" items="${orders}">
            <tr>
              <td>#${o.orderId}</td>
              <td>${restaurantNames[o.restaurantId]}</td>
              <td>User #${o.userId}</td>
              <td>₹<fmt:formatNumber value="${o.totalAmount}" maxFractionDigits="2"/></td>
              <td><span class="status-pill status-${o.status}">${o.status}</span></td>
              <td><fmt:formatDate value="${o.createdAt}" pattern="dd MMM, hh:mm a"/></td>
              <td>
                <form method="post" action="${pageContext.request.contextPath}/adminOrder" style="display:flex;gap:6px;align-items:center">
                  <input type="hidden" name="action" value="updateStatus">
                  <input type="hidden" name="orderId" value="${o.orderId}">
                  <select name="newStatus" style="padding:4px 8px;border-radius:4px;border:1px solid var(--border)">
                    <option value="accepted" ${o.status eq 'accepted' ? 'selected' : ''}>accepted</option>
                    <option value="preparing" ${o.status eq 'preparing' ? 'selected' : ''}>preparing</option>
                    <option value="ready_for_pickup" ${o.status eq 'ready_for_pickup' ? 'selected' : ''}>ready_for_pickup</option>
                    <option value="out_for_delivery" ${o.status eq 'out_for_delivery' ? 'selected' : ''}>out_for_delivery</option>
                    <option value="delivered" ${o.status eq 'delivered' ? 'selected' : ''}>delivered</option>
                    <option value="cancelled" ${o.status eq 'cancelled' ? 'selected' : ''}>cancelled</option>
                  </select>
                  <button type="submit" class="btn btn--primary" style="padding:4px 10px;font-size:0.85rem">Update</button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>

</div>
</main>
<%@ include file="/jsp/shared/footer.jspf" %>
</body>
</html>
