# RTDAS — Locked Decisions, Update Checklist & Dev Roadmap

Status: pre-implementation. All design questions resolved below.
Scope: PRD, DESIGN.md, architecture.md, database.md, networking.md, ui.md, README.md, code skeleton (interfaces, models, constants, repositories, services).
Outcome target: a coherent, university-grade-but-well-implemented spec ready to code against, plus a phased roadmap that touches every inconsistent file.

---

## 0. Final Decisions (locked)

| # | Decision | Choice |
|---|---|---|
| D1 | Default admin credentials | **`admin` / `admin`**. Update `Constants.java` (currently `abelmekonen/demo123`). PRD/README already say this. |
| D2 | Per-call authentication on RMI | **Session tokens.** `login()` returns a UUID token; mutating calls require it; server keeps `ConcurrentHashMap<String, Session>` with TTL + `logout(token)`. No JWT. |
| D3 | Rate limiting | **Sliding-window per-IP limiter on `login()` and `placeBid()` only.** Polling reads are unlimited. IP via `RemoteServer.getClientHost()`. |
| D4 | Real-time update mechanism | **Keep 2 s short polling.** No long polling, no RMI callbacks. |
| D5 | First-bid rule | If no bids: `amount >= startingPrice`. Else: `amount >= currentBid * 1.05`. Plus `amount > 0` and `Double.isFinite(amount)`. |
| D6 | Snipe protection cap | **Hard `cap_end_time` set at auction creation.** Default = `endTime + 10 minutes`. Extensions can never push `endTime` past `cap_end_time`. |
| D7 | Reaper ↔ placeBid race | **Same per-auction `ReentrantLock`.** Both re-check `status == ACTIVE && now < endTime` inside the lock. |
| D8 | Bid commit atomicity | **Single SQL transaction**: insert bid + update `auction_items.current_bid`/`highest_bidder_username`/`end_time`. `setAutoCommit(false)`, commit/rollback. |
| D9 | Money representation | **`long cents` end-to-end** (DB, RMI, models). Format only at the UI boundary. |
| D10 | Lock-map cleanup | Acceptable to leave entries (small N for a demo). Documented, not engineered. |
| D11 | Server-side auction cache | **None.** DB is the source of truth. The "Collections" requirement is satisfied by the lock map and other internal collections. README's `HashMap` mention will be removed. |
| D12 | Defensive copies | DB layer always returns **fresh DTOs**; never share mutable instances across threads/clients. |
| D13 | DB backup | Use SQLite **online backup** via `VACUUM INTO 'data/backup-<ts>.db'` then return its bytes. |
| D14 | Bidder "My Activity" view | **In scope.** Tabs: My Bids, Won, Outbid. |
| D15 | Cancel rules | Only when `status == ACTIVE` **and** zero bids. |
| D16 | Relist semantics | **New auction row** with `relisted_from INTEGER` FK. Original `EXPIRED` row is immutable. Allowed only from `EXPIRED`. New end_time must be in the future. |
| D17 | Timestamp format & clock | All timestamps stored/transported as **ISO-8601 UTC with `Z`** (e.g., `2026-05-09T15:46:36Z`). New RMI method `String serverTime()` lets clients compute an offset and use server-time for countdowns. |
| D18 | UDP discovery packet | `RTDAS|v1|<rmiPort>|<serverName>|<serverId>|<rmiHost>`. Client prefers payload `rmiHost`, falls back to `DatagramPacket.getAddress()`. |
| D19 | Multi-NIC | Document `-Djava.rmi.server.hostname=<chosen-IP>` in the demo runbook. |
| D20 | Reconnect UX | Persist last-successful server in `~/.rtdas/last_server`. After 3 consecutive polling failures show a "Connection lost" banner with Reconnect / Choose Another Server. |
| D21 | Schema hardening | `PRAGMA foreign_keys = ON;` on every connection. Indexes: `bids(auction_id)`, `auction_items(status, end_time)`, `auction_items(seller_username)`. CHECK on `status`, `amount > 0`, prices `>= 0`. |
| D22 | Image format | **Re-encode all uploads to JPG** on save; strip EXIF; center-crop square then scale for thumbnails. |
| D23 | Client image cache | In-memory `Map<String, byte[]>` keyed `auctionId:index` to avoid re-downloads. Cleared on logout. |
| D24 | Free-text gallery search | **Dropped.** Category filter + sort only. Update DESIGN.md. |
| D25 | UI animations | Stretch goals only. Core components: Card, Button, TextField, ListView, TableView, ComboBox, Alert, FileChooser. |
| D26 | First-run filesystem | Server auto-creates `data/`, `logs/`, `resources/images/`, `resources/thumbs/`, `exports/` on startup. No manual mkdir. |
| D27 | Seed script | **Yes.** Optional `mvn exec:java -Dexec.mainClass=...DemoSeeder` that creates 2 sellers, 3 bidders, 5 auctions with placeholder images. |
| D28 | Audit log | Append-only text. Drop the "tamper-resistant" claim from `database.md`. No rotation (documented). |
| D29 | CSV format | RFC 4180 escaping. `Winner = ""` for non-SOLD rows. Includes auctions of all statuses for the calling seller. Server filters with parameterized SQL `WHERE seller_username = ?`. |
| D30 | Login error messaging | Single generic message ("Invalid username or password"). |

