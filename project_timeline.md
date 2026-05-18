# Project Timeline - Real-Time Distributed Auction System (Updated)

## Overview
- **Duration**: 6 Days (Starting Tomorrow)
- **Team Size**: 2 Members
- **Goal**: Complete remaining implementation, integration, and testing of RTDAS
- **Current Status**: ✅ Infrastructure Complete | ⚠️ Controllers Need Implementation

---

## Current Project Status (As of Today)

### ✅ Completed Components
| Component | Status | Notes |
|-----------|--------|-------|
| **Database** | ✅ Complete | SQLite schema, DatabaseManager, all Repositories working |
| **RMI Interface** | ✅ Complete | `IAuctionService` fully defined |
| **RMI Server** | ✅ Complete | `AuctionServiceImpl` implemented (some TODOs remain) |
| **Server Core** | ✅ Complete | `AuctionManager`, `LifecycleManager`, `ImageStore`, `UdpBroadcaster` |
| **Client Network** | ✅ Complete | `RmiClientProvider`, `UdpDiscoveryClient` |
| **Polling Service** | ⚠️ Partial | `PollingService` skeleton exists, needs implementation |
| **FXML Files** | ✅ Complete | All 6 views created (login, gallery, auction_detail, seller_dashboard, admin_panel, connect) |
| **CSS Styling** | ✅ Complete | All styles applied (IDE warnings are false positives) |
| **Shared Models** | ✅ Complete | User, AuctionItem, Bid, Seller, Bidder, Admin |
| **Exceptions** | ✅ Complete | All custom exceptions defined |

### ⚠️ Remaining Implementation Work
| Component | Status | Effort Required |
|-----------|--------|-----------------|
| **LoginController** | 🔴 Empty | Connect RMI, handle login, navigate to appropriate dashboard |
| **GalleryController** | 🔴 Empty | Load auctions, display grid, fetch thumbnails, click-to-detail |
| **AuctionDetailController** | 🔴 Empty | Display auction info, place bids, show bid history, polling integration |
| **SellerDashboardController** | 🔴 Empty | List seller's auctions, create auction form, cancel auctions, CSV export |
| **AdminPanelController** | 🔴 Empty | Create users, view all users, backup DB, view audit logs |
| **ConnectController** | 🔴 Empty | Server connection UI, UDP discovery or manual connect |
| **PollingService** | 🔴 Empty | Implement scheduled polling logic |
| **Export/Backup/Logs** | 🔴 TODO in Service | Implement CSV export, DB backup, audit log retrieval |

---

## Revised Team Roles - Module-Based Ownership (2 Members)

Each member owns **full-stack features** (backend logic + RMI exposure + GUI controller) for their assigned modules:

### Member 1 (M1) - Authentication, Seller & Admin Module
**Owns**: Login flow, server connection, seller dashboard, admin panel, service utilities, documentation for auth/seller/admin
- **Backend**: User authentication, seller operations, admin operations, CSV export, backup, audit logs
- **RMI**: Auth methods, seller/admin service methods, utility methods
- **GUI**: `LoginController`, `ConnectController`, `SellerDashboardController`, `AdminPanelController`
- **Integration**: Full login → dashboard flow, create auction flow, admin operations flow

### Member 2 (M2) - Auction Bidding & Real-Time Module  
**Owns**: Auction browsing, bidding system, real-time updates, polling service, deployment packaging
- **Backend**: Bid validation, gallery loading, auction detail retrieval, polling support
- **RMI**: Bidding methods, gallery/auction query methods, thread safety optimization
- **GUI**: `GalleryController`, `AuctionDetailController`, `PollingService`
- **Integration**: Full browse → bid → poll flow, real-time update flow

---

## Day-by-Day Schedule (Revised for Remaining Work)

### Day 1: Connection & Authentication Flow
**Theme**: Establish server connection and login system

| Time | M1 (Auth, Seller, Admin Module) | M2 (Bidding, Gallery Module) |
|------|---------------------------------|------------------------------|
| Morning | - Implement `ConnectController`<br>- Add UDP discovery or manual connect<br>- Store server connection<br>- Complete `exportAuctionsToCSV()` in service<br>- Complete `backupDatabase()` in service<br>- Complete `getAuditLogs()` in service | - Implement `PollingService`<br>- Add scheduled executor logic<br>- Test with mock data |
| Afternoon | - Implement `LoginController`<br>- Call RMI login<br>- Navigate based on role<br>- Start `SellerDashboardController` structure<br>- Load seller's auctions<br>- Display in table | - Start `GalleryController` structure<br>- Load active auctions from RMI<br>- Display auction cards |
| Evening Integration | **Joint Session (1.5 hours)**<br>- Test: Connect → Login → Dashboard navigation<br>- Verify role-based routing (Bidder/Seller/Admin)<br>- Test connection error handling |

