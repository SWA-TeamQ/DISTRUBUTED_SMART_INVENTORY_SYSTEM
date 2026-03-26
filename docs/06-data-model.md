# Data Model & Persistence

## Database Schema
### Table: `products`
- `id` INTEGER PRIMARY KEY AUTOINCREMENT
- `name` VARCHAR(100) NOT NULL
- `sku` VARCHAR(20) UNIQUE
- `price` DOUBLE CHECK ($price \ge 0$)
- `quantity` INTEGER CHECK ($quantity \ge 0$)

## In-Memory Structures
- `HashMap<Integer, Product>` for fast lookups
- `PriorityQueue<Product>` for restock alerts

## File Storage
- `/data/inventory_backup.db` – SQLite database file
- `/logs/system_audit.log` – append-only audit log
- `/exports/report_YYYYMMDD.csv` – exported CSV reports

## CSV Format (Target)
Each line represents a product:
```text
id,name,sku,price,quantity
```
