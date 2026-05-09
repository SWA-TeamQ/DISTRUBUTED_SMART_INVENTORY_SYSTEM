package com.auction.shared.interfaces;

import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.shared.models.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI remote interface — the sole contract between client and server.
 * Every method throws RemoteException per RMI spec.
 */
public interface IAuctionService extends Remote {

    // --- Authentication ---
    User login(String username, String password) throws RemoteException;

    // --- Auction Browsing ---
    List<AuctionItem> getActiveAuctions() throws RemoteException;
    AuctionItem getAuctionById(int auctionId) throws RemoteException;

    // --- Bidding ---
    /** clientExpectedPrice enables stale-data detection on the server. */
    void placeBid(int auctionId, String bidderUsername, double amount,
                  double clientExpectedPrice) throws RemoteException, AuctionException;
    List<Bid> getBidHistory(int auctionId) throws RemoteException;

    // --- Auction Management (Seller) ---
    /** Returns the new auction's ID. image bytes may be null if no image provided. */
    int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3)
            throws RemoteException;
    void cancelAuction(int auctionId, String sellerUsername)
            throws RemoteException, AuctionException;

    // --- Image Handling (LQIP) ---
    byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException;
    byte[] getFullImage(int auctionId, int imageIndex) throws RemoteException;

    // --- Data Export ---
    byte[] exportAuctionsToCSV(String sellerUsername) throws RemoteException;

    // --- Administration ---
    void createUser(String adminUsername, String newUsername, String password, String role)
            throws RemoteException, AuctionException;
    List<User> getAllUsers(String adminUsername) throws RemoteException, AuctionException;
    byte[] backupDatabase(String adminUsername) throws RemoteException, AuctionException;
    List<String> getAuditLogs(String adminUsername, int lastNLines)
            throws RemoteException, AuctionException;
}