**Deliverables**:
- ✅ Server connection works (UDP or manual)
- ✅ Login authenticates via RMI
- ✅ Users routed to correct dashboard by role

---

### Day 2: Auction Browsing & Gallery
**Theme**: View and explore active auctions

| Time | M1 (Auth, Seller, Admin Module) | M2 (Bidding, Gallery Module) |
|------|---------------------------------|------------------------------|
| Morning | - Add session management<br>- Implement logout functionality<br>- Test re-authentication<br>- Implement create auction form in `SellerDashboardController`<br>- Add image picker for 3 images<br>- Validate form inputs | - Complete `GalleryController`<br>- Fetch thumbnails via RMI<br>- Add click-to-detail navigation |
| Afternoon | - Wire up create auction to RMI<br>- Handle success/error feedback<br>- Test auction appears in gallery | - Implement image loading optimization<br>- Add lazy loading for gallery<br>- Test with many auctions |
| Evening Integration | **Joint Session (1.5 hours)**<br>- Test: Login → Browse Gallery → View Details<br>- Create auction → Appears in gallery<br>- Test thumbnail loading performance |

**Deliverables**:
- ✅ Gallery displays all active auctions with thumbnails
- ✅ Clicking auction opens detail view
- ✅ Sellers can create new auctions

---

### Day 3: Bidding System & Real-Time Updates
**Theme**: Place bids and see live updates

| Time | M1 (Auth, Seller, Admin Module) | M2 (Bidding, Gallery Module) |
|------|---------------------------------|------------------------------|
| Morning | - Start `AdminPanelController`<br>- Create user form<br>- List all users table<br>- Add cancel auction feature<br>- Add CSV export button<br>- Test seller restrictions | - Complete `AuctionDetailController`<br>- Display auction info & current bid<br>- Implement bid placement UI |
| Afternoon | - Complete admin user creation<br>- Wire to RMI `createUser()`<br>- Test new user can login<br>- Start `AdminPanelController` backup/logs sections<br>- Wire backup button<br>- Display audit logs | - Integrate `PollingService` in detail view<br>- Update UI on poll callback (Platform.runLater)<br>- Show bid history |
| Evening Integration | **Joint Session (2 hours)**<br>- Test: Place bid → Poll updates UI<br>- Multiple clients bidding simultaneously<br>- Test stale data detection<br>- Admin creates user → User can login |

**Deliverables**:
- ✅ Bids can be placed via detail view
- ✅ Polling updates price/bidder in real-time
- ✅ Admin can create users and view system data

---

### Day 4: Seller Dashboard & Auction Management
**Theme**: Complete seller features and polish

| Time | M1 (Auth, Seller, Admin Module) | M2 (Bidding, Gallery Module) |
|------|---------------------------------|------------------------------|
| Morning | - Add password change feature<br>- Implement user profile view<br>- Test security<br>- Complete seller's auction list with status<br>- Add relist ended auctions feature<br>- Test cancellation rules | - Add bid validation feedback<br>- Show error messages for invalid bids<br>- Add countdown timer display |
| Afternoon | - Polish login error messages<br>- Add "remember me" option<br>- Test edge cases<br>- Polish admin panel layout<br>- Add confirmation dialogs<br>- Test all admin operations | - Optimize polling interval<br>- Add pause/resume on view switch<br>- Memory leak check |
| Evening Integration | **Joint Session (2 hours)**<br>- Full seller flow: Create → Monitor → Cancel/Export<br>- Full admin flow: Create user → View logs → Backup<br>- Test concurrent sellers |

**Deliverables**:
- ✅ Sellers can manage all their auctions
- ✅ Admin panel fully functional
- ✅ All error states handled gracefully

---

### Day 5: Comprehensive Testing & Bug Fixes
**Theme**: Find and fix all issues

