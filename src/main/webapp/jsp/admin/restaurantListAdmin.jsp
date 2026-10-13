<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Manage Restaurants" />
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;flex-wrap:wrap;gap:12px">
    <h1 class="page-title" style="margin:0">Restaurant Management</h1>
    <a href="${pageContext.request.contextPath}/admin/restaurants?action=add" class="btn btn--primary">+ Add New Restaurant</a>
  </div>

  <c:choose>
    <c:when test="${empty restaurants}">
      <div class="card" style="text-align:center;padding:40px">
        <p>No restaurants found.</p>
        <a href="${pageContext.request.contextPath}/admin/restaurants?action=add" class="btn btn--primary mt-3">Add First Restaurant</a>
      </div>
    </c:when>
    <c:otherwise>
      <div class="grid-3">
        <c:forEach var="r" items="${restaurants}">
          <div class="card">
            <h3>${r.name}</h3>
            <p style="color:var(--medium-text);font-size:0.9rem">${r.cuisineType} | Rating: ${r.rating} ⭐</p>
            <p class="mt-2" style="font-size:0.9rem">${r.address}, ${r.city}</p>
            <p style="font-size:0.85rem;color:var(--medium-text)">Estimated Delivery: ${r.deliveryTime} mins</p>
            <div style="display:flex;gap:8px;margin-top:16px;flex-wrap:wrap">
              <a href="${pageContext.request.contextPath}/admin/menu?action=list&restaurantId=${r.restaurantId}" class="btn btn--secondary" style="font-size:0.85rem;padding:6px 12px">Manage Menu</a>
              <a href="${pageContext.request.contextPath}/admin/restaurants?action=edit&id=${r.restaurantId}" class="btn btn--secondary" style="font-size:0.85rem;padding:6px 12px">Edit</a>
              <a href="${pageContext.request.contextPath}/admin/restaurants?action=delete&id=${r.restaurantId}" class="btn btn--secondary" style="font-size:0.85rem;padding:6px 12px;color:var(--accent-red)" onclick="return confirm('Delete this restaurant?')">Delete</a>
            </div>
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