These are final; the roadmap below assumes them.

---

## 1. Update Checklist (every inconsistent artifact)

Each item lists **file → exact change**. This is the master "nothing missed" list — the roadmap in §2 schedules them.

### 1.1 Documentation

- [x] **README.md** — replace the duplicated spec with a short overview + link to PRD. Remove: `importAuctionsFromCSV`, `active BOOLEAN`, H2 alternative, `HashMap` server cache, "manageUsers()". Add: link to PRD, demo runbook pointer.
- [x] **docs/RTDAS_PRD.md** — apply edits per §1.4 below.
- [x] **docs/architecture.md** — add: snipe cap, locking discipline, atomic bid commit, server-time clock authority. Remove "tamper-resistant" wording.
- [x] **docs/database.md** — add full schema (tables, columns, types, indexes, CHECK constraints, FKs, `PRAGMA foreign_keys`). Add `relisted_from` column. Document online backup. Remove tamper-resistant claim.
- [x] **docs/networking.md** — add UDP packet v1 schema, `serverTime()` method, reconnect UX, multi-NIC note.
- [x] **docs/ui.md** — drop free-text search, drop animation requirements (mark as stretch), align with DESIGN.md component scope.
- [x] **docs/DESIGN.md** — drop "search bar" and FAB if not implemented; reduce animation list to "stretch"; explicitly list AtlantaFX components used.

### 1.2 Code skeleton

- [x] **`shared/Constants.java`** — set admin to `admin/admin`; add `SNIPE_CAP_DEFAULT_MINUTES = 10`; add `SESSION_TTL_MINUTES`; add money helpers (e.g., cents formatter).
- [x] **`shared/interfaces/IAuctionService.java`** — add `String login(...)` returning token (or wrapping object); add logout(token); add `serverTime()`; add `String token` arg to `placeBid`, `createAuction`, `cancelAuction`, `relistAuction`, `createUser`, `getAllUsers`, `backupDatabase`, `getAuditLogs`, `exportAuctionsToCSV`. Add `relistAuction(int auctionId, String newEndTimeIso, String token)`. Add Bidder activity reads: `List<Bid> getMyBids(token)`, `List<AuctionItem> getMyWonAuctions(token)`. Money args become `long amountCents` and `long clientExpectedPriceCents`.
- [x] **`shared/models/AuctionItem.java`** — change `double startingPrice/currentBid` to `long startingPriceCents/currentBidCents`. Add `Long capEndTime` (ISO string, UTC). Add `Integer relistedFrom`. Update Javadoc to specify UTC `Z` timestamps.
- [x] **`shared/models/Bid.java`** — `long amountCents`; UTC timestamp.
- [ ] **`shared/models/User.java`** — confirm `passwordHash`, `roleType`. Ensure no plaintext password ever leaves the server.
- [x] **`shared/exceptions/`** — add `UnauthorizedException` (bad/missing token), `SnipeCapReachedException` (optional — could fold into AuctionClosedException).
- [x] **`server/repository/DatabaseManager.java`** — set `PRAGMA foreign_keys = ON` per connection; create directories on init; create indexes; add `relisted_from` column.
- [x] **`server/repository/AuctionRepository.java`** — long-cents columns; `findActiveExpired()` query for the reaper; update with snipe-extended `end_time`; insert-with-relisted_from; transactional `placeBidAndUpdate(...)`.
- [x] **`server/repository/BidRepository.java`** — long-cents amount; `findByBidder(username)` for the activity view.
- [x] **`server/repository/UserRepository.java`** — verify `findAll`, `findByUsername`, `insert`. Username uniqueness enforced at DB.
- [ ] **`server/service/AuctionServiceImpl.java`** — implement: session map, per-auction `ReentrantLock` map, `placeBid` flow with all D5/D6/D7/D8 rules, `relistAuction`, `serverTime`, activity reads, online-backup.
- [ ] **`server/service/AuctionReaper.java`** — acquire per-auction lock before transitioning. On startup, sweep overdue ACTIVE rows.
- [ ] **`server/repository/FileHandler.java`** (or `ImageManager.java`) — JPG re-encode, EXIF strip, center-crop thumbnail, placeholder-on-missing.
- [ ] **`server/util/SecurityUtil.java`** — SHA-256 helper; constant-time comparison; `generateToken()`.
- [ ] **`server/core/UdpBroadcaster.java`** — emit v1 payload incl. server IP and serverId. Pick the broadcast NIC explicitly.
- [ ] **`server/core/ServerLauncher.java`** — directory bootstrap; `java.rmi.server.hostname` reminder; reaper startup sweep.
- [ ] **`client/network/UdpDiscoveryClient.java`** — parse v1 payload; tolerate unknown future fields.
- [ ] **`client/network/RmiClientProvider.java`** — health-check via `serverTime()` on connect; persist last server; offset-based clock.
- [ ] **`client/controllers/*`** — improve user dashboard (`user_dashboard.fxml` + controller); cleanup polling on view exit; shake/red animation on stale-price exception; "Timer Extended" toast on snipe.
- [ ] **`client/service/PollingService.java`** — also used by gallery, not only detail. Backoff + reconnect banner after 3 failures.
- [ ] **`pom.xml`** — confirm AtlantaFX dependency; pin Java 17; `maven-compiler-plugin` source/target; `maven-shade-plugin` (already present?) verified.