| Time | M1 (Auth, Seller, Admin Module) | M2 (Bidding, Gallery Module) |
|------|---------------------------------|------------------------------|
| Morning | - Test auth edge cases<br>- Concurrent logins<br>- Session timeout<br>- Test all seller operations<br>- Large image uploads<br>- CSV export verification | - Stress test bidding<br>- 10+ concurrent bidders<br>- Network disconnection scenarios |
| Afternoon | - Fix discovered bugs<br>- Performance profiling<br>- Memory optimization | - Fix race conditions<br>- Optimize RMI calls<br>- Reduce polling overhead |
| Evening Integration | **Joint Session (2.5 hours)**<br>- Full regression test suite<br>- Document all remaining issues<br>- Prioritize fixes for Day 6 |

**Deliverables**:
- ✅ All critical bugs fixed
- ✅ Performance acceptable (<2s response time)
- ✅ No memory leaks detected

---

### Day 6: Final Polish & Deployment Preparation
**Theme**: Production readiness

| Time | M1 (Auth, Seller, Admin Module) | M2 (Bidding, Gallery Module) |
|------|---------------------------------|------------------------------|
| Morning | - Write user authentication guide<br>- Record login flow demo<br>- Final security review<br>- Write seller/admin guide<br>- Record management demo<br>- Final UI polish | - Write bidding system documentation<br>- Record bidding demo<br>- Final performance check |
| Afternoon | - Build production JAR<br>- Test standalone server<br>- Test multi-client scenario<br>- Create deployment checklist<br>- Document ports/firewall rules<br>- Troubleshooting guide | - Package all resources<br>- Verify all images/assets<br>- Create quick-start guide |
| Evening Integration | **Final Demo & Presentation (2.5 hours)**<br>- Complete end-to-end demonstration<br>- Record final demo video<br>- Prepare presentation slides<br>- Celebrate! 🎉 |

**Deliverables**:
- ✅ Production-ready fat JAR
- ✅ Complete documentation set
- ✅ Demo video recorded
- ✅ Both team members can present any feature

---

## Integration Points Schedule

| Day | Time | Duration | Focus Area |
|-----|------|----------|------------|
| Day 1 | Evening | 1.5 hours | Connect → Login → Dashboard |
| Day 2 | Evening | 1.5 hours | Gallery → Auction Detail → Create |
| Day 3 | Evening | 2 hours | Bidding → Polling → Admin Users |
| Day 4 | Evening | 2 hours | Seller Management → Admin Operations |
| Day 5 | Evening | 2.5 hours | Full Regression Testing |
| Day 6 | Evening | 2.5 hours | Final Demo & Deployment |

---

## Risk Mitigation

### Potential Risks
1. **RMI Connection Issues** - M2 leads troubleshooting, M1 assists
2. **UI Freezing on Network Calls** - M2 ensures background threading in controllers
3. **Integration Conflicts** - Daily evening sync prevents accumulation
4. **Polling Performance** - M2 optimizes interval, implements pause/resume
5. **Workload Imbalance** - M1 has broader scope (Auth+Seller+Admin); M2 has deeper technical complexity (Real-time). Both should communicate if one falls behind.
6. **Interface Changes** - Any `IAuctionService` changes require both members to coordinate immediately

### Contingency Plan
- If a task falls behind: Reallocate from next day's buffer time
- If integration fails: Roll back to morning's stable version
- If one member unavailable: Other member covers critical path tasks (both should understand full system)
- Day 5 afternoon reserved for overflow bug fixes
- If M1 falls behind on Seller Dashboard: M2 can assist since gallery already displays auctions
- If M2 falls behind on Polling: M1 can implement basic UI updates while M2 optimizes

---

## Success Criteria

- [ ] All 6 days completed with daily integrations
- [ ] Zero merge conflicts in final build
- [ ] All controllers implemented and wired to RMI
- [ ] Polling service updates UI in real-time
- [ ] Server handles 10+ concurrent clients
- [ ] Complete documentation delivered
- [ ] Successful demo recording

---

## Notes
- **Daily Standup**: 15 minutes each morning (virtual or in-person)
- **Code Reviews**: Pair review before each integration session
- **Git Strategy**: Feature branches → Daily merge to `develop` → Final merge to `main`
- **Communication**: Use group chat for quick questions, save detailed discussions for integration sessions
- **Module Ownership**: Each member fully owns their module (backend + RMI + GUI)
- **2-Member Workflow**: M1 handles Auth+Seller+Admin, M2 handles Bidding+Gallery+Real-time; both collaborate on integration
- **Shared Interface**: Any changes to `IAuctionService.java` must be discussed and agreed upon by both members before implementation
