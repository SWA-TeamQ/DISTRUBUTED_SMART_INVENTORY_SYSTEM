# Logging & Errors

## Audit Logging
- Append-only file: `/logs/system_audit.log`
- Example entry format:
  - `YYYY-MM-DD: User 'Staff1' sold 5 units of 'Monitor'`

## Error Handling
- **RemoteException**
  - Indicates server unreachable or network failure.
  - Client shows JavaFX alert dialog.

- **StockException**
  - Thrown when an update would result in negative quantity.

## Logging Targets
- Login attempts (success/failure)
- Stock updates
- CSV imports/exports
- Admin operations (backup, user management)