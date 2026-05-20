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
    /** Returns a unique session token UUID if login is successful. */
    String login(String username, String password) throws RemoteException, AuctionException;
    void logout(String token) throws RemoteException;
    String serverTime() throws RemoteException;

    // --- Auction Browsing ---
    List<AuctionItem> getActiveAuctions() throws RemoteException;
    AuctionItem getAuctionById(int auctionId) throws RemoteException;

    // --- Bidding ---
    /** clientExpectedPriceCents enables stale-data detection on the server. */
    void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token)
            throws RemoteException, AuctionException;
    List<Bid> getBidHistory(int auctionId) throws RemoteException;

    // --- Auction Management (Seller) ---
    /** Returns the new auction's ID. image bytes may be null if no image provided. */
    int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token)
            throws RemoteException, AuctionException;
    void cancelAuction(int auctionId, String token)
            throws RemoteException, AuctionException;
    void relistAuction(int auctionId, String newEndTimeIso, String token)
            throws RemoteException, AuctionException;

    // --- Bidder Activity ---
    List<Bid> getMyBids(String token) throws RemoteException, AuctionException;
    List<AuctionItem> getMyWonAuctions(String token) throws RemoteException, AuctionException;

    // --- Image Handling (LQIP) ---
    byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException;
    byte[] getFullImage(int auctionId, int imageIndex) throws RemoteException;

    // --- Data Export ---
    byte[] exportAuctionsToCSV(String token) throws RemoteException, AuctionException;

    // --- Administration ---
    void createUser(String newUsername, String password, String role, String token)
            throws RemoteException, AuctionException;
    List<User> getAllUsers(String token) throws RemoteException, AuctionException;
    byte[] backupDatabase(String token) throws RemoteException, AuctionException;
    List<String> getAuditLogs(int lastNLines, String token)
            throws RemoteException, AuctionException;
}
