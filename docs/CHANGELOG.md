# CHANGELOG

All notable changes merged on branch `features/member2`.

Date: 2026-05-22

## Overview
- Replaced client-side mocks with real `IAuctionService` RMI calls across the JavaFX client controllers.
- Consolidated user roles to `ADMIN` and `USER` (migrated legacy `SELLER`/`BIDDER`).
- Implemented server-side session tokens and real authentication exposed over RMI.
- Dual-write user data to the primary canonical SQLite database and a secondary `auction.db.sqlite` file.
- Added `DatabaseSyncService` to reconcile primary → secondary databases without closing the primary connection prematurely.
- Restored and fixed the RMI defaults: registry port set to `1999`, and `java.rmi.server.hostname` is configurable (defaults to `localhost`).
- Server lifecycle made non-blocking (server runs on a non-daemon lifecycle thread) and registers a shutdown hook.

## Server / Backend
- `ServerBootstrap` and `ServerLauncher` enhancements:
  - `configureRmiHostname()` added; RMI advertised host is consistent with UDP discovery payload.
  - RMI registry now listens on port `1999` by default (see `Constants.DEFAULT_RMI_PORT`).
  - Server starts background services (Auction Reaper, UDP Broadcaster, DatabaseSyncService) and keeps JVM alive via a non-daemon thread.
- `AuctionServiceImpl`:
  - Implements `login(...)` to return session tokens and exposes `getSessionRole(token)`.
  - Rate limiting and session TTL handling.
- Database changes:
  - `DatabaseManager` canonicalizes the DB path and migrates legacy user rows (normalizes roles and `created_at`).
  - `UserRepository` performs dual-write to primary DB and `auction.db.sqlite` (INSERT OR IGNORE/REPLACE where appropriate).
  - `DatabaseSyncService` now maintains a persistent primary connection for its lifetime and performs periodic INSERT OR REPLACE updates to secondary DBs.

## Client / UI
- Replaced mocked auth calls with real RMI lookups via `RmiClientProvider` and `IAuctionService`.
- `LoginController` and `RegistrationController` call `service.login(...)` and `service.getSessionRole(...)` and store session token in `ClientContext`.
- Dashboard controllers (`AdminPanelController`, `UserDashboardController`) load data asynchronously to avoid blocking the JavaFX UI thread.
- FXML/CSS fixes:
  - Centered the login layout and updated `login.fxml`/`login.css`.
  - Fixed invalid FXML imports that caused `FXMLLoader` to fail (e.g., removed improper `Region` import and added missing `URL` imports where required).

## Tests
- Added/updated smoke tests that cover FXML loading and backend auth flows (e.g., `DashboardFxmlSmokeTest`, `TestRegisterLogin`).
- Tests used to expose and fix the FXML import and DB sync issues; current tests pass locally after fixes.

## Developer / Run Instructions
1. Build the project (skip tests when iterating):
```
mvn -f "Real-Time-Distributed-Auction-System/pom.xml" -DskipTests compile
```
2. Run the server directly (recommended for local development). This uses the same classpath the tests use — replace `cp:...` with the output of `mvn dependency:build-classpath -Dmdep.outputFile=cp.txt` if needed.
```
REM (Powershell):
java -cp "target\classes;$(cat cp.txt)" com.auction.server.core.ServerLauncher -Dauction.rmi.hostname=localhost
```
Or use the included VSCode task `Run RTDAS Server`.

Notes:
- Default RMI port is `1999`. To override the advertised host set the system property `-Dauction.rmi.hostname=<host>`.
- The server will advertise the configured host in UDP discovery messages so clients can discover and connect reliably.

## Database policy and recommendations
- The canonical primary DB path is resolved at runtime by `DatabaseManager` and is under the project `data/auction.db` (canonicalized). For local development the project maintains a secondary `auction.db.sqlite` used for fast discovery/replication.
- Recommendation: add the runtime DB to `.gitignore` to avoid committing transient state. Consider adding `data/auction.db` to VCS ignore rules.

## Files changed (high level)
- Server: `ServerBootstrap`, `ServerLauncher`, `AuctionServiceImpl`, `DatabaseManager`, `UserRepository`, `DatabaseSyncService`, `UdpBroadcaster`
- Client: `RmiClientProvider`, `LoginController`, `RegistrationController`, `AdminPanelController`, `UserDashboardController`, `ViewLoader`
- Resources: `fxml/*` (login, dashboards), `css/login.css`, `css/style.css`
- Tests: `DashboardFxmlSmokeTest`, `TestRegisterLogin`

If you want, I can also produce a condensed CHANGES entry for the PR description and a short snippet to add to the top of the active PR for reviewers.
