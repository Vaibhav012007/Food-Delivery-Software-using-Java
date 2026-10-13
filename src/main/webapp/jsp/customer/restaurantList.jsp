<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="pageTitle" value="Restaurants Near You" />

<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/jsp/shared/head.jspf" %>
</head>
<body>

    <%@ include file="/jsp/shared/header.jspf" %>

    <!-- =============================== -->
    <!-- MAIN CONTENT                   -->
    <!-- =============================== -->
    <main class="main-content">
        <div class="container">

            <!-- Page Heading -->
            <div class="mb-5">
                <h1 class="page-title">Restaurants Near You</h1>
                <p class="card__text">
                    Choose a restaurant to explore its menu
                </p>
            </div>

            <!-- =============================== -->
            <!-- RESTAURANT GRID                 -->
            <!-- =============================== -->
            <div class="grid-4">

              <c:forEach var="restaurant" items="${restaurants}">
    <a href="${pageContext.request.contextPath}/restaurant?action=view&id=${restaurant.restaurantId}"
       class="card">

        <!-- Restaurant images may be an external CDN URL or a local application path. -->
        <c:choose>
            <c:when test="${not empty restaurant.imageUrl and (fn:startsWith(restaurant.imageUrl, 'http://') or fn:startsWith(restaurant.imageUrl, 'https://'))}">
                <img src="${restaurant.imageUrl}"
                     alt="${restaurant.name}"
                     class="card__image"
                     onerror="this.onerror=null;this.src='${pageContext.request.contextPath}/assets/images/default_image.png';" />
            </c:when>
            <c:when test="${not empty restaurant.imageUrl}">
                <img src="${pageContext.request.contextPath}/${restaurant.imageUrl}"
                     alt="${restaurant.name}"
                     class="card__image"
                     onerror="this.onerror=null;this.src='${pageContext.request.contextPath}/assets/images/default_image.png';" />
            </c:when>
            <c:otherwise>
                <img src="${pageContext.request.contextPath}/assets/images/default_image.png"
                     alt="${restaurant.name}"
                     class="card__image" />
            </c:otherwise>
        </c:choose>

        <div class="card__body">

            <h3 class="card__title">${restaurant.name}</h3>

            <p class="card__text mb-2">
                ${restaurant.cuisineType}
            </p>

            <p class="card__text mb-2">
                <i class="fa-solid fa-location-dot"></i>
                ${restaurant.city}
            </p>

            <div class="mt-3"
                 style="display:flex; justify-content:space-between; align-items:center;">

                <span style="color:#ffaa00; font-weight:600;">
                    ⭐ ${restaurant.rating}
                </span>

                <span class="btn btn--secondary">
                    View Menu
                </span>
            </div>

        </div>
    </a>
</c:forEach>


            </div>

            <!-- Empty State -->
            <c:if test="${empty restaurants}">
                <div class="text-center mt-5">
                    <p>No restaurants available right now.</p>
                </div>
            </c:if>

        </div>
    </main>

    <%@ include file="/jsp/shared/footer.jspf" %>

</body>
</html>
