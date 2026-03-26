# Testing Plan

## Unit Tests
- Validate `Product` comparison and serialization.
- Validate role-based permissions.

## Integration Tests
- RMI client to server handshake.
- Update stock, then read inventory.

## Concurrency Tests
- Multiple clients updating the same product.
- Verify stock never goes below zero.

## Persistence Tests
- Insert and read products via JDBC.
- Import/export CSV correctness.

## UI Tests (Manual)
- Verify UI remains responsive during RMI calls.
- Validate alert dialogs on failure.