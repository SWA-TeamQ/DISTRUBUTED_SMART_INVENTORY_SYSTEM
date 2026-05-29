# RTDAS — Locked Decisions, Update Checklist & Dev Roadmap

Status: pre-implementation. All design questions resolved below.
Scope: PRD, DESIGN.md, architecture.md, database.md, networking.md, ui.md, README.md, code skeleton (interfaces, models, constants, repositories, services).
Outcome target: a coherent, university-grade-but-well-implemented spec ready to code against, plus a phased roadmap that touches every inconsistent file.

---

## 0. Final Decisions (locked)

| #   | Decision                       | Choice                                                                                                                                                                                                 |
| --- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --- | --------- | ------------ | ---------- | ------------------------------------------------------------------------------------------ |
| D1  | Default admin credentials      | **`admin` / `admin`**. Update `Constants.java` (currently `abelmekonen/demo123`). PRD/README already say this.                                                                                         |
| D2  | Per-call authentication on RMI | **Session tokens.** `login()` returns a UUID token; mutating calls require it; server keeps `ConcurrentHashMap<String, Session>` with TTL + `logout(token)`. No JWT.                                   |
| D3  | Rate limiting                  | **Sliding-window per-IP limiter on `login()` and `placeBid()` only.** Polling reads are unlimited. IP via `RemoteServer.getClientHost()`.                                                              |
| D4  | Real-time update mechanism     | **Keep 2 s short polling.** No long polling, no RMI callbacks.                                                                                                                                         |
| D5  | First-bid rule                 | If no bids: `amount >= startingPrice`. Else: `amount >= currentBid * 1.05`. Plus `amount > 0` and `Double.isFinite(amount)`.                                                                           |
| D6  | Snipe protection cap           | **Hard `cap_end_time` set at auction creation.** Default = `endTime + 10 minutes`. Extensions can never push `endTime` past `cap_end_time`.                                                            |
| D7  | Reaper ↔ placeBid race         | **Same per-auction `ReentrantLock`.** Both re-check `status == ACTIVE && now < endTime` inside the lock.                                                                                               |
| D8  | Bid commit atomicity           | **Single SQL transaction**: insert bid + update `auction_items.current_bid`/`highest_bidder_username`/`end_time`. `setAutoCommit(false)`, commit/rollback.                                             |
| D9  | Money representation           | **`long cents` end-to-end** (DB, RMI, models). Format only at the UI boundary.                                                                                                                         |
| D10 | Lock-map cleanup               | Acceptable to leave entries (small N for a demo). Documented, not engineered.                                                                                                                          |
| D11 | Server-side auction cache      | **None.** DB is the source of truth. The "Collections" requirement is satisfied by the lock map and other internal collections.                                                                        |
| D12 | Defensive copies               | DB layer always returns **fresh DTOs**; never share mutable instances across threads/clients.                                                                                                          |
| D13 | DB backup                      | Use SQLite **online backup** via `VACUUM INTO 'data/backup-<ts>.db'` then return its bytes.                                                                                                            |
| D14 | Bidder "My Activity" view      | **In scope.** Tabs: My Bids, Won, Outbid.                                                                                                                                                              |
| D15 | Cancel rules                   | Only when `status == ACTIVE` **and** zero bids.                                                                                                                                                        |
| D16 | Relist semantics               | **New auction row** with `relisted_from INTEGER` FK. Original `EXPIRED` row is immutable. Allowed only from `EXPIRED`. New end_time must be in the future.                                             |
| D17 | Timestamp format & clock       | All timestamps stored/transported as **ISO-8601 UTC with `Z`** (e.g., `2026-05-09T15:46:36Z`). New RMI method `String serverTime()` lets clients compute an offset and use server-time for countdowns. |
| D18 | UDP discovery packet           | `RTDAS                                                                                                                                                                                                 | v1  | <rmiPort> | <serverName> | <serverId> | <rmiHost>`. Client prefers payload `rmiHost`, falls back to `DatagramPacket.getAddress()`. |
| D19 | Multi-NIC                      | Document `-Djava.rmi.server.hostname=<chosen-IP>` in the demo runbook.                                                                                                                                 |
| D20 | Reconnect UX                   | Persist last-successful server in `~/.rtdas/last_server`. After 3 consecutive polling failures show a "Connection lost" banner with Reconnect / Choose Another Server.                                 |
| D21 | Schema hardening               | `PRAGMA foreign_keys = ON;` on every connection. Indexes: `bids(auction_id)`, `auction_items(status, end_time)`, `auction_items(seller_username)`. CHECK on `status`, `amount > 0`, prices `>= 0`.     |
| D22 | Image format                   | **Re-encode all uploads to JPG** on save; strip EXIF; center-crop square then scale for thumbnails.                                                                                                    |
| D23 | Client image cache             | In-memory `Map<String, byte[]>` keyed `auctionId:index` to avoid re-downloads. Cleared on logout.                                                                                                      |
| D24 | Free-text gallery search       | **Dropped.** Category filter + sort only. Update DESIGN.md.                                                                                                                                            |
| D25 | UI animations                  | Stretch goals only. Core components: Card, Button, TextField, ListView, TableView, ComboBox, Alert, FileChooser.                                                                                       |
| D26 | First-run filesystem           | Server auto-creates `data/`, `logs/`, `resources/images/`, `resources/thumbs/`, `exports/` on startup. No manual mkdir.                                                                                |
| D27 | Seed script                    | **Yes.** Optional `mvn exec:java -Dexec.mainClass=...DemoSeeder` that creates 2 sellers, 3 bidders, 5 auctions with placeholder images.                                                                |
| D28 | Audit log                      | Append-only text. Drop the "tamper-resistant" claim from `database.md`. No rotation (documented).                                                                                                      |
| D29 | CSV format                     | RFC 4180 escaping. `Winner = ""` for non-SOLD rows. Includes auctions of all statuses for the calling seller. Server filters with parameterized SQL `WHERE seller_username = ?`.                       |
| D30 | Login error messaging          | Single generic message ("Invalid username or password").                                                                                                                                               |

These are final; the roadmap below assumes them.
