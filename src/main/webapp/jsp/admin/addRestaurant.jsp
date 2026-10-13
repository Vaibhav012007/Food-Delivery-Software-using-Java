<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Add Restaurant" />
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <h1 class="page-title">Add New Restaurant</h1>

  <div class="card" style="max-width:700px">
    <form method="post" action="${pageContext.request.contextPath}/admin/restaurants">
      <input type="hidden" name="action" value="save">
      
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
        <div>
          <label><strong>Name</strong></label>
          <input type="text" name="name" required style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Cuisine Type</strong></label>
          <input type="text" name="cuisineType" required placeholder="e.g. Indian, Chinese" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div style="grid-column: span 2">
          <label><strong>Description</strong></label>
          <textarea name="description" rows="2" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px"></textarea>
        </div>
        <div>
          <label><strong>Address</strong></label>
          <input type="text" name="address" required style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>City</strong></label>
          <input type="text" name="city" required value="Campus" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>State</strong></label>
          <input type="text" name="state" value="State" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Zip</strong></label>
          <input type="text" name="zip" value="123456" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Phone</strong></label>
          <input type="text" name="phone" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Email</strong></label>
          <input type="email" name="email" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Rating (0-5)</strong></label>
          <input type="number" step="0.1" min="0" max="5" name="rating" value="4.0" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label><strong>Est. Delivery Time (mins)</strong></label>
          <input type="number" name="deliveryTime" value="30" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div style="grid-column: span 2">
          <label><strong>Image URL</strong></label>
          <input type="text" name="imageUrl" placeholder="https://example.com/image.jpg" style="width:100%;padding:8px;border-radius:6px;border:1px solid var(--border);margin-top:4px">
        </div>
        <div>
          <label>
            <input type="checkbox" name="isActive" value="true" checked> Active
          </label>
        </div>
      </div>

      <div class="mt-4" style="display:flex;gap:12px">
        <button type="submit" class="btn btn--primary">Save Restaurant</button>
        <a href="${pageContext.request.contextPath}/admin/restaurants" class="btn btn--secondary">Cancel</a>
      </div>
    </form>
  </div>

</div>
</main>
<%@ include file="/jsp/shared/footer.jspf" %>
</body>
</html>