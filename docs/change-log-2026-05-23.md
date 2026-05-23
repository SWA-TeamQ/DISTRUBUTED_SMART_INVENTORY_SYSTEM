# Change Log — 2026-05-23

## Scope
This document captures the integration and stabilization changes made on branch `merge/features-member2` up to now.

## Highlights
- Resolved merge integration by accepting incoming changes from `main` where applicable.
- Fixed server compile/runtime contract mismatch by implementing missing auction service method.
- Stabilized client auth navigation and controller error handling.
- Polished and restructured login/registration UI and CSS.
- Enforced post-registration flow to return to login (not dashboard auto-entry).
- Migrated runtime DB target from `data/auction.db` to `data/auction.db.sqlite`.
- Added one-time automatic migration from legacy DB file to new DB file.

## Functional Changes

### Authentication + Navigation
- `LoginController` improved error handling with clearer auth/connection/UI-load failures.
- `RegistrationController` now:
  - Registers user only.
  - Clears any session fields after register.
  - Always routes to `login.fxml` after successful registration.
- Registration screen includes a "Back/Login" navigation link to return to login.

### Server / Services
- Implemented `getActiveAuctions()` in `AuctionServiceImpl` to satisfy service interface and restore clean compilation.

### UI / FXML / CSS
- Reworked `login.fxml` layout to match registration visual structure.
- Updated `registration.fxml` navigation controls.
- Expanded `style.css` with card-based auth styling, refined spacing, typography, button treatment, and focus/hover polish.
- Standardized scene stylesheet application via existing `ViewLoader` flow.

### Database Path + Migration
- Runtime DB constant changed to `data/auction.db.sqlite`.
- Added startup migration in `DatabaseManager`:
  - If configured DB is missing and legacy `auction.db` exists, copy legacy file to `auction.db.sqlite` once.
  - Migration skips when `auction.db.sqlite` already exists.

## Documentation Updates
- Updated DB filename references in:
  - `docs/database.md`
  - `docs/RTDAS_PRD.md`
  - `docs/demo-runbook.md`

## Utility / Verification Tools Added
- `src/main/java/com/auction/tools/TestRegisterLogin.java`
- `src/main/java/com/auction/tools/UdpDiscoveryListener.java`

## Current Working Tree Snapshot
`git diff --stat` currently reports:
- 13 files changed, 239 insertions(+), 66 deletions(-)
- includes binary updates to:
  - `data/auction.db`
  - `data/auction.db.sqlite`

## Files Currently Modified (Uncommitted)
- `data/auction.db`
- `data/auction.db.sqlite`
- `docs/RTDAS_PRD.md`
- `docs/database.md`
- `docs/demo-runbook.md`
- `pom.xml`
- `src/main/java/com/auction/client/controllers/LoginController.java`
- `src/main/java/com/auction/client/controllers/RegistrationController.java`
- `src/main/java/com/auction/server/repository/DatabaseManager.java`
- `src/main/java/com/auction/shared/Constants.java`
- `src/main/resources/css/style.css`
- `src/main/resources/fxml/login.fxml`
- `src/main/resources/fxml/registration.fxml`

## Build Verification
- Verified compile after latest changes with:
  - `mvn -DskipTests compile`
  - `mvn -DskipTests clean compile`
- Both completed with `BUILD SUCCESS` in the current session.

## Notes
- There are two `data` folders in the parent workspace; app runtime configuration in this project resolves DB path relative to `Real-Time-Distributed-Auction-System`.
- Active configured DB file is `Real-Time-Distributed-Auction-System/data/auction.db.sqlite`.
