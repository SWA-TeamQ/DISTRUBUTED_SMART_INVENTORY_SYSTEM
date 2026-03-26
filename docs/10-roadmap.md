# Roadmap

## Sprint 1: Core Models & Local Persistence
- Define OOP hierarchy for users and products.
- Implement JDBC access and CSV file I/O locally.
- Basic logging to audit file.

## Sprint 2: Distributed Middleware (RMI)
- Define `IInventoryService` interface.
- Implement server binding and client stub access.
- Validate remote calls for inventory operations.

## Sprint 3: Concurrency & Reactive UI
- Enforce server-side locking for stock updates.
- Move RMI calls into JavaFX background tasks.
- Implement responsive UI updates and background clock.