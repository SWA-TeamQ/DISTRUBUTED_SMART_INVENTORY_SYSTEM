# Distributed Smart Inventory Management System (DSIMS) Documentation

## Overview
DSIMS is a distributed, multi-user inventory system that manages commercial stock using a client-server architecture. The solution showcases advanced Java topics: OOP, collections, multithreading, file I/O, JDBC, and Java RMI. The system includes a headless server for business logic and persistence, and a JavaFX desktop client for role-based interaction.

## Architecture Summary
### Layers
- **Presentation (JavaFX):** FXML views and controllers for user interactions.
- **Service (RMI Server):** Remote interface implementation, request handling, and synchronization.
- **Data Access (DAL):** JDBC for structured data and Java I/O for file streams (CSV, logs, binaries).

### Technology Stack
- **GUI:** JavaFX 17+ (`javafx.controls`, `javafx.fxml`)
- **Networking:** Java RMI (`java.rmi.*`, `java.rmi.server.*`)
- **Persistence:** SQLite or H2 (`java.sql.*`)
- **System I/O:** `java.io.*`, `java.nio.file.*`
- **Concurrency:** `java.util.concurrent.locks`

## Functional Requirements
### Role-Based Access Control (RBAC)
- **User (Abstract):** `username`, `password`, `roleType`
- **Staff:** view inventory, `updateStockQuantity()`
- **Manager:** all Staff permissions + `importCSV()`, `exportCSV()`, `modifyPrice()`
- **Admin:** all Manager permissions + `backupDatabase()`, `viewSystemLogs()`, `manageUsers()`

### Inventory & Collections
- **Product Model:** `Serializable`, `Comparable<Product>`
- **Server Cache:** `HashMap<Integer, Product>`
- **Urgent Restock:** `PriorityQueue` for low-stock alerts
- **Client Sorting:** Comparators for price and category

### Hybrid Persistence
- **Database:** `inventory` table with fields `ID`, `Name`, `SKU`, `Price`, `Qty`, `CategoryId`
- **CSV:** Buffered read/parse into `Product` objects
- **Logging:** Append-only audit log entries

## Distributed Interface (RMI)
Core contract between client and server:
- `List<Product> getAllProducts()`
- `void updateStock(int productId, int delta)`
- `byte[] exportInventoryToCSV()`
- `void importInventoryFromCSV(byte[] fileData)`
- `User login(String username, String password)`

## Non-Functional Requirements
### Server-Side Concurrency
- **Thread Safety:** `updateStock` guarded with `synchronized` or `ReentrantLock`
- **Scalability:** Stateless or thread-safe service design

### Client-Side Concurrency
- **Non-Blocking UI:** All RMI calls in `javafx.concurrent.Task`
- **Background Clock:** Thread updates UI via `Platform.runLater()`

## Data Dictionary
### `products` Table
- `id`: INTEGER, PK, AUTOINCREMENT  
- `name`: VARCHAR(100), NOT NULL  
- `sku`: VARCHAR(20), UNIQUE  
- `price`: DOUBLE, CHECK ($price \ge 0$)  
- `quantity`: INTEGER, CHECK ($quantity \ge 0$)

### File Layout
- `/data/inventory_backup.db`
- `/logs/system_audit.log`
- `/exports/report_YYYYMMDD.csv`

## Error Handling
- **RemoteException:** Display JavaFX alert on connectivity failure
- **StockException:** Thrown if `updateStock` results in negative quantity