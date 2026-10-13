<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="pageTitle" value="Order Details" />
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/jsp/shared/head.jspf" %>
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
    <div class="container">
        <div class="card mb-4"><div class="card__body">
            <h1 class="page-title">Order #${order.orderId}</h1>
            <p class="mb-1"><strong>Status:</strong> ${order.status}</p>
            <p class="mb-1"><strong>Placed:</strong> <fmt:formatDate value="${order.createdAt}" pattern="dd MMM yyyy, hh:mm a" /></p>
            <p class="mb-1"><strong>Payment:</strong> ${order.paymentMethod} (${order.paymentStatus})</p>
            <c:if test="${not empty order.deliveryInstructions}">
                <p class="mb-1"><strong>Delivery instructions:</strong> ${order.deliveryInstructions}</p>
            </c:if>
        </div></div>

        <div class="card mb-4"><div class="card__body">
            <h2 class="card__title">Items</h2>
            <c:forEach var="item" items="${items}">
                <c:set var="menu" value="${menuItemsMap[item.menuItemId]}" />
                <div style="display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid var(--border);">
                    <c:choose>
                        <c:when test="${not empty menu.imageUrl and (fn:startsWith(menu.imageUrl, 'http://') or fn:startsWith(menu.imageUrl, 'https://'))}">
                            <img src="${menu.imageUrl}" alt="${menu.name}" style="width:64px;height:64px;object-fit:cover;border-radius:8px;"
                                 onerror="this.onerror=null;this.src='${pageContext.request.contextPath}/assets/images/default_image.png';" />
                        </c:when>
                        <c:otherwise>
                            <img src="${pageContext.request.contextPath}/${empty menu.imageUrl ? 'assets/images/default_image.png' : menu.imageUrl}"
                                 alt="${empty menu.name ? 'Menu item' : menu.name}" style="width:64px;height:64px;object-fit:cover;border-radius:8px;"
                                 onerror="this.onerror=null;this.src='${pageContext.request.contextPath}/assets/images/default_image.png';" />
                        </c:otherwise>
                    </c:choose>
                    <div style="flex:1;"><strong>${empty menu.name ? 'Menu item' : menu.name}</strong><br>
                        <span>${item.quantity} × ₹<fmt:formatNumber value="${item.priceAtOrder}" minFractionDigits="2" maxFractionDigits="2" /></span></div>
                    <strong>₹<fmt:formatNumber value="${item.priceAtOrder * item.quantity}" minFractionDigits="2" maxFractionDigits="2" /></strong>
                </div>
            </c:forEach>
            <div style="display:flex;justify-content:space-between;padding-top:16px;font-size:1.1rem;">
                <strong>Total</strong><strong>₹<fmt:formatNumber value="${order.totalAmount}" minFractionDigits="2" maxFractionDigits="2" /></strong>
            </div>
        </div></div>
        <a href="${pageContext.request.contextPath}/orderHistory" class="btn btn--secondary">Back to My Orders</a>
        <a href="${pageContext.request.contextPath}/restaurant?action=list" class="btn btn--primary">Order Again</a>
    </div>
</main>
<%@ include file="/jsp/shared/footer.jspf" %>
</body>
</html>
