package com.auction.client.service;

import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.Bid;
import com.auction.shared.models.User;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Lightweight in-memory auction service for UI integration before the real RMI path is ready.
 */
public class MockAuctionService implements IAuctionService {

    private final Map<Integer, AuctionItem> auctions = new ConcurrentHashMap<>();
    private final Map<Integer, List<Bid>> bidsByAuction = new ConcurrentHashMap<>();
    private final AtomicInteger bidIdSequence = new AtomicInteger(1);
    private final AtomicInteger auctionIdSequence = new AtomicInteger(100);

    public MockAuctionService() {
        seed();
    }

    private void seed() {
        if (!auctions.isEmpty()) {
            return;
        }

        AuctionItem camera = new AuctionItem(1, "Vintage Camera", "A working mock auction item.", "Collectibles", 2500,
                "seller1", Instant.now().toString(), Instant.now().plusSeconds(3600).toString(), Instant.now().plusSeconds(4200).toString());
        AuctionItem laptop = new AuctionItem(2, "Developer Laptop", "Mock hardware for bidding flow.", "Electronics", 75000,
                "seller2", Instant.now().toString(), Instant.now().plusSeconds(5400).toString(), Instant.now().plusSeconds(6000).toString());
        AuctionItem art = new AuctionItem(3, "Abstract Art Print", "Used for gallery/polling tests.", "Art", 5000,
                "seller3", Instant.now().toString(), Instant.now().plusSeconds(2700).toString(), Instant.now().plusSeconds(3300).toString());

        auctions.put(camera.getId(), camera);
        auctions.put(laptop.getId(), laptop);
        auctions.put(art.getId(), art);

        List<Bid> cameraBids = new ArrayList<>();
        cameraBids.add(new Bid(bidIdSequence.getAndIncrement(), camera.getId(), "demo-bidder", 3000, Instant.now().toString()));
        bidsByAuction.put(camera.getId(), cameraBids);
        bidsByAuction.put(laptop.getId(), new ArrayList<>());
        bidsByAuction.put(art.getId(), new ArrayList<>());
        camera.setCurrentBidCents(3000);
        camera.setHighestBidderUsername("demo-bidder");
    }

    @Override
    public String login(String username, String password) throws RemoteException, AuctionException {
        throw new AuctionException("Mock auction service does not handle login.");
    }

    @Override
    public void logout(String token) throws RemoteException {
        // no-op
    }

    @Override
    public String serverTime() throws RemoteException {
        return Instant.now().toString();
    }

    @Override
    public List<AuctionItem> getActiveAuctions() throws RemoteException {
        return auctions.values().stream()
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public AuctionItem getAuctionById(int auctionId) throws RemoteException {
        return auctions.get(auctionId);
    }

    @Override
    public void placeBid(int auctionId, long amountCents, long clientExpectedPriceCents, String token)
            throws RemoteException, AuctionException {
        AuctionItem item = auctions.get(auctionId);
        if (item == null) {
            throw new AuctionException("Auction not found");
        }
        if (!"ACTIVE".equals(item.getStatus())) {
            throw new AuctionException("Auction is not active");
        }
        if (amountCents <= Math.max(item.getCurrentBidCents(), clientExpectedPriceCents)) {
            throw new AuctionException("Bid must exceed the current price");
        }
        item.setCurrentBidCents(amountCents);
        item.setHighestBidderUsername(token == null || token.isBlank() ? "mock-bidder" : token);
        bidsByAuction.computeIfAbsent(auctionId, ignored -> new ArrayList<>())
                .add(new Bid(bidIdSequence.getAndIncrement(), auctionId, item.getHighestBidderUsername(), amountCents, Instant.now().toString()));
    }

    @Override
    public List<Bid> getBidHistory(int auctionId) throws RemoteException {
        return new ArrayList<>(bidsByAuction.getOrDefault(auctionId, Collections.emptyList()));
    }

    @Override
    public int createAuction(AuctionItem item, byte[] image1, byte[] image2, byte[] image3, String token)
            throws RemoteException, AuctionException {
        int id = auctionIdSequence.getAndIncrement();
        item.setId(id);
        auctions.put(id, item);
        bidsByAuction.put(id, new ArrayList<>());
        return id;
    }

    @Override
    public void cancelAuction(int auctionId, String token) throws RemoteException, AuctionException {
        AuctionItem item = auctions.get(auctionId);
        if (item == null) {
            throw new AuctionException("Auction not found");
        }
        item.setStatus("CANCELLED");
    }

    @Override
    public void relistAuction(int auctionId, String newEndTimeIso, String token) throws RemoteException, AuctionException {
        AuctionItem item = auctions.get(auctionId);
        if (item == null) {
            throw new AuctionException("Auction not found");
        }
        item.setStatus("ACTIVE");
        item.setEndTime(newEndTimeIso);
    }

    @Override
    public List<Bid> getMyBids(String token) throws RemoteException, AuctionException {
        if (token == null || token.isBlank()) {
            return List.of();
        }
        List<Bid> result = new ArrayList<>();
        for (List<Bid> bidList : bidsByAuction.values()) {
            for (Bid bid : bidList) {
                if (token.equals(bid.getBidderUsername())) {
                    result.add(bid);
                }
            }
        }
        return result;
    }

    @Override
    public List<AuctionItem> getMyWonAuctions(String token) throws RemoteException, AuctionException {
        if (token == null || token.isBlank()) {
            return List.of();
        }
        return auctions.values().stream()
                .filter(item -> token.equals(item.getHighestBidderUsername()))
                .collect(Collectors.toList());
    }

    @Override
    public byte[] getThumbnail(int auctionId, int imageIndex) throws RemoteException {
        return null;
    }

    @Override
    public byte[] getFullImage(int auctionId, int imageIndex) throws RemoteException {
        return null;
    }

    @Override
    public byte[] exportAuctionsToCSV(String token) throws RemoteException, AuctionException {
        StringBuilder builder = new StringBuilder("id,title,status,currentBidCents\n");
        for (AuctionItem item : auctions.values()) {
            builder.append(item.getId()).append(',')
                    .append(item.getTitle()).append(',')
                    .append(item.getStatus()).append(',')
                    .append(item.getCurrentBidCents()).append('\n');
        }
        return builder.toString().getBytes();
    }

    @Override
    public void createUser(String newUsername, String password, String role, String token)
            throws RemoteException, AuctionException {
        throw new AuctionException("Mock auction service does not support admin users yet.");
    }

    @Override
    public List<User> getAllUsers(String token) throws RemoteException, AuctionException {
        return List.of();
    }

    @Override
    public byte[] backupDatabase(String token) throws RemoteException, AuctionException {
        return new byte[0];
    }

    @Override
    public List<String> getAuditLogs(int lastNLines, String token) throws RemoteException, AuctionException {
        return List.of("Mock audit log: ready for integration testing.");
    }
}