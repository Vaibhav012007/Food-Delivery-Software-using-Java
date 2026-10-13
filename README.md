# FoodXpress

FoodXpress is a full-stack food-ordering application built with Java Servlets, JSP, JDBC, MySQL, and Maven. Customers can browse restaurants, manage a cart, place orders, view order history, and participate in group or surplus-food flows. Administrative and delivery-oriented features are protected by role-based access control.

> This is a traditional Java web application packaged as a WAR. It is not a Node.js, React, or Vercel serverless application.

## Objectives

FoodXpress is designed to demonstrate a complete food-ordering workflow while keeping the application architecture understandable:

- Secure user registration and login with BCrypt password hashing.
- Role-based access for customer, administrator, and delivery users.
- Restaurant and menu browsing with image fallbacks.
- Persistent carts and controlled quantity updates.
- Checkout validation, address ownership validation, and duplicate-checkout protection.
- MySQL-backed orders, payments, delivery batches, group orders, and surplus inventory.
- Transactional inventory reservation to prevent overselling during concurrent checkout.

## Features

### Customer features

- Register, sign in, sign out, and manage a profile.
- Browse restaurants and menu items.
- Add items to a cart, increase or decrease quantities, remove items, and clear a cart.
- Select an address and place an order.
- View order history and individual order details.
- Use group-order and surplus-food functionality where enabled.

### Administration and delivery features

- Manage restaurants and menu items.
- Manage order status and delivery workflows.
- Assign orders to delivery batches where applicable.

### Reliability and security controls

- Passwords are hashed with BCrypt; plaintext passwords are not stored.
- Authorization is enforced by `AuthFilter`.
- Address updates are scoped to the authenticated user.
- Cart and checkout input is validated server-side.
- Checkout uses an idempotency token to reduce duplicate submissions.
- Inventory reservation uses conditional database updates to avoid negative stock under concurrent orders.
- HikariCP manages JDBC connections; DAO code uses try-with-resources.

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java 8-compatible source, tested with JDK 17 |
| Web | Java Servlet 4, JSP 2.3, JSTL |
| Development server | Jetty Maven Plugin 9.4 |
| Build | Maven, WAR packaging |
| Database | MySQL 8 |
| Data access | JDBC with HikariCP |
| Security | BCrypt |
| Tests | JUnit 5 and Mockito |
| Optional desktop sources | JavaFX 17 |

## Architecture

```text
Browser
  │
  ▼
JSP views ──► Servlets / Controllers ──► Services ──► DAO layer ──► MySQL
  │                   │                                  │
  └──── HTML/CSS/JS ──┴── AuthFilter / session checks ───┴── HikariCP pool
```

Important source locations:

| Path | Responsibility |
| --- | --- |
| `src/main/java/com/app/controllers` | HTTP request handling and navigation |
| `src/main/java/com/app/dao` | DAO interfaces |
| `src/main/java/com/app/dao_implementation` | JDBC implementations |
| `src/main/java/com/app/models` | Domain models |
| `src/main/java/com/app/services` | Business workflows |
| `src/main/java/com/app/filters/AuthFilter.java` | Authentication and role checks |
| `src/main/java/com/app/utility/DBConnection.java` | HikariCP database pool |
| `src/main/webapp/jsp` | JSP views grouped by role |
| `src/main/webapp/assets` | CSS, JavaScript, and local images |
| `src/main/resources/schema.sql` | Database schema |
| `src/main/resources/sample_data.sql` | Demo users, restaurants, and menu items |
| `src/test/java` | Unit and concurrency tests |

## Application Flow

```text
Register / Login
      │
      ▼
Restaurant List → Restaurant Details → Select Quantity → Add to Cart
      │                                                    │
      └────────────────────────────────────────────────────┘
                            │
                            ▼
                         My Cart
                            │
             Update quantity / remove / clear items
                            │
                            ▼
                         Checkout
                            │
          Validate user, address, menu availability, stock,
          restaurant consistency, total, idempotency token
                            │
                            ▼
                   Order + Order Items saved
                            │
                            ▼
              Payment / Order Summary / Order History
```

### Checkout flow

1. The authenticated customer opens checkout.
2. The application creates a session-bound idempotency token.
3. The customer submits an address, restaurant, cart items, and payment option.
4. The server validates ownership of the address and availability of every menu item.
5. The order DAO conditionally reserves stock and creates the order records.
6. The cart is cleared only after a successful order operation.
7. The customer is redirected to order confirmation and can later open **My Orders**.

## Prerequisites

Install the following before running locally:

- JDK 17 (a full JDK, not only a JRE)
- Apache Maven 3.9 or later
- MySQL Server 8.0 or later
- A browser

Check the tools from a new PowerShell window:

```powershell
java -version
javac -version
mvn -version
mysql --version
```

