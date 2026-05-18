# Task Classification - Real-Time Distributed Auction System (Updated)

## Overview
This document classifies all **remaining** project tasks by category, priority, and assignment. Each team member owns complete modules (backend + RMI + GUI) to minimize overlap and ensure clear integration points.

**Current Status**: Infrastructure complete, controllers need implementation

---

## Revised Module-Based Ownership

### 🔵 BLUE - Authentication, Admin & Seller Module (Member 1)
- Login flow, server connection, admin operations, seller dashboard, CSV export, backups
- Owns: `LoginController`, `ConnectController`, `SellerDashboardController`, `AdminPanelController`, auth/admin/seller/service methods

### 🟢 GREEN - Auction Bidding, Gallery & Real-Time Module (Member 2)  
- Auction browsing, bidding, polling updates, auction detail view, deployment docs
- Owns: `GalleryController`, `AuctionDetailController`, `PollingService`, bidding/gallery service methods

### 🟣 PURPLE - Integration & Testing (Both Members)
- Daily integration sessions, end-to-end testing, deployment, final demo

---

## Remaining Task Classification Matrix

### 1. Server Connection & Authentication (M1)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| AUTH-01 | Implement `ConnectController` UI | 🔵 BLUE | High | M1 | 2 | None |
| AUTH-02 | Add UDP discovery or manual connect logic | 🔵 BLUE | High | M1 | 2 | AUTH-01 |
| AUTH-03 | Store server connection in singleton | 🔵 BLUE | High | M1 | 1 | AUTH-02 |
| AUTH-04 | Implement `LoginController` login handler | 🔵 BLUE | High | M1 | 2 | AUTH-03 |
| AUTH-05 | Call RMI `login()` and handle response | 🔵 BLUE | High | M1 | 1 | AUTH-04 |
| AUTH-06 | Navigate to role-based dashboard | 🔵 BLUE | High | M1 | 2 | AUTH-05 |
| AUTH-07 | Implement logout functionality | 🔵 BLUE | Medium | M1 | 1 | AUTH-06 |
| AUTH-08 | Add session management | 🔵 BLUE | Low | M1 | 1 | AUTH-07 |
| AUTH-09 | Test concurrent logins | 🟣 PURPLE | Medium | M1 | 1 | AUTH-06 |

---

### 2. Auction Gallery & Browsing (M2)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| GAL-01 | Implement `GalleryController` structure | 🟢 GREEN | High | M2 | 2 | AUTH-03 |
| GAL-02 | Load active auctions via RMI | 🟢 GREEN | High | M2 | 2 | GAL-01 |
| GAL-03 | Display auction cards in grid | 🟢 GREEN | High | M2 | 2 | GAL-02 |
| GAL-04 | Fetch thumbnails via RMI `getThumbnail()` | 🟢 GREEN | High | M2 | 2 | GAL-03 |
| GAL-05 | Add click-to-detail navigation | 🟢 GREEN | High | M2 | 2 | GAL-04 |
| GAL-06 | Implement lazy loading for images | 🟢 GREEN | Medium | M2 | 2 | GAL-05 |
| GAL-07 | Add search/filter functionality | 🟢 GREEN | Low | M2 | 2 | GAL-06 |
| GAL-08 | Test gallery with 50+ auctions | 🟣 PURPLE | Medium | M2 | 1 | GAL-07 |

---

### 3. Auction Detail & Bidding (M2)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| DET-01 | Implement `AuctionDetailController` structure | 🟢 GREEN | High | M2 | 2 | GAL-05 |
| DET-02 | Display auction info (title, desc, price) | 🟢 GREEN | High | M2 | 2 | DET-01 |
| DET-03 | Show current bid and highest bidder | 🟢 GREEN | High | M2 | 1 | DET-02 |
| DET-04 | Implement bid input UI | 🟢 GREEN | High | M2 | 2 | DET-03 |
| DET-05 | Call RMI `placeBid()` with validation | 🟢 GREEN | High | M2 | 2 | DET-04 |
| DET-06 | Display bid history from RMI | 🟢 GREEN | Medium | M2 | 2 | DET-05 |
| DET-07 | Add countdown timer display | 🟢 GREEN | Medium | M2 | 2 | DET-06 |
| DET-08 | Show error messages for invalid bids | 🟢 GREEN | High | M2 | 1 | DET-05 |
| DET-09 | Stress test concurrent bidding | 🟣 PURPLE | High | M2 | 2 | DET-08 |

