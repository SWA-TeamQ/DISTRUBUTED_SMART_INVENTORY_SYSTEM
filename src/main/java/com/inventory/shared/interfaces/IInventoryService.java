package com.inventory.shared.interfaces;

import com.inventory.shared.exceptions.StockException;
import com.inventory.shared.models.Product;
import com.inventory.shared.models.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IInventoryService extends Remote {
    List<Product> getAllProducts() throws RemoteException;
    void updateStock(int productId, int delta) throws RemoteException, StockException;
    byte[] exportInventoryToCSV() throws RemoteException;
    void importInventoryFromCSV(byte[] fileData) throws RemoteException;
    User login(String username, String password) throws RemoteException;
}
