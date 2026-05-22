Server lifecycle & verification notes
==================================

Startup verification
--------------------
- When the server starts it prints the canonical database path. Look for a log line like:

  [RTDAS] Using database file: D:\Real Time Distributed Auction System\data\auction.db

- Confirm the path above and use a SQLite client or `QueryUsers` utility to inspect the file.

DatabaseSyncService
-------------------
- The `DatabaseSyncService` is started by `ServerBootstrap` and runs periodically to reconcile user records across `data/auction.db` and `data/auction.db.sqlite`.
- If you plan to run multiple server processes against the same directory, disable the sync service and use a proper DB setup.

Troubleshooting
---------------
- If registrations appear in the client but not in the DB you inspected, check the startup log for the canonical DB path — working-directory differences often cause multiple DB copies.
- Backups created during migrations live in the `data/` folder and are timestamped; use them to recover previous state.

Quick reproduction steps
-----------------------
1. Start server.
2. Run `TestRegisterLogin` to create a test user and obtain a token.
3. Use `QueryUsers` or a SQLite client to confirm the user exists in the canonical DB path printed at startup.

Related code
------------
- `ServerBootstrap`: src/main/java/com/auction/server/core/ServerBootstrap.java — starts/stops `DatabaseSyncService` and RMI components.
- `DatabaseSyncService`: src/main/java/com/auction/server/repository/DatabaseSyncService.java — background sync implementation.

