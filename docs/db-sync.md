DatabaseSyncService
===================

Purpose
-------
Ensures user records are kept consistent across local SQLite files used by Member-1 and other processes.

Behavior
--------
- Runs at server startup (immediate run) and every 60 seconds by default.
- Sync scope: `users` table only (idempotent INSERT/REPLACE operations).
- Best-effort: primary writes occur to the canonical DB; secondary writes are attempted and do not cause registration to fail if they fail.

Configuration & Control
-----------------------
- Disable background sync by setting environment variable `RTDAS_DB_SYNC=false` or JVM property `-Drtdas.db.sync=false`.

Backups & Safety
----------------
- Migrations and merge operations create timestamped backups in the `data/` directory (look for files ending with `.bak*`).
- To restore: stop the server, replace the DB file with the backup, then restart the server.

Logs & Verification
-------------------
- On startup the server prints the canonical DB path in logs:

  [RTDAS] Using database file: <absolute-path>

- Sync service logs appear in the server output; search for `DatabaseSyncService` markers to verify runs.

Quick checks
------------
- Use the `TestRegisterLogin` tool to register/login test users via RMI (see docs/dev-tools.md).
- Use `QueryUsers` or a SQLite client to inspect `data/auction.db` and `data/auction.db.sqlite`.

Notes
-----
- The service is intentionally narrow in scope and is NOT a substitute for a proper replication solution. It was implemented to resolve historic issues caused by different working directories and multiple DB copies.