### 1.3 Tests (JUnit 5, in-memory SQLite)

- [ ] Repository tests: schema creation, CRUD, FK + CHECK enforcement, indexes used (sanity).
- [ ] `AuctionServiceImpl.placeBid` happy path + every rejection branch (D5 cases incl. NaN/Infinity/negative).
- [ ] **Race test:** two threads, same auction, both with valid `clientExpectedPriceCents`. Exactly one wins; the other gets `StaleDataException`.
- [ ] **Reaper-vs-bid race test:** end in 50 ms; bid placed during reap → outcome consistent with locking rules.
- [ ] Snipe cap: extension never exceeds `cap_end_time`.
- [ ] Relist: only from EXPIRED, new row, future end_time required.
- [ ] CSV escaping: titles with commas, quotes, newlines round-trip correctly.
- [ ] Image: missing file → placeholder; oversized upload rejected on server.
- [ ] Auth: missing/bad token → `UnauthorizedException`; admin-only methods reject non-admin tokens.
- [ ] Rate limiter: floods on `login` and `placeBid` are throttled; reads unaffected.

### 1.4 PRD concrete edits

- [x] 1. Replace double prices with cents (long) everywhere.
- [x] 2. State the explicit bid acceptance predicate (D5).
- [x] 3. Add `cap_end_time` to the AuctionItem model and snipe section (D6).
- [x] 4. Add session-token auth model with login/logout (D2).
- [x] 5. Add `serverTime()` and UTC `Z` rule (D17).
- [x] 6. Add `relisted_from` column and "new row" relist (D16).
- [x] 7. Add Bidder "My Activity" view to user stories and views table (D14).
- [x] 8. Update RMI contract list to match `IAuctionService` after edits.
- [x] 9. Update §4 (DB) with full schema, indexes, `PRAGMA foreign_keys`, `relisted_from` (D21).
- [x] 10. Update §6 (Image handling) with JPG re-encode + center-crop (D22).
- [x] 11. Update §5 (Concurrency) with locking discipline + transactional bid commit (D7, D8).
- [x] 12. Replace UDP packet description with v1 schema (D18).
- [x] 13. Add reconnect UX & persisted-last-server (D20).
- [x] 14. Add demo-runbook items: directory bootstrap, `java.rmi.server.hostname`, seed script (D19, D26, D27).
- [x] 15. Drop "tamper-resistant" wording from audit log description (D28).

---

## 2. Dev Roadmap (phased, dependency-ordered)

Each phase is mergeable on its own. Run repo tests at the end of every phase.

