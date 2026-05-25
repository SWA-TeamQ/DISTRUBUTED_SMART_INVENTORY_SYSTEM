package com.auction.server.core;

import com.auction.shared.Constants;
import com.auction.server.repository.AuctionRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Deep module for media persistence.
 * Handles filesystem storage, JPG re-encoding, and thumbnail generation.
 */
public class ImageStore {
    private final AuctionRepository auctionRepo;

    private static final byte[] PLACEHOLDER_BYTES = createPlaceholder();

    public ImageStore(AuctionRepository auctionRepo) {
        this.auctionRepo = auctionRepo;
        ensureDirectoriesExist();
    }

    private void ensureDirectoriesExist() {
        try {
            Files.createDirectories(Paths.get(Constants.IMAGES_DIR));
            Files.createDirectories(Paths.get(Constants.THUMBS_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize image storage", e);
        }
    }

    public String[] stageImages(byte[] i1, byte[] i2, byte[] i3) {
        String baseId = java.util.UUID.randomUUID().toString();
        String p1 = saveProcessedToDisk(baseId, 1, i1, true);
        String p2 = saveProcessedToDisk(baseId, 2, i2, false);
        String p3 = saveProcessedToDisk(baseId, 3, i3, false);
        
        return new String[]{p1, p2, p3};
    }

    public void deleteStagedImages(String[] paths) {
        if (paths == null) return;
        for (String pathStr : paths) {
            if (pathStr != null) {
                try {
                    Path path = Paths.get(pathStr);
                    Files.deleteIfExists(path);
                    if (pathStr.contains("_1.jpg")) {
                        String thumbName = path.getFileName().toString().replace(".jpg", "_thumb.jpg");
                        Files.deleteIfExists(Paths.get(Constants.THUMBS_DIR, thumbName));
                    }
                } catch (IOException e) {
                    System.err.println("Failed to delete orphaned image: " + pathStr);
                }
            }
        }
    }

    public byte[] loadFullImage(String path) {
        return readBytes(path, true);
    }

    public byte[] loadThumbnail(String img1Path) {
        if (img1Path == null) return PLACEHOLDER_BYTES;
        try {
            String thumbName = Paths.get(img1Path).getFileName().toString().replace(".jpg", "_thumb.jpg");
            Path path = Paths.get(Constants.THUMBS_DIR, thumbName);
            return readBytes(path.toString(), false);
        } catch (Exception e) {
            return PLACEHOLDER_BYTES;
        }
    }

    private byte[] readBytes(String pathStr, boolean usePlaceholder) {
        if (pathStr == null || pathStr.isEmpty()) return usePlaceholder ? PLACEHOLDER_BYTES : new byte[0];
        try {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            System.err.println("Failed to read image: " + pathStr);
        }
        return usePlaceholder ? PLACEHOLDER_BYTES : new byte[0];
    }

    private String saveProcessedToDisk(String baseId, int index, byte[] data, boolean generateThumb) {
        if (data == null || data.length == 0) return null;
        
        String filename = baseId + "_" + index + ".jpg";
        Path path = Paths.get(Constants.IMAGES_DIR, filename);
        try {
            byte[] jpgData = reencodeToJpg(data);
            Files.write(path, jpgData);
            
            if (generateThumb) {
                byte[] thumbData = generateThumbnail(jpgData);
                Path thumbPath = Paths.get(Constants.THUMBS_DIR, baseId + "_" + index + "_thumb.jpg");
                Files.write(thumbPath, thumbData);
            }
            
            return path.toString();
        } catch (IOException e) {
            System.err.println("Failed to process/save image " + filename + ": " + e.getMessage());
            return null;
        }
    }

    private byte[] reencodeToJpg(byte[] originalData) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(originalData));
        if (img == null) throw new IOException("Invalid image data");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] generateThumbnail(byte[] jpgData) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(jpgData));
        if (original == null) throw new IOException("Invalid image data");

        int size = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - size) / 2;
        int y = (original.getHeight() - size) / 2;

        BufferedImage cropped = original.getSubimage(x, y, size, size);
        BufferedImage thumb = new BufferedImage(Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumb, "jpg", baos);
        return baos.toByteArray();
    }

    private static byte[] createPlaceholder() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("NO IMAGE", 20, 55);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try { ImageIO.write(img, "jpg", baos); } catch (IOException ignored) {}
        return baos.toByteArray();
    }
}
