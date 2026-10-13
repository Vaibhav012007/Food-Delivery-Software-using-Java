<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Group Order" />
<style>
.invite-code {
    font-size: 1.8rem;
    font-weight: 700;
    letter-spacing: 0.3rem;
    color: var(--primary);
    background: var(--primary-light);
    padding: 12px 24px;
    border-radius: 8px;
    display: inline-block;
    margin: 8px 0;
}
.member-section {
    border-left: 4px solid var(--primary);
    padding-left: 12px;
    margin-bottom: 16px;
}
.group-status-open { color: var(--accent-green); font-weight: 600; }
.group-status-closed { color: var(--accent-red); font-weight: 600; }
</style>
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <c:if test="${not empty error}">
    <div class="alert alert--error mb-3">${error}</div>
  </c:if>
  <c:if test="${not empty message}">
    <div class="alert alert--success mb-3">${message}</div>
  </c:if>

  <!-- ===== LIST VIEW ===== -->
  <c:if test="${formAction eq 'list'}">
    <h1 class="page-title">Campus Group Orders</h1>
    <div style="display:flex;gap:12px;margin-bottom:20px">
      <a href="${pageContext.request.contextPath}/groupOrder?action=create" class="btn btn--primary">+ Create Group Order</a>
      <a href="${pageContext.request.contextPath}/groupOrder?action=join" class="btn btn--secondary">Join by Code</a>
    </div>
    <c:choose>
      <c:when test="${empty openGroups}">
        <div class="card" style="text-align:center;padding:40px">
          <p>No open group orders right now.</p>
          <a href="${pageContext.request.contextPath}/groupOrder?action=create" class="btn btn--primary mt-3">Start One!</a>
        </div>
      </c:when>
      <c:otherwise>
        <div class="grid-3">
          <c:forEach var="g" items="${openGroups}">
            <div class="card">
              <h4>Group #${g.groupOrderId}</h4>
              <p>Code: <strong>${g.inviteCode}</strong></p>
              <p>Deadline: <fmt:formatDate value="${g.deadline}" pattern="dd MMM hh:mm a"/></p>
              <a href="${pageContext.request.contextPath}/groupOrder?action=view&id=${g.groupOrderId}" class="btn btn--primary mt-2">View / Join</a>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </c:if>

  <!-- ===== CREATE FORM ===== -->
  <c:if test="${formAction eq 'create'}">
    <h1 class="page-title">Create Group Order</h1>
    <div class="card" style="max-width:500px">
      <form method="post" action="${pageContext.request.contextPath}/groupOrder">
        <input type="hidden" name="action" value="create">
        <div class="mb-3">
          <label><strong>Select Restaurant</strong></label>
          <select name="restaurantId" required style="width:100%;padding:8px;margin-top:4px;border-radius:8px;border:1px solid var(--border)">
            <c:forEach var="r" items="${restaurants}">
              <option value="${r.restaurantId}">${r.name} (${r.cuisineType})</option>
            </c:forEach>
          </select>
        </div>
        <div class="mb-3">
          <label><strong>Order Deadline (minutes from now)</strong></label>
          <input type="number" name="deadlineMinutes" value="60" min="10" max="240"
            style="width:100%;padding:8px;margin-top:4px;border-radius:8px;border:1px solid var(--border)">
        </div>
        <button type="submit" class="btn btn--primary">Create Group</button>
        <a href="${pageContext.request.contextPath}/groupOrder" class="btn btn--secondary">Cancel</a>
      </form>
    </div>
  </c:if>

  <!-- ===== JOIN FORM ===== -->
  <c:if test="${formAction eq 'join'}">
    <h1 class="page-title">Join Group Order</h1>
    <div class="card" style="max-width:400px">
      <form method="post" action="${pageContext.request.contextPath}/groupOrder">
        <input type="hidden" name="action" value="join">
        <div class="mb-3">
          <label><strong>Enter Invite Code</strong></label>
          <input type="text" name="inviteCode" placeholder="e.g. ABCD1234" required maxlength="10"
            style="width:100%;padding:8px;margin-top:4px;border-radius:8px;border:1px solid var(--border);text-transform:uppercase">
        </div>
        <button type="submit" class="btn btn--primary">Join Group</button>
      </form>
    </div>
  </c:if>

  <!-- ===== VIEW GROUP ===== -->
  <c:if test="${formAction eq 'view'}">
    <h1 class="page-title">Group Order #${group.groupOrderId}</h1>

    <!-- Group Header -->
    <div class="card mb-4">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px">
        <div>
          <p>Status: <span class="${group.status eq 'open' ? 'group-status-open' : 'group-status-closed'}">${group.status}</span></p>
          <p>Deadline: <fmt:formatDate value="${group.deadline}" pattern="dd MMM yyyy hh:mm a"/></p>
          <p>Members: ${members.size()}</p>
        </div>
        <div style="text-align:right">
          <p>Invite Code:</p>
          <div class="invite-code">${group.inviteCode}</div>
          <p style="font-size:12px;color:var(--medium-text)">Share this code with friends!</p>
        </div>
      </div>

      <c:if test="${not isMember and group.status eq 'open'}">
        <form method="post" action="${pageContext.request.contextPath}/groupOrder" style="margin-top:12px">
          <input type="hidden" name="action" value="join">
          <input type="hidden" name="inviteCode" value="${group.inviteCode}">
          <button type="submit" class="btn btn--primary">Join This Group</button>
        </form>
      </c:if>
    </div>

    <!-- Add Item (members only, open groups) -->
    <c:if test="${isMember and group.status eq 'open'}">
      <div class="card mb-4">
        <h3 class="mb-3">Add Item to Group Cart</h3>
        <form method="post" action="${pageContext.request.contextPath}/groupOrder">
          <input type="hidden" name="action" value="addItem">
          <input type="hidden" name="groupOrderId" value="${group.groupOrderId}">
          <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:flex-end">
            <select name="menuItemId" required style="padding:8px;border-radius:8px;border:1px solid var(--border);min-width:200px">
              <c:forEach var="item" items="${restaurantMenu}">
                <c:if test="${item.available}">
                  <option value="${item.menuItemId}">${item.name} - ₹${item.price}</option>
                </c:if>
              </c:forEach>
            </select>
            <input type="number" name="quantity" value="1" min="1" max="10"
              style="width:80px;padding:8px;border-radius:8px;border:1px solid var(--border)">
            <button type="submit" class="btn btn--primary">Add</button>
          </div>
        </form>
      </div>
    </c:if>

    <!-- Shared Cart -->
    <div class="card mb-4">
      <h3 class="mb-3">Shared Cart</h3>
      <c:choose>
        <c:when test="${empty cartItems}">
          <p>Cart is empty. Add items above!</p>
        </c:when>
        <c:otherwise>
          <c:forEach var="ci" items="${cartItems}">
            <c:set var="mi" value="${menuItemsMap[ci.menuItemId]}"/>
            <div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid var(--border)">
              <div>
                <strong>${mi.name}</strong>
                <span style="margin-left:8px;font-size:12px;color:var(--medium-text)">× ${ci.quantity}</span>
              </div>
              <div style="display:flex;align-items:center;gap:12px">
                <span>₹<fmt:formatNumber value="${mi.price * ci.quantity}" maxFractionDigits="2"/></span>
                <c:if test="${ci.userId eq sessionScope.loggedUser.userId and group.status eq 'open'}">
                  <form method="post" action="${pageContext.request.contextPath}/groupOrder" style="margin:0">
                    <input type="hidden" name="action" value="removeItem">
                    <input type="hidden" name="groupOrderId" value="${group.groupOrderId}">
                    <input type="hidden" name="menuItemId" value="${ci.menuItemId}">
                    <button type="submit" style="background:none;border:none;color:var(--accent-red);cursor:pointer;font-size:16px">✕</button>
                  </form>
                </c:if>
              </div>
            </div>
          </c:forEach>
        </c:otherwise>
      </c:choose>
    </div>

    <!-- Member Totals & Grand Total -->
    <div class="card mb-4">
      <h3 class="mb-3">Bill Breakdown</h3>
      <c:forEach var="entry" items="${memberTotals}">
        <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--border)">
          <span>Member #${entry.key}</span>
          <span>₹<fmt:formatNumber value="${entry.value}" maxFractionDigits="2"/></span>
        </div>
      </c:forEach>
      <div style="display:flex;justify-content:space-between;padding:12px 0;font-weight:700;font-size:1.1rem">
        <span>Grand Total</span>
        <span>₹<fmt:formatNumber value="${grandTotal}" maxFractionDigits="2"/></span>
      </div>
    </div>

    <!-- Checkout -->
    <c:if test="${isCreator and group.status eq 'open' and not empty cartItems}">
      <div class="card">
        <h3 class="mb-3">Checkout Group Order</h3>
        <form method="post" action="${pageContext.request.contextPath}/groupOrder">
          <input type="hidden" name="action" value="checkout">
          <input type="hidden" name="groupOrderId" value="${group.groupOrderId}">
          <div class="mb-3">
            <label><strong>Payment Method</strong></label><br>
            <label><input type="radio" name="paymentMethod" value="cash_on_delivery" checked> Cash on Delivery</label>
          </div>
          <button type="submit" class="btn btn--primary" onclick="return confirm('Place group order for entire group?')">
            Place Group Order (₹<fmt:formatNumber value="${grandTotal}" maxFractionDigits="2"/>)
          </button>
        </form>
      </div>
    </c:if>
  </c:if>

</div>
</main>
<%@ include file="/jsp/shared/footer.jspf" %>
</body>
</html>
