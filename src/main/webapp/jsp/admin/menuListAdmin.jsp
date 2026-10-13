<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Manage Menu Items" />
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;flex-wrap:wrap;gap:12px">
    <h1 class="page-title" style="margin:0">Menu Management</h1>
    <div>
      <a href="${pageContext.request.contextPath}/admin/menu?action=add&restaurantId=${restaurantId}" class="btn btn--primary">+ Add Menu Item</a>
      <a href="${pageContext.request.contextPath}/admin/restaurants" class="btn btn--secondary">Back to Restaurants</a>
    </div>
  </div>

  <c:choose>
    <c:when test="${empty menuItems}">
      <div class="card" style="text-align:center;padding:40px">
        <p>No menu items found for this restaurant.</p>
        <a href="${pageContext.request.contextPath}/admin/menu?action=add&restaurantId=${restaurantId}" class="btn btn--primary mt-3">Add First Menu Item</a>
      </div>
    </c:when>
    <c:otherwise>
      <div class="grid-3">
        <c:forEach var="item" items="${menuItems}">
          <div class="card">
            <div style="display:flex;justify-content:space-between;align-items:flex-start">
              <h3>${item.name}</h3>
              <span style="font-size:1.1rem">${item.veg ? '🟢' : '🔴'}</span>
            </div>
            <p style="color:var(--medium-text);font-size:0.9rem">${item.category}</p>
            <p class="mt-2" style="font-size:0.9rem">${item.description}</p>
            <p class="mt-2">Price: <strong>₹<fmt:formatNumber value="${item.price}" maxFractionDigits="2"/></strong></p>
            <p style="font-size:0.85rem;color:var(--medium-text)">
              Status: ${item.available ? '<span style=\"color:var(--accent-green)\">Available</span>' : '<span style=\"color:var(--accent-red)\">Unavailable</span>'}
            </p>
            <div style="display:flex;gap:8px;margin-top:16px">
              <a href="${pageContext.request.contextPath}/admin/menu?action=edit&id=${item.menuItemId}" class="btn btn--secondary" style="font-size:0.85rem;padding:6px 12px">Edit</a>
              <a href="${pageContext.request.contextPath}/admin/menu?action=delete&id=${item.menuItemId}" class="btn btn--secondary" style="font-size:0.85rem;padding:6px 12px;color:var(--accent-red)" onclick="return confirm('Delete this menu item?')">Delete</a>
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