---

### 4. Polling Service & Real-Time Updates (M2)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| POLL-01 | Implement `PollingService` scheduled executor | 🟢 GREEN | High | M2 | 2 | None |
| POLL-02 | Add `startPolling(auctionId, callback)` method | 🟢 GREEN | High | M2 | 2 | POLL-01 |
| POLL-03 | Call `getAuctionById()` every 2 seconds | 🟢 GREEN | High | M2 | 1 | POLL-02 |
| POLL-04 | Dispatch updates via callback | 🟢 GREEN | High | M2 | 1 | POLL-03 |
| POLL-05 | Integrate polling in `AuctionDetailController` | 🟢 GREEN | High | M2 | 2 | DET-01, POLL-04 |
| POLL-06 | Update UI with `Platform.runLater()` | 🟢 GREEN | High | M2 | 1 | POLL-05 |
| POLL-07 | Add pause/resume on view switch | 🟢 GREEN | Medium | M2 | 2 | POLL-06 |
| POLL-08 | Implement `shutdown()` method | 🟢 GREEN | High | M2 | 1 | POLL-01 |
| POLL-09 | Optimize polling interval | 🟢 GREEN | Medium | M2 | 1 | POLL-08 |
| POLL-10 | Memory leak check | 🟣 PURPLE | High | M2 | 1 | POLL-09 |

---

### 5. Seller Dashboard & Auction Creation (M1)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| SELL-01 | Implement `SellerDashboardController` structure | 🔵 BLUE | High | M1 | 2 | AUTH-06 |
| SELL-02 | Load seller's auctions via RMI | 🔵 BLUE | High | M1 | 2 | SELL-01 |
| SELL-03 | Display auctions in table with status | 🔵 BLUE | High | M1 | 2 | SELL-02 |
| SELL-04 | Create auction form UI | 🔵 BLUE | High | M1 | 3 | SELL-03 |
| SELL-05 | Add image picker for 3 images | 🔵 BLUE | High | M1 | 2 | SELL-04 |
| SELL-06 | Validate form inputs | 🔵 BLUE | High | M1 | 2 | SELL-05 |
| SELL-07 | Call RMI `createAuction()` with images | 🔵 BLUE | High | M1 | 2 | SELL-06 |
| SELL-08 | Handle success/error feedback | 🔵 BLUE | High | M1 | 1 | SELL-07 |
| SELL-09 | Implement cancel auction feature | 🔵 BLUE | Medium | M1 | 2 | SELL-03 |
| SELL-10 | Add relist ended auctions | 🔵 BLUE | Low | M1 | 2 | SELL-09 |
| SELL-11 | Test cancellation rules | 🟣 PURPLE | Medium | M1 | 1 | SELL-09 |

---

### 6. Admin Panel & User Management (M1)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| ADM-01 | Implement `AdminPanelController` structure | 🔵 BLUE | High | M1 | 2 | AUTH-06 |
| ADM-02 | Create user form UI | 🔵 BLUE | High | M1 | 2 | ADM-01 |
| ADM-03 | List all users table | 🔵 BLUE | High | M1 | 2 | ADM-02 |
| ADM-04 | Call RMI `createUser()` | 🔵 BLUE | High | M1 | 2 | ADM-03 |
| ADM-05 | Test new user can login | 🟣 PURPLE | High | M1 | 1 | ADM-04 |
| ADM-06 | Wire backup database button | 🔵 BLUE | Medium | M1 | 2 | ADM-01 |
| ADM-07 | Display audit logs | 🔵 BLUE | Medium | M1 | 2 | ADM-06 |
| ADM-08 | Add confirmation dialogs | 🔵 BLUE | Low | M1 | 1 | ADM-07 |
| ADM-09 | Test all admin operations | 🟣 PURPLE | High | All | 2 | ADM-08 |

