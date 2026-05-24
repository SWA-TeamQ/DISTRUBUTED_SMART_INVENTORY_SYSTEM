# Day 1 Kickoff Checklist — Member 2 (Evening Integration)

**Time & Participants**
- Evening sync (1.5h) — M1, M2, optional observer.

**Goal**
- Verify Connect → Login → Gallery → Auction Detail → Poll → Bid end-to-end; confirm role routing and no UI freezes.

**Environment Prep**
- Ensure server built and running, clients on `features/member2` branch.
- Confirm sample auctions exist (or load test data).

**Pre-meeting Build Steps**
```bash
mvn -DskipTests clean package
# Run server (IDE or):
java -jar target/<server-jar>.jar
```

**Connection Checklist**
- [ ] UDP discovery/manual connect works.
- [ ] `LoginController` authenticates via RMI and navigates to bidder dashboard.

**Polling & Bidding Tests**
- Start `PollingService` for selected auction.
- Verify UI updates current bid and bid history every poll.
- Attempt a bid from client A; verify client B sees update within one poll cycle.
- Verify UI disables bid input when auction state becomes `EXPIRED` or `SOLD`.

**Failure & Edge Cases**
- Simulate RMI failure: polling should show error and retry/backoff without freezing UI.
- Simulate `LifecycleManager` expiry during active poll: polling stops for that auction and UI shows final state.

**Acceptance Criteria (pass/fail)**
- Pass: Polling updates visible, bids accepted only when `ACTIVE`, no UI thread freezes, role routing correct.
- Fail: UI freeze, uncaught exceptions, bids accepted when auction not `ACTIVE`.

**Artifacts to Commit After Session**
- Small focused commits for `PollingService` and controller wiring on `features/member2`.
- Include short PR description and note any `IAuctionService` incompatibilities.

**Rollback Plan**
- If integration breaks, revert to morning stable commit and log errors for Day 2 fix.

**Next Steps Post-Integration**
- If server API changed, coordinate with M1 to apply minimal client patches to `AuctionDetailController`/`GalleryController`.
- If polling shows race conditions, schedule immediate fix: strengthen client-side checks and re-validate auction `ACTIVE` before placing bids.

---

*Generated on May 21, 2026 — for `features/member2` branch.*

## Step-by-step Tasks (Priority: High → Low)

1. Build & API compatibility check (P0 — Highest)
	- Action: Run a quick build and compile to detect any server-side API/signature changes.
	- Why: Prevent wasted UI work if `IAuctionService` or server DTOs changed.
	- Files to inspect/update if needed: [Real-Time-Distributed-Auction-System/src/main/java/com/auction/shared/interfaces/IAuctionService.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/shared/interfaces/IAuctionService.java), [Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/network/RmiClientProvider.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/network/RmiClientProvider.java)

2. Harden client RMI connector and error handling (P0)
	- Action: Ensure `RmiClientProvider` reconnects gracefully and exposes `getService()` safely.
	- Files: [Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/network/RmiClientProvider.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/network/RmiClientProvider.java)

3. Implement / verify `PollingService` behavior (P0)
	- Action: Confirm polling interval, start/stop, pause/resume, and that callbacks run UI-thread safe (`Platform.runLater`). Add retry/backoff for RMI errors.
	- Files: [Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/service/PollingService.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/service/PollingService.java)

4. Wire polling into `AuctionDetailController` (P0)
	- Action: Subscribe to `PollingService` updates, update highest bid and bid history on the JavaFX thread, disable bid UI when auction state != `ACTIVE`.
	- Files: [Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/controllers/AuctionDetailController.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/controllers/AuctionDetailController.java)

5. Gallery data fetch and navigation (P1)
	- Action: Fetch active auctions via RMI, render cards with lazy thumbnail loading, navigate to detail view and start polling for selected auction.
	- Files: [Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/controllers/GalleryController.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/controllers/GalleryController.java)

6. Client-side bid placement safety (P1)
	- Action: Before sending a bid RPC, re-check local view of auction state (from latest poll); block requests if not `ACTIVE`. Handle `RemoteException` and show user-friendly messages.
	- Files: [Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/controllers/AuctionDetailController.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/controllers/AuctionDetailController.java), [Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/network/RmiClientProvider.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/client/network/RmiClientProvider.java)

7. Quick integration tests & manual scenarios (P2)
	- Action: Manual test matrix: connect/login, gallery open, detail open, poll updates, bid from two clients, simulate server expiration during poll.
	- Files / resources: `src/main/resources/fxml/auction_detail.fxml`, `src/main/resources/fxml/gallery.fxml` ([Real-Time-Distributed-Auction-System/src/main/resources/fxml/auction_detail.fxml](Real-Time-Distributed-Auction-System/src/main/resources/fxml/auction_detail.fxml), [Real-Time-Distributed-Auction-System/src/main/resources/fxml/gallery.fxml](Real-Time-Distributed-Auction-System/src/main/resources/fxml/gallery.fxml))

8. Review server refactor impact & coordinate (P2)
	- Action: Check for new server modules (`LockManager`, `LifecycleManager`) and confirm `IAuctionService` behavior; if server expects `User` objects in core APIs, coordinate to pass session `User` or adapt client wrapper.
	- Files to review (server-side, informational): `src/main/java/com/auction/server/service/AuctionServiceImpl.java` ([Real-Time-Distributed-Auction-System/src/main/java/com/auction/server/service/AuctionServiceImpl.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/server/service/AuctionServiceImpl.java)), `src/main/java/com/auction/server/core/ServerLauncher.java` ([Real-Time-Distributed-Auction-System/src/main/java/com/auction/server/core/ServerLauncher.java](Real-Time-Distributed-Auction-System/src/main/java/com/auction/server/core/ServerLauncher.java))

9. Logging, metrics and UI resilience (P3 — lower)
	- Action: Add logging around poll start/stop, RMI failures, and bid attempts; surface short error messages in UI and telemetry for later debugging.
	- Files: `PollingService.java`, `AuctionDetailController.java`, `GalleryController.java` (same links above)

10. Polish & follow-ups (P4 — lowest)
	 - Action: Optimize thumbnail lazy-loading, fine-tune polling interval, document any API mismatches in PR notes.
	 - Files: Frontend controllers and `PollingService` files referenced above.

Each task above is intentionally small and focused. If you'd like, I can now:
- run a quick build to detect API breaks (and propose minimal client patches), or
- implement the highest-priority client changes (`PollingService` improvements + controller wiring) and open focused commits.
