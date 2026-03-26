# Architecture

## Layered Design
1. **Presentation Layer (JavaFX)**
   - FXML views and controllers
   - Handles user input
   - Calls RMI service via client stub

2. **Service Layer (RMI Server)**
   - Implements remote interface
   - Validates requests
   - Enforces thread safety

3. **Data Access Layer (DAL)**
   - JDBC handler for relational data
   - File handlers for CSV and logs

## Packages and Responsibilities
- `com.inventory.client` – client bootstrap and app lifecycle
- `com.inventory.client.controllers` – UI controllers
- `com.inventory.client.network` – RMI client connectivity
- `com.inventory.server` – server bootstrap
- `com.inventory.server.service` – RMI service implementation
- `com.inventory.server.repository` – database and file persistence
- `com.inventory.shared` – shared constants, models, interfaces, exceptions

## Key Components
- **RMI Registry:** exposes the `IInventoryService` stub
- **Inventory Cache:** `HashMap<Integer, Product>` for fast access
- **Urgent Queue:** `PriorityQueue<Product>` for restock alerts