---

### 7. Service Layer TODOs (M1)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| SVC-01 | Implement `exportAuctionsToCSV()` | 🔵 BLUE | Medium | M1 | 2 | None |
| SVC-02 | Implement `backupDatabase()` | 🔵 BLUE | Medium | M1 | 2 | SVC-01 |
| SVC-03 | Implement `getAuditLogs()` | 🔵 BLUE | Medium | M1 | 2 | SVC-02 |
| SVC-04 | Test CSV export format | 🟣 PURPLE | Low | M1 | 1 | SVC-01 |
| SVC-05 | Test database backup file | 🟣 PURPLE | Low | M1 | 1 | SVC-02 |

---

### 8. Integration Tasks (All Members)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| INT-01 | Connect → Login → Dashboard flow | 🟣 PURPLE | High | Both | 2 | AUTH-06, GAL-01, SELL-01 |
| INT-02 | Gallery → Detail → Bid flow | 🟣 PURPLE | High | Both | 2 | GAL-05, DET-08, POLL-06 |
| INT-03 | Create auction → Appears in gallery | 🟣 PURPLE | High | Both | 2 | SELL-08, GAL-02 |
| INT-04 | Admin creates user → User logs in | 🟣 PURPLE | High | Both | 1 | ADM-05, AUTH-05 |
| INT-05 | Full regression test suite | 🟣 PURPLE | High | Both | 3 | All features |
| INT-06 | Performance optimization | 🟣 PURPLE | Medium | Both | 2 | INT-05 |
| INT-07 | Bug fixing sprint | 🟣 PURPLE | High | Both | 4 | INT-05 |

---

### 9. Documentation & Deployment (Both Members)

| ID | Task | Category | Priority | Owner | Est. Hours | Dependencies |
|----|------|----------|----------|-------|------------|--------------|
| DOC-01 | Write authentication guide | 🔵 BLUE | Medium | M1 | 1 | AUTH-08 |
| DOC-02 | Record login flow demo | 🔵 BLUE | Medium | M1 | 1 | AUTH-08 |
| DOC-03 | Write bidding system docs | 🟢 GREEN | Medium | M2 | 1 | DET-08 |
| DOC-04 | Record bidding demo | 🟢 GREEN | Medium | M2 | 1 | DET-08 |
| DOC-05 | Write seller/admin guide | 🔵 BLUE | Medium | M1 | 1 | ADM-09 |
| DOC-06 | Record management demo | 🔵 BLUE | Medium | M1 | 1 | ADM-09 |
| DOC-07 | Build production JAR | 🟣 PURPLE | High | Both | 1 | INT-07 |
| DOC-08 | Create deployment checklist | 🟣 PURPLE | Medium | Both | 1 | DOC-07 |
| DOC-09 | Document ports/firewall rules | 🟣 PURPLE | Medium | Both | 1 | DOC-08 |
| DOC-10 | Write troubleshooting guide | 🟣 PURPLE | Low | Both | 1 | DOC-09 |
| DOC-11 | Record final demo video | 🟣 PURPLE | High | Both | 1 | DOC-10 |
| DOC-12 | Prepare presentation slides | 🟣 PURPLE | Medium | Both | 1 | DOC-11 |

---

## Workload Distribution

### Member 1 (Authentication, Admin, Seller & Services)
| Category | Tasks | Exclusive Hours | Integration Hours |
|----------|-------|-----------------|-------------------|
| Auth Controllers | AUTH-01 to AUTH-08 | 11 | - |
| Seller Dashboard | SELL-01 to SELL-11 | 19 | 2 |
| Admin Panel | ADM-01 to ADM-09 | 11 | 2 |
| Service TODOs | SVC-01 to SVC-05 | 8 | 2 |
| Documentation | DOC-01, DOC-02, DOC-05, DOC-06 | 4 | - |
| **Total** | | **53 hours** | **~12 hours** |

