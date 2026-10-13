<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Surplus Marketplace" />
<style>
.badge-discount {
    background-color: var(--accent-green);
    color: white;
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 0.85rem;
    font-weight: 600;
}
.price-strike {
    text-decoration: line-through;
    color: var(--medium-text);
    margin-right: 6px;
}
.price-highlight {
    color: var(--primary);
    font-size: 1.25rem;
    font-weight: 700;
}
</style>
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <h1 class="page-title">Surplus Food Marketplace</h1>
  <p class="mb-4" style="color:var(--medium-text)">
    Rescue delicious surplus meals at steep discounts while reducing campus food waste!
  </p>

  <c:if test="${not empty error}">
    <div class="alert alert--error mb-3">${error}</div>
  </c:if>
  <c:if test="${not empty message}">
    <div class="alert alert--success mb-3">${message}</div>
  </c:if>

  <div style="display:flex;gap:12px;margin-bottom:24px">
    <a href="${pageContext.request.contextPath}/surplus?action=browse" class="btn btn--primary">Browse Listings</a>
    <a href="${pageContext.request.contextPath}/surplus?action=myReservations" class="btn btn--secondary">My Reservations</a>
  </div>

  <c:choose>
    <c:when test="${not empty reservations}">
      <h3 class="mb-3">My Reservations</h3>
      <div class="grid-3">
        <c:forEach var="res" items="${reservations}">
          <div class="card">
            <h4>Reservation #${res.reservationId}</h4>
            <p>Quantity: <strong>${res.quantity}</strong></p>
            <p>Total: <strong>₹<fmt:formatNumber value="${res.totalPrice}" maxFractionDigits="2"/></strong></p>
            <p>Status: <span class="badge-discount">${res.status}</span></p>
            <p style="font-size:0.85rem;color:var(--medium-text)">
              Expires: <fmt:formatDate value="${res.expiresAt}" pattern="dd MMM hh:mm a"/>
            </p>
            <c:if test="${res.status eq 'reserved'}">
              <form method="post" action="${pageContext.request.contextPath}/surplus" class="mt-2">
                <input type="hidden" name="action" value="cancel">
                <input type="hidden" name="reservationId" value="${res.reservationId}">
                <button type="submit" class="btn btn--secondary" onclick="return confirm('Cancel this reservation?')">Cancel</button>
              </form>
            </c:if>
          </div>
        </c:forEach>
      </div>
    </c:when>

    <c:when test="${empty listings}">
      <div class="card" style="text-align:center;padding:40px">
        <h3>No Surplus Food Listings Right Now</h3>
        <p style="color:var(--medium-text)" class="mt-2">Check back later or explore our regular menus!</p>
        <a href="${pageContext.request.contextPath}/restaurant?action=list" class="btn btn--primary mt-3">Explore Restaurants</a>
      </div>
    </c:when>

    <c:otherwise>
      <div class="grid-3">
        <c:forEach var="l" items="${listings}">
          <c:set var="mi" value="${menuItems[l.menuItemId]}"/>
          <c:set var="r" value="${restaurants[l.restaurantId]}"/>
          <div class="card">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
              <span class="badge-discount"><fmt:formatNumber value="${l.discountPercentage}" maxFractionDigits="0"/>% OFF</span>
              <span style="font-size:0.85rem;color:var(--medium-text)">
                Ends: <fmt:formatDate value="${l.expiryTime}" pattern="hh:mm a"/>
              </span>
            </div>

            <h4>${mi.name}</h4>
            <p style="color:var(--medium-text);font-size:0.9rem">${r.name}</p>

            <div class="mt-2 mb-3">
              <span class="price-strike">₹${l.originalPrice}</span>
              <span class="price-highlight">₹${l.discountedPrice}</span>
              <span style="font-size:0.85rem;color:var(--medium-text);margin-left:8px">(${l.availableQuantity} left)</span>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/surplus">
              <input type="hidden" name="action" value="reserve">
              <input type="hidden" name="listingId" value="${l.listingId}">
              <div style="display:flex;gap:8px;align-items:center">
                <input type="number" name="quantity" value="1" min="1" max="${l.availableQuantity}"
                  style="width:70px;padding:6px;border-radius:6px;border:1px solid var(--border)">
                <button type="submit" class="btn btn--primary" style="flex:1">Reserve Now</button>
              </div>
            </form>
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
