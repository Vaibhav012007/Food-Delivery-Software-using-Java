<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Edit Menu Item" />
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <h1 class="page-title">Edit Menu Item #${menuItem.menuItemId}</h1>

  <div class="card" style="max-width:600px">
    <form method="post" action="${pageContext.request.contextPath}/admin/menu">
      <input type="hidden" name="action" value="update">
      <input type="hidden" name="menuItemId" value="${menuItem.menuItemId}">
      <input type="hidden" name="restaurantId" value="${menuItem.restaurantId}">

      <div class="mb-3">
        <label><strong>Item Name</strong></label>
        <input type="text" name="name" value="${menuItem.name}" required style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
      </div>

      <div class="mb-3">
        <label><strong>Description</strong></label>
        <textarea name="description" rows="2" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">${menuItem.description}</textarea>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px" class="mb-3">
        <div>
          <label><strong>Price (₹)</strong></label>
          <input type="number" step="0.01" name="price" value="${menuItem.price}" required style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Category</strong></label>
          <input type="text" name="category" value="${menuItem.category}" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
      </div>

      <div class="mb-3">
        <label><strong>Image URL</strong></label>
        <input type="text" name="imageUrl" value="${menuItem.imageUrl}" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
      </div>

      <div class="mb-3" style="display:flex;gap:24px">
        <label>
          <input type="checkbox" name="isVeg" value="veg" ${menuItem.veg ? 'checked' : ''}> Vegetarian
        </label>
        <label>
          <input type="checkbox" name="isAvailable" value="available" ${menuItem.available ? 'checked' : ''}> Available
        </label>
      </div>

      <div class="mt-4" style="display:flex;gap:12px">
        <button type="submit" class="btn btn--primary">Update Item</button>
        <a href="${pageContext.request.contextPath}/admin/menu?action=list&restaurantId=${menuItem.restaurantId}" class="btn btn--secondary">Cancel</a>
      </div>
    </form>
  </div>

</div>
</main>
<%@ include file="/jsp/shared/footer.jspf" %>
</body>
</html>