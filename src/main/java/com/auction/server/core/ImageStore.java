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
     * Saves provided image payloads to disk and returns the paths.
     * Generates random UUIDs for names instead of depending on auction ID.
     */
    public String[] stageImages(byte[] i1, byte[] i2, byte[] i3) {
        String baseId = java.util.UUID.randomUUID().toString();
        String p1 = saveToDisk(baseId, 1, i1, true);
        String p2 = saveToDisk(baseId, 2, i2, false);
        String p3 = saveToDisk(baseId, 3, i3, false);
        
        return new String[]{p1, p2, p3};
    }

    /**
     * Deletes staged images if the database transaction fails.
     */
    public void deleteStagedImages(String[] paths) {
        if (paths == null) return;
        for (String pathStr : paths) {
            if (pathStr != null) {
                try {
                    Files.deleteIfExists(Paths.get(pathStr));
                    // Also try to delete thumb if it was the first image
                    if (pathStr.endsWith("_1.jpg")) {
                        String thumbName = Paths.get(pathStr).getFileName().toString().replace(".jpg", "_thumb.jpg");
                        Files.deleteIfExists(Paths.get(THUMBS_DIR, thumbName));
                    }
                } catch (IOException e) {
                    System.err.println("Failed to delete orphaned image: " + pathStr);
                }
            }
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
    public byte[] loadThumbnail(String img1Path) {
        if (img1Path == null) return new byte[0];
        String thumbName = Paths.get(img1Path).getFileName().toString().replace(".jpg", "_thumb.jpg");
        Path path = Paths.get(THUMBS_DIR, thumbName);
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

    private String saveToDisk(String baseId, int index, byte[] data, boolean generateThumb) {
        if (data == null || data.length == 0) return null;
        
        String filename = baseId + "_" + index + ".jpg";
        Path path = Paths.get(IMAGES_DIR, filename);
        try {
            Files.write(path, data);
            
            if (generateThumb) {
                // Simplified: just copy to thumb dir for now, or imagine a resize logic here
                Path thumbPath = Paths.get(THUMBS_DIR, baseId + "_" + index + "_thumb.jpg");
                Files.copy(path, thumbPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            return path.toString();
        } catch (IOException e) {
            System.err.println("Failed to save image " + filename + ": " + e.getMessage());
            return null;
        }
    }
}
