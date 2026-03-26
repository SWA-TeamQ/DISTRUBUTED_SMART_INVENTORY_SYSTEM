# RMI API

## Service Interface
`IInventoryService`

### Methods
- `List<Product> getAllProducts()`
  - Returns full product list.

- `void updateStock(int productId, int delta)`
  - Adjusts stock by `delta`.
  - Throws `StockException` if the result would be negative.

- `byte[] exportInventoryToCSV()`
  - Returns CSV bytes of current inventory.

- `void importInventoryFromCSV(byte[] fileData)`
  - Loads products from CSV bytes.

- `User login(String username, String password)`
  - Authenticates user and returns role-specific user object.

## Error Semantics
- `RemoteException` signals connectivity issues.
- `StockException` signals invalid inventory update.