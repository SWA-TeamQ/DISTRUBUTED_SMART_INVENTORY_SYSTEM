# Real-Time Distributed Auction System (RTDAS)

A distributed, multi-user English auction platform demonstrating advanced Java concepts: OOP, collections, multithreading, file I/O, JDBC, and Java RMI.

## Key Architectural Features

- **Deep Module Design**: Core domain logic is encapsulated in dedicated managers (`AuctionManager`, `LifecycleManager`) rather than being scattered in the networking layer.
- **Transactional Bidding**: 5% minimum increment, self-bid prevention, and optimistic locking to handle concurrent bids safely.
- **Snipe Protection**: Automatic auction extension if a bid is placed in the final 30 seconds.
- **Background Lifecycle**: A dedicated `AuctionReaper` daemon manages state transitions (ACTIVE -> SOLD/EXPIRED) independently of user activity.

## Quick Start

```bash
# Terminal 1 - Start the server
mvn exec:java

# Terminal 2 - Launch a client
mvn javafx:run
```

## Documentation

| Document | Purpose |
|----------|---------|
| [docs/table-of-contents.md](docs/table-of-contents.md) | Master index of all documentation |
| [docs/RTDAS_PRD.md](docs/RTDAS_PRD.md) | Product requirements, user stories, scope |
| [docs/DESIGN.md](docs/DESIGN.md) | UI/UX design, screen layouts, interactions |
| [docs/architecture.md](docs/architecture.md) | System architecture (Deep Modules), RMI contract |
| [docs/database.md](docs/database.md) | Database schema, constraints, backup strategy |
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