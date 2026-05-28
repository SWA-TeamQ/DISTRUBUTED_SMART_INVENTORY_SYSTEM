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
    /** Registers a new user. */
    void register(String username, String password, String role) throws RemoteException, AuctionException;
        /** Returns the authenticated user's role for the current session. */
        String getMyRole(String token) throws RemoteException, AuctionException;
    void logout(String token) throws RemoteException;
    String serverTime() throws RemoteException;

    // --- Auction Browsing ---
    List<AuctionItem> getActiveAuctions() throws RemoteException;
        List<AuctionItem> getAllAuctions() throws RemoteException;
        /**
         * Server-side active auction query.
         * @param query free-text query over title/description/category (nullable)
         * @param category exact category filter (nullable)
         * @param sortBy one of: newest, price_asc, price_desc
         */
        default List<AuctionItem> searchActiveAuctions(String query, String category, String sortBy) throws RemoteException {
                // Backward-compatible default for tests/fakes that don't override this yet.
                List<AuctionItem> base = getActiveAuctions();
                if (base == null) return java.util.List.of();

                String q = query == null ? "" : query.trim().toLowerCase();
                String c = category == null ? "" : category.trim();

                java.util.stream.Stream<AuctionItem> stream = base.stream();
                if (!q.isBlank()) {
                        stream = stream.filter(a -> containsIgnoreCase(a.getTitle(), q)
                                        || containsIgnoreCase(a.getDescription(), q)
                                        || containsIgnoreCase(a.getCategory(), q));
                }
                if (!c.isBlank()) {
                        stream = stream.filter(a -> c.equalsIgnoreCase(a.getCategory()));
                }

                java.util.List<AuctionItem> filtered = stream.toList();
                if ("price_asc".equals(sortBy)) {
                        return filtered.stream()
                                        .sorted(java.util.Comparator.comparingLong(AuctionItem::getCurrentBidCents))
                                        .toList();
                }
                if ("price_desc".equals(sortBy)) {
                        return filtered.stream()
                                        .sorted(java.util.Comparator.comparingLong(AuctionItem::getCurrentBidCents).reversed())
                                        .toList();
                }
                return filtered.stream()
                                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                                .toList();
        }

        /**
         * Backward-compatible default for implementations that do not yet override all-auction search.
         */
        default List<AuctionItem> searchAllAuctions(String query, String category, String sortBy) throws RemoteException {
                return searchActiveAuctions(query, category, sortBy);
        }

        private static boolean containsIgnoreCase(String value, String needleLower) {
                return value != null && value.toLowerCase().contains(needleLower);
        }

    List<AuctionItem> getActiveAuctionsBySeller(String sellerUsername, String token) throws RemoteException, AuctionException;
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
    default void updateAuction(int auctionId, AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token)
            throws RemoteException, AuctionException {
        throw new AuctionException("Auction editing is not supported by this implementation");
    }
    void cancelAuction(int auctionId, String token)
            throws RemoteException, AuctionException;
    void relistAuction(int auctionId, String newEndTimeIso, String token)
            throws RemoteException, AuctionException;

        /** Start a scheduled auction immediately (seller or admin only) */
        void startAuction(int auctionId, String token) throws RemoteException, AuctionException;

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
