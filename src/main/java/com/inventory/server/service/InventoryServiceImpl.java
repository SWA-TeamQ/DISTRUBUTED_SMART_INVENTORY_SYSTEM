package com.inventory.server.service;

import com.inventory.shared.exceptions.StockException;
import com.inventory.shared.interfaces.IInventoryService;
import com.inventory.shared.models.Product;
import com.inventory.shared.models.User;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

public class InventoryServiceImpl implements IInventoryService {
    @Override
    public List<Product> getAllProducts() throws RemoteException {
        return Collections.emptyList();
    }

    @Override
    public void updateStock(int productId, int delta) throws RemoteException, StockException {
        // TODO: implement stock update with concurrency control
    }

    @Override
    public byte[] exportInventoryToCSV() throws RemoteException {
        return new byte[0];
    }

    @Override
    public void importInventoryFromCSV(byte[] fileData) throws RemoteException {
        // TODO: implement import
    }

    @Override
    public User login(String username, String password) throws RemoteException {
        return null;
    }
}
