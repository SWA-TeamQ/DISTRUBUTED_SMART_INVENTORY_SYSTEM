# Setup & Run

## Prerequisites
- Java 17+
- Maven or Gradle (if build tooling is added later)
- SQLite or H2 (embedded)

## Local Run (Target Steps)
1. Start the RMI registry.
2. Launch the server to bind `IInventoryService`.
3. Launch the JavaFX client.

## Configuration Targets
- RMI host/port in client config
- Database file path under `/data`
- Log file path under `/logs`

## Expected Output
- Server registers service name `InventoryService`.
- Client connects and shows login screen.