# M2 Demo Plan — RTDAS

Goal: record a 4–7 minute demo showing the stable gallery→detail→bid flow, connection-loss handling, and high-concurrency behavior.

Segments:

- Intro (20s): one-sentence problem statement and what's being shown.
- Start server + client (30s): show terminal commands to start server and client. Use existing VS Code task `Run RTDAS Server` for server.
- Gallery (60s): show category filter, sort, refresh, and open an auction detail (hero image).
- Auction Detail (90s): show server-time countdown, bid-history refresh, place a bid, and show immediate UI updates.
- Connection loss (45s): simulate network loss (stop server), show reconnect banner, then restart server and show recovery.
- High-concurrency note (30s): mention stress test results and show test summary output (console). Optionally play a short snippet of test run output.
- Wrap-up (20s): call-to-action, PR link, and where code lives.

Prep checklist:

- [ ] Ensure branch `task/member2` is pushed and tests pass locally.
- [ ] Close other apps that may show notifications.
- [ ] Set screen recording to 1920x1080, 30fps, 1280kbps audio.
- [ ] Have terminal and app windows sized for readability.

Recording commands (PowerShell) — start server:

```powershell
$root = 'd:\Real Time Distributed Auction System\Real-Time-Distributed-Auction-System'
Set-Location $root
mvn -DskipTests compile exec:java
```

Recording commands — run targeted tests (optional clip):

```powershell
Set-Location $root
mvn -Dtest=com.auction.stress.ConcurrentBiddingHighStressTest test
```

How to simulate connection loss during demo:

- In the server terminal, press Ctrl+C to stop the server; wait to show the reconnect banner on the client.
- Restart server with the same command and show the client auto-reconnect.

Deliverables I will produce if you want:

- `docs/M2_demo_plan.md` (this file)
- `scripts/record_demo.ps1` — helper to start server/test runs
- Short script/narration lines for each segment (ready-to-read)

Next step: tell me which deliverables you want me to create now (script, narration, or attempt to start server for a recording rehearsal).
