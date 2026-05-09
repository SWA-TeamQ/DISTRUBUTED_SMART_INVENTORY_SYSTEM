# Real-Time Distributed Auction System (RTDAS)

A distributed, multi-user English auction platform demonstrating advanced Java concepts: OOP, collections, multithreading, file I/O, JDBC, and Java RMI.

## Quick Start

```bash
# Terminal 1 - Start the server
mvn exec:java -Dexec.mainClass="com.auction.server.core.ServerLauncher"

# Terminal 2 - Launch a client
mvn javafx:run
```

## Documentation

| Document | Purpose |
|----------|---------|
| [docs/table-of-contents.md](docs/table-of-contents.md) | Master index of all documentation |
| [docs/RTDAS_PRD.md](docs/RTDAS_PRD.md) | Product requirements, user stories, scope |
| [docs/DESIGN.md](docs/DESIGN.md) | UI/UX design, screen layouts, interactions |
| [docs/architecture.md](docs/architecture.md) | System architecture, RMI contract, concurrency model |
| [docs/database.md](docs/database.md) | Database schema, constraints, backup strategy |
| [docs/networking.md](docs/networking.md) | UDP discovery, RMI config, reconnect logic |
| [docs/demo-runbook.md](docs/demo-runbook.md) | Demo day setup and troubleshooting guide |

## Tech Stack

| Layer | Technology |
|-------|------------|
| GUI | JavaFX 17+, AtlantaFX PrimerDark |
| Networking | Java RMI, UDP broadcast |
| Persistence | SQLite (JDBC), CSV export |
| Concurrency | ReentrantLock, ScheduledExecutorService |

## License

See [LICENSE](LICENSE).