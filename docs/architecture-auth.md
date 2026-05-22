RMI & Authentication (server)
=============================

Overview
--------
This document summarizes the RMI contract and authentication behavior implemented by the server.

IAuctionService (key methods)
- `String login(String username, String password)` — returns a session token when credentials match.
- `void register(String username, String password, String role)` — registers a new user (role should be `ADMIN` or `USER`).
- `String getSessionRole(String token)` — returns `ADMIN` or `USER` for a valid session token.
- `void logout(String token)` — invalidates the session token.

Session semantics
- Tokens are stored in-memory on the server in a sessions map and include a TTL. The TTL is sliding: validated activity refreshes expiration.
- Authentication uses SHA-256 password hashing (see `SecurityUtil.hashPassword`).
- Role model: only `ADMIN` and `USER` are supported (legacy `SELLER`/`BIDDER` values are normalized during migration).

Server lifecycle interactions
- The session store is ephemeral (in-memory). For demo/production setups where sessions must survive restarts, consider an external session store.
- The `DatabaseSyncService` is started with the server bootstrap and runs alongside the RMI service to reconcile user records across local DB files.

Rate limiting & safety
- Login/register endpoints include basic rate limiting to defend against brute force; monitor logs for repeated failures.

Related code
------------
- `AuctionServiceImpl`: src/main/java/com/auction/server/service/AuctionServiceImpl.java — implements `login`, `register`, `getSessionRole`, and session token handling.
- `ServerBootstrap`: src/main/java/com/auction/server/core/ServerBootstrap.java — starts RMI, repositories, and the `DatabaseSyncService` during server lifecycle.
- `SecurityUtil`: src/main/java/com/auction/server/util/SecurityUtil.java — password hashing helper (SHA-256).

