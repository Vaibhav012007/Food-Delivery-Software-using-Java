<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/jsp/shared/head.jspf" %>
<c:set var="pageTitle" value="Delivery Batches" />
<style>
.batch-card {
    background: white;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 16px;
    border: 1px solid var(--border);
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.order-chip {
    display: inline-block;
    background: var(--light-bg);
    border: 1px solid var(--border);
    padding: 4px 10px;
    border-radius: 16px;
    font-size: 0.85rem;
    margin-right: 6px;
    margin-top: 4px;
}
</style>
</head>
<body>
<%@ include file="/jsp/shared/header.jspf" %>
<main class="main-content">
<div class="container">

  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;flex-wrap:wrap;gap:12px">
    <h1 class="page-title" style="margin:0">Delivery Batches</h1>
    <form method="post" action="${pageContext.request.contextPath}/adminBatch">
      <input type="hidden" name="action" value="trigger">
      <button type="submit" class="btn btn--primary">⚡ Run Batching Cycle Now</button>
    </form>
  </div>

  <c:if test="${not empty error}">
    <div class="alert alert--error mb-3">${error}</div>
  </c:if>
  <c:if test="${not empty message}">
    <div class="alert alert--success mb-3">${message}</div>
  </c:if>

  <c:choose>
    <c:when test="${empty batches}">
      <div class="card" style="text-align:center;padding:40px">
        <h3>No Delivery Batches Created Yet</h3>
        <p style="color:var(--medium-text)" class="mt-2">
          Click "Run Batching Cycle Now" to bundle eligible 'Ready For Pickup' orders from the same restaurant.
        </p>
      </div>
    </c:when>
    <c:otherwise>
      <c:forEach var="b" items="${batches}">
        <div class="batch-card">
          <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px">
            <div>
              <h3>Batch #${b.batchId}</h3>
              <p style="color:var(--medium-text);font-size:0.9rem">
                Restaurant #${b.restaurantId} | Max Size: ${b.maxSize} | Status: <strong>${b.status}</strong>
              </p>
              <p style="color:var(--medium-text);font-size:0.85rem">
                Created: <fmt:formatDate value="${b.createdAt}" pattern="dd MMM, hh:mm a"/>
                <c:if test="${not empty b.dispatchedAt}">
                  | Dispatched: <fmt:formatDate value="${b.dispatchedAt}" pattern="dd MMM, hh:mm a"/>
                </c:if>
              </p>
            </div>
            <div>
              <c:if test="${b.status eq 'open'}">
                <form method="post" action="${pageContext.request.contextPath}/adminBatch">
                  <input type="hidden" name="action" value="dispatch">
                  <input type="hidden" name="batchId" value="${b.batchId}">
                  <button type="submit" class="btn btn--primary">Dispatch Batch</button>
                </form>
              </c:if>
            </div>
          </div>

          <div class="mt-3">
            <strong>Assigned Orders (${batchOrders[b.batchId].size()}):</strong>
            <div class="mt-1">
              <c:choose>
                <c:when test="${empty batchOrders[b.batchId]}">
                  <span style="color:var(--medium-text);font-size:0.9rem">No orders in this batch</span>
                </c:when>
                <c:otherwise>
                  <c:forEach var="ord" items="${batchOrders[b.batchId]}">
                    <span class="order-chip">
                      Order #${ord.orderId} (₹<fmt:formatNumber value="${ord.totalAmount}" maxFractionDigits="2"/> - ${ord.status})
                    </span>
                  </c:forEach>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
      </c:forEach>
    </c:otherwise>
  </c:choose>

</div>
</main>
<%@ include file="/jsp/shared/footer.jspf" %>
</body>
</html>
