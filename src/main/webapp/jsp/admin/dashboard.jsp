<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="../shared/head.jspf" %>
<c:set var="pageTitle" value="Admin Dashboard" />
</head>
<body>
<%@ include file="../shared/header.jspf" %>
<div class="container main-content">
  <h1 class="page-title">Admin Dashboard</h1>
  <div class="grid-3">
    <a class="card" href="${pageContext.request.contextPath}/restaurant?action=list" style="display:block;padding:20px;text-align:center">
      <div style="font-size:2rem">🍽️</div>
      <h3 class="mt-2">Restaurants</h3>
      <p>Manage restaurants</p>
    </a>
    <a class="card" href="${pageContext.request.contextPath}/adminOrder" style="display:block;padding:20px;text-align:center">
      <div style="font-size:2rem">📦</div>
      <h3 class="mt-2">Orders</h3>
      <p>View & manage all orders</p>
    </a>
    <a class="card" href="${pageContext.request.contextPath}/adminBatch" style="display:block;padding:20px;text-align:center">
      <div style="font-size:2rem">🚚</div>
      <h3 class="mt-2">Delivery Batches</h3>
      <p>Batch & dispatch orders</p>
    </a>
    <a class="card" href="${pageContext.request.contextPath}/adminSurplus" style="display:block;padding:20px;text-align:center">
      <div style="font-size:2rem">♻️</div>
      <h3 class="mt-2">Surplus Food</h3>
      <p>Manage surplus listings</p>
    </a>
    <a class="card" href="${pageContext.request.contextPath}/jsp/admin/restaurantListAdmin.jsp" style="display:block;padding:20px;text-align:center">
      <div style="font-size:2rem">🏪</div>
      <h3 class="mt-2">Restaurant CRUD</h3>
      <p>Add/edit restaurants</p>
    </a>
    <a class="card" href="${pageContext.request.contextPath}/jsp/admin/menuListAdmin.jsp" style="display:block;padding:20px;text-align:center">
      <div style="font-size:2rem">🍱</div>
      <h3 class="mt-2">Menu CRUD</h3>
      <p>Add/edit menu items</p>
    </a>
  </div>
  <div class="card mt-4" style="padding:16px">
    <h3>Quick Actions</h3>
    <p>Logged in as: <strong>${sessionScope.loggedUser.name}</strong> (${sessionScope.loggedUser.role})</p>
    <a href="${pageContext.request.contextPath}/user?action=logout" class="btn btn--secondary mt-2">Logout</a>
  </div>
</div>
<%@ include file="../shared/footer.jspf" %>
</body>
</html>
