package com.auction.server.core;

import com.auction.server.repository.AuctionRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Deep module for media persistence.
 * Handles filesystem storage and database path synchronization.
 * Provides a high-leverage interface that hides byte-to-file orchestration.
 */
public class ImageStore {
    private static final String IMAGES_DIR = "data/images";
    private static final String THUMBS_DIR = "data/thumbs";
    
    private final AuctionRepository auctionRepo;

    public ImageStore(AuctionRepository auctionRepo) {
        this.auctionRepo = auctionRepo;
        ensureDirectoriesExist();
    }

    private void ensureDirectoriesExist() {
        try {
            Files.createDirectories(Paths.get(IMAGES_DIR));
            Files.createDirectories(Paths.get(THUMBS_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize image storage", e);
        }
    }

    /**
     * Saves provided image payloads for an auction and updates the database record.
     * Hides the complexity of naming, directory management, and persistence synchronization.
     */
    public void saveAuctionImages(int auctionId, byte[] i1, byte[] i2, byte[] i3) {
        String p1 = saveToDisk(auctionId, 1, i1, true);
        String p2 = saveToDisk(auctionId, 2, i2, false);
        String p3 = saveToDisk(auctionId, 3, i3, false);
        
        if (p1 != null || p2 != null || p3 != null) {
            auctionRepo.updateAuctionImages(auctionId, p1, p2, p3);
        }
    }

    /**
     * Loads full image bytes from disk.
     */
    public byte[] loadFullImage(String path) {
        return readBytes(path);
    }

    /**
     * Loads thumbnail bytes. If path points to full image, it should ideally 
     * point to the generated thumb in THUMBS_DIR.
     */
    public byte[] loadThumbnail(int auctionId) {
        Path path = Paths.get(THUMBS_DIR, auctionId + "_1_thumb.jpg");
        return readBytes(path.toString());
    }

    private byte[] readBytes(String pathStr) {
        if (pathStr == null || pathStr.isEmpty()) return new byte[0];
        try {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            System.err.println("Failed to read image: " + pathStr);
        }
        return new byte[0];
    }

    private String saveToDisk(int auctionId, int index, byte[] data, boolean generateThumb) {
        if (data == null || data.length == 0) return null;
        
        String filename = auctionId + "_" + index + ".jpg";
        Path path = Paths.get(IMAGES_DIR, filename);
        try {
            Files.write(path, data);
            
            if (generateThumb) {
                // Simplified: just copy to thumb dir for now, or imagine a resize logic here
                Path thumbPath = Paths.get(THUMBS_DIR, auctionId + "_" + index + "_thumb.jpg");
                Files.copy(path, thumbPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            return path.toString();
        } catch (IOException e) {
            System.err.println("Failed to save image " + filename + ": " + e.getMessage());
            return null;
        }
    }
}
