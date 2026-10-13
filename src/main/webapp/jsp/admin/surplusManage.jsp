<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Manage Surplus Listings" />
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <h1 class="page-title">Manage Surplus Food</h1>

  <c:if test="${not empty error}">
    <div class="alert alert--error mb-3">${error}</div>
  </c:if>
  <c:if test="${not empty message}">
    <div class="alert alert--success mb-3">${message}</div>
  </c:if>

  <!-- Create New Listing -->
  <div class="card mb-4" style="max-width:700px">
    <h3>Create Surplus Listing</h3>
    <form method="post" action="${pageContext.request.contextPath}/adminSurplus" class="mt-3">
      <input type="hidden" name="action" value="create">
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div>
          <label><strong>Restaurant</strong></label>
          <select name="restaurantId" required style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
            <c:forEach var="r" items="${restaurants}">
              <option value="${r.restaurantId}">${r.name}</option>
            </c:forEach>
          </select>
        </div>
        <div>
          <label><strong>Menu Item ID</strong></label>
          <input type="number" name="menuItemId" placeholder="e.g. 1" required
            style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Quantity Available</strong></label>
          <input type="number" name="quantity" min="1" value="10" required
            style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Original Price (₹)</strong></label>
          <input type="number" step="0.01" name="originalPrice" placeholder="250.00" required
            style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Discounted Price (₹)</strong></label>
          <input type="number" step="0.01" name="discountedPrice" placeholder="100.00" required
            style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Expires in (Minutes)</strong></label>
          <input type="number" name="expiryMinutes" min="15" value="120" required
            style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
      </div>
      <button type="submit" class="btn btn--primary mt-3">Post Surplus Listing</button>
    </form>
  </div>

  <!-- Active Listings -->
  <h3>Active Surplus Listings</h3>
  <c:choose>
    <c:when test="${empty listings}">
      <p style="color:var(--medium-text)" class="mt-2">No active surplus listings.</p>
    </c:when>
    <c:otherwise>
      <div class="grid-3 mt-3">
        <c:forEach var="l" items="${listings}">
          <c:set var="mi" value="${menuItems[l.menuItemId]}"/>
          <c:set var="rest" value="${restaurantMap[l.restaurantId]}"/>
          <div class="card">
            <h4>Listing #${l.listingId}: ${mi != null ? mi.name : ('Item #' + l.menuItemId)}</h4>
            <p style="color:var(--medium-text);font-size:0.9rem">${rest != null ? rest.name : ('Restaurant #' + l.restaurantId)}</p>
            <p class="mt-2">Available: <strong>${l.availableQuantity} units</strong></p>
            <p>Original: ₹${l.originalPrice} | Discount: <strong>₹${l.discountedPrice}</strong></p>
            <p style="font-size:0.85rem;color:var(--medium-text)">
              Expires: <fmt:formatDate value="${l.expiryTime}" pattern="dd MMM, hh:mm a"/>
            </p>
            <p>Status: <strong>${l.status}</strong></p>
          </div>
        </c:forEach>
      </div>
    </c:otherwise>
  </c:choose>

</div>
</main>
<%@ include file="/jsp/shared/footer.jspf" %>
</body>
</html>