## Local Setup

### 1. Clone and enter the project

```powershell
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd FoodApp
```

If your cloned folder has another name, use that folder name instead.

### 2. Create the database

Open MySQL:

```powershell
mysql -u root -p
```

Then create the database if it does not already exist:

```sql
CREATE DATABASE foodxpress CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

Load the schema and optional demo data from the project root:

```powershell
mysql -u root -p foodxpress < src\main\resources\schema.sql
mysql -u root -p foodxpress < src\main\resources\sample_data.sql
```

The scripts are designed to be repeatable. `sample_data.sql` uses `INSERT IGNORE` so it does not duplicate seeded rows.

### 3. Configure database access

Edit `src/main/resources/app.properties` for your local MySQL installation:

```properties
db.url=jdbc:mysql://localhost:3306/foodxpress?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=YOUR_LOCAL_MYSQL_PASSWORD
```

Do not commit real passwords. Keep personal credentials outside version control.

### 4. Build and test

```powershell
mvn test
```

Expected result:

```text
Tests run: 8, Failures: 0, Errors: 0
BUILD SUCCESS
```

### 5. Run the web application

```powershell
mvn jetty:run
```

Open:

```text
http://localhost:8080/FoodApp/
```

Keep this terminal open while using the app. Stop the server with `Ctrl+C`.

> Run `mvn jetty:run` only once. Starting a second instance gives `Address already in use` because port `8080` is already occupied.

## Demo Accounts

When `sample_data.sql` has been loaded, use:

| Role | Username | Password |
| --- | --- | --- |
| Administrator | `admin` | `admin123` |
| Customer | `customer1` | `pass123` |
| Customer | `customer2` | `pass123` |

These accounts are for local demonstration only. Change or remove them before a real deployment.

## Common URLs

| Page | URL |
| --- | --- |
| Home | `http://localhost:8080/FoodApp/` |
| Restaurants | `/FoodApp/restaurant?action=list` |
| Cart | `/FoodApp/cart` |
| Checkout | `/FoodApp/checkout` |
| My Orders | `/FoodApp/orderHistory` |
| Login | `/FoodApp/user?action=login` |

Do not link directly to JSP files such as `/customer/cart.jsp`. Use servlet routes such as `/cart`; they prepare data and enforce access controls.

## Tests

Run all tests:

```powershell
mvn test
```

Current tests cover:

- Delivery batching logic.
- Group checkout safeguards.
- Order status state transitions.
- Surplus inventory concurrency behavior.

Build a deployable WAR without rerunning tests:

```powershell
mvn package -DskipTests
```

The output is written to:

```text
target/FoodApp-0.0.1-SNAPSHOT.war
```

## Deployment Guidance

For the current Java Servlet/JSP/MySQL architecture, use a Docker-capable Java host such as **Render**, Railway, Fly.io, or a virtual machine. Vercel is not a direct drop-in deployment target for this server-rendered Java application.

The lowest-change deployment approach is:

1. Deploy the WAR in a Java 17 Tomcat/Jetty Docker container.
2. Use a managed or private MySQL 8 database with persistent storage.
3. Configure database host, user, and password using deployment environment variables.
4. Import `schema.sql`, then optionally `sample_data.sql`.
5. Keep the database private; only the web service should connect to it.

Before production deployment, update `DBConnection` to prefer environment variables such as `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` over local properties. Never include production secrets in this repository.

## Troubleshooting

### `mvn` is not recognized

Maven is not installed or its `bin` directory is missing from `PATH`. Install Maven, open a new PowerShell window, and run `mvn -version`.

### `mysql` is not recognized

Install MySQL Server/client tools and add MySQL's `bin` directory to `PATH`. Verify with `mysql --version`.

### `No compiler is provided in this environment`

Maven is using a JRE instead of a full JDK. Install JDK 17 and set `JAVA_HOME` to the JDK directory, then reopen PowerShell.

### Port 8080 is already in use

Another Jetty instance is running. Stop the terminal running Jetty with `Ctrl+C`, or identify the process:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```

### Images do not load

Restaurant and menu image fields can contain either external `https://` URLs or local paths. Check that the stored URL is valid and that your internet connection permits access to the external image provider. A local fallback image is displayed when an image cannot be loaded.

### A protected page redirects to login

Sign in first. Customer pages require an authenticated customer session; administration pages require an administrator account.

## Development Notes

- Use servlet routes for navigation instead of directly opening protected JSP files.
- Keep business rules in services/DAOs, not in JSPs.
- Use prepared statements for every database operation.
- Use try-with-resources for every JDBC connection, statement, and result set.
- Run `mvn test` after functional changes.
- Do not delete tests to hide a failure; investigate and fix the cause.

## License

No license has been specified for this repository. Add a license file before distributing or reusing the project publicly.