### Member 2 (Auction Bidding, Gallery & Real-Time)
| Category | Tasks | Exclusive Hours | Integration Hours |
|----------|-------|-----------------|-------------------|
| Gallery | GAL-01 to GAL-08 | 13 | - |
| Auction Detail | DET-01 to DET-09 | 14 | 2 |
| Polling Service | POLL-01 to POLL-10 | 13 | - |
| Documentation | DOC-03, DOC-04 | 2 | - |
| **Total** | | **42 hours** | **~12 hours** |

**Workload Balance Note**: M1 has more tasks (Auth+Seller+Admin) while M2 has more complex real-time technical challenges. Both members should plan for ~65 total hours over 6 days including integration time.

---

## Git Workflow

```
main (production-ready)
  ↑
develop (daily integration)
  ↑
feature/m1-auth-seller-admin-module
feature/m2-bidding-gallery-module
```

### Merge Protocol
1. Complete task on feature branch
2. Run local tests
3. Request review from one other team member
4. Address feedback
5. Merge to `develop` during evening integration
6. Test integrated build together
7. Resolve conflicts immediately

---

## Conflict Prevention Rules

1. **Module Ownership**: Each member fully owns their module files
2. **Shared Interfaces**: `IAuctionService` changes require team discussion
3. **Daily Sync**: Never go more than 24 hours without merging to develop
4. **Communication**: Announce file changes in group chat before modifying
5. **Backup Plan**: If conflict occurs, revert to morning's stable version

---

## Priority Legend

- **High**: Must complete for basic functionality (blocks other tasks)
- **Medium**: Important but can be deferred if needed
- **Low**: Nice-to-have features, complete only if time permits

---

## Daily Status Tracking Template

```markdown
### [Date] Status Update

#### M1 (Auth, Seller, Admin Module)
- ✅ Completed: [Task IDs]
- 🔄 In Progress: [Task IDs]
- 🚧 Blocked: [Task IDs + reason]
- 📅 Next: [Task IDs]

#### M2 (Bidding, Gallery Module)
- ✅ Completed: [Task IDs]
- 🔄 In Progress: [Task IDs]
- 🚧 Blocked: [Task IDs + reason]
- 📅 Next: [Task IDs]

#### Integration Notes
- [Issues discovered during evening integration]
- [Action items for tomorrow]
```

---

## Module File Ownership

### M1 Files (Auth, Seller, Admin)
- `src/main/java/com/auction/client/controllers/LoginController.java`
- `src/main/java/com/auction/client/controllers/ConnectController.java`
- `src/main/java/com/auction/client/controllers/SellerDashboardController.java`
- `src/main/java/com/auction/client/controllers/AdminPanelController.java`
- `src/main/resources/fxml/login.fxml`
- `src/main/resources/fxml/connect.fxml`
- `src/main/resources/fxml/seller_dashboard.fxml`
- `src/main/resources/fxml/admin_panel.fxml`

### M2 Files (Gallery, Bidding, Real-Time)
- `src/main/java/com/auction/client/controllers/GalleryController.java`
- `src/main/java/com/auction/client/controllers/AuctionDetailController.java`
- `src/main/java/com/auction/client/service/PollingService.java`
- `src/main/resources/fxml/gallery.fxml`
- `src/main/resources/fxml/auction_detail.fxml`

### Shared Files (Require Coordination)
- `src/main/java/com/auction/shared/interfaces/IAuctionService.java`
- `src/main/java/com/auction/client/network/RmiClientProvider.java`
- `src/main/resources/css/*.css`

---

## Notes

- Tasks are organized by **module ownership** for 2 members, not layer
- Each member handles backend logic + RMI + GUI for their module
- Integration happens daily to prevent accumulation of conflicts
- Estimated hours are guidelines; adjust based on actual progress
- Purple (integration) tasks require both members present
- Day 5 afternoon reserved for overflow bug fixes
- M1 has broader scope (Auth+Seller+Admin), M2 has deeper technical complexity (Real-time polling)
- Both members should coordinate on `IAuctionService` interface changes
