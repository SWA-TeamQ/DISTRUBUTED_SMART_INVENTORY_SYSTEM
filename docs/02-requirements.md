# Requirements

## Functional Requirements
### Role-Based Access Control (RBAC)
- **User (abstract):** `username`, `password`, `roleType`
- **Staff:** view inventory, update stock quantities
- **Manager:** all Staff permissions + import/export CSV, modify price
- **Admin:** all Manager permissions + backup database, view system logs, manage users

### Inventory Operations
- View all products
- Update stock quantities with validation
- Sort by price or category (client-side)

### Import/Export
- Export inventory to CSV
- Import inventory from CSV with validation

### Authentication
- Login with username/password
- Return a role-specific user object on success

## Non-Functional Requirements
### Concurrency
- Server-side stock updates must be thread-safe.
- RMI requests must not corrupt inventory state.

### Performance
- Use in-memory caching for product access.
- Use efficient collections for sorting and urgent restock detection.

### Reliability
- Log critical operations to an audit file.
- Handle network errors gracefully in the client.

### Usability
- JavaFX UI must remain responsive during network calls.