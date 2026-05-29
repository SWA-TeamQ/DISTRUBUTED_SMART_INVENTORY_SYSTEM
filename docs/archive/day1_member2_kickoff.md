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

_Generated on May 21, 2026 — for `features/member2` branch._