### Phase 0 — Spec sync (1 sitting)
Goal: zero documentation drift before any code changes.
- Apply all edits in §1.1 and §1.4.
- Cross-link PRD ↔ README ↔ docs/*.md.
- **Exit criterion:** every doc reads consistently; default admin = `admin/admin` everywhere.

### Phase 1 — Foundations (constants, money, schema, exceptions)
Goal: lock the data shape so nothing has to be re-typed later.
- Update `Constants.java` (D1, snipe cap, session TTL).
- Convert money to `long cents` in `AuctionItem`, `Bid`, repository signatures, SQL types (`INTEGER`).
- Add `relisted_from`, `cap_end_time` columns; `PRAGMA foreign_keys`; indexes (D21).
- Add new exception classes (`UnauthorizedException`, `SnipeCapReachedException` if used).
- Repository tests: schema, FK on, CHECKs.
- **Exit criterion:** `mvn test` green on repository layer with new schema.

### Phase 2 — Auth + RMI contract update
- Update `IAuctionService`: `login` → token, `logout`, `serverTime`, token args on mutators, money in cents, Bidder activity reads, `relistAuction`.
- Server: `SessionManager` (token map + TTL), `SecurityUtil.generateToken`.
- Tests: token lifecycle, logout, expiry.
- **Exit criterion:** all RMI methods authenticate; admin-only ones authorize.

### Phase 3 — Bidding engine (the heart)
- Per-auction `ReentrantLock` map.
- `placeBid` with full predicate (D5), stale-price (cents-exact), self-bid, duplicate-bid, snipe extension capped at `cap_end_time` (D6), inside one DB transaction (D8).
- `AuctionReaper` using the same locks + startup sweep.
- Tests: the full matrix incl. race + reaper-vs-bid + snipe cap.
- **Exit criterion:** concurrency tests deterministic and green.

### Phase 4 — Auction lifecycle complete
- `createAuction`, `cancelAuction` (D15), `relistAuction` (D16) with all validations.
- CSV export with RFC 4180 escaping (D29).
- Online DB backup via `VACUUM INTO` (D13).
- Audit log writer.
- Tests: lifecycle transitions; CSV escaping; backup integrity.
- **Exit criterion:** seller flow fully usable headlessly.

### Phase 5 — Image pipeline
- Server: JPG re-encode, EXIF strip, center-crop thumbnail (D22).
- Server: placeholder bytes on missing file.
- Client: in-memory image cache (D23).
- Tests: oversize rejection, missing-file placeholder, thumbnail dimensions.
- **Exit criterion:** image round-trip works end-to-end with placeholders covering edge cases.

### Phase 6 — Networking & discovery
- UDP v1 payload (D18), `serverTime()` wired (D17).
- Multi-NIC docs + `-Djava.rmi.server.hostname` (D19).
- Client persists last server, reconnect banner after 3 polling failures (D20).
- **Exit criterion:** two laptops on Wi-Fi auto-discover and reconnect cleanly after a brief Wi-Fi drop.

### Phase 7 — Client UI (JavaFX)
- Connect screen (with discovered list + manual + last-server prefill).
- Login → role-routing.
- Gallery (filter + sort, polling, no search per D24).
- Auction detail (countdown using server-time offset, bid input, history).
- User Dashboard (status tabs, create/cancel/relist, CSV save dialog).
- **User Dashboard** (D14): My Bids / Won / Outbid.
- Admin panel (create user, list users, backup, last-N audit logs).
- Stale-price shake animation, snipe "Timer Extended" toast.
- **Exit criterion:** full role-based flows work in a 2-client demo.

### Phase 8 — Demo polish
- Auto-create directories on server start (D26).
- Seed script (D27).
- Demo runbook in README (firewall prompt, hostname flag, reset instructions).
- **Exit criterion:** clean checkout → `mvn package` → server up → 4 clients connect → simultaneous-bid scenario works on first try.

---

## 3. Where we start

**Start at Phase 0 (Spec sync).** It's read-only-of-code and pure documentation, so it cannot break anything, and it eliminates every cross-document contradiction before a single line of business logic is touched. After Phase 0, Phase 1 (foundations) is the next safest move because every later phase depends on the cents-typed schema and the new exception/column additions.

Concretely the very first commit-sized chunk of work is:

1. README.md rewrite (short overview + pointer to PRD).
2. PRD edits per §1.4 items 1–15.
3. Sync architecture.md / database.md / networking.md / ui.md / DESIGN.md.

After that, we move to Phase 1 with a clean spec to code against.

---

## 4. Loose-ends check (anything not covered?)

Cross-checked against your summary and the original docs:

- ✅ Auth model — D2.
- ✅ Rate limiting scope — D3.
- ✅ Polling vs long polling — D4.
- ✅ Bidding rules incl. first bid, snipe cap, race, atomicity, money — D5–D9.
- ✅ Lock map cleanup, server cache removal, defensive copies, backup — D10–D13.
- ✅ Bidder activity, cancel rules, relist semantics, time zone — D14–D17.
- ✅ UDP packet, multi-NIC, reconnect — D18–D20.
- ✅ Schema hardening, image format, image cache — D21–D23.
- ✅ Free-text search dropped, animations as stretch — D24–D25.
- ✅ Directory bootstrap, seed script — D26–D27.
- ✅ Audit log honesty, CSV format, login error messaging — D28–D30.
- ✅ Tests: race, reaper, CSV escape, auth — §1.3.
- ✅ All seven open questions from your summary are answered in §0.

Nothing material is left unspecified.
