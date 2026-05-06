package com.auction.shared.interfaces;

import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.shared.models.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IAuctionService extends Remote {
    List<AuctionItem> getActiveAuctions() throws RemoteException;
    AuctionItem getAuctionById(int auctionId) throws RemoteException;
    void placeBid(int auctionId, String bidderUsername, double amount) throws RemoteException, AuctionException;
    int createAuction(AuctionItem item) throws RemoteException;
    List<Bid> getBidHistory(int auctionId) throws RemoteException;
    byte[] exportAuctionsToCSV() throws RemoteException;
    void importAuctionsFromCSV(byte[] fileData) throws RemoteException;
    User login(String username, String password) throws RemoteException;
}
