package com.auction.server.core;

import com.auction.shared.Constants;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    public ImageStore() {
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
        String p1 = saveToDisk(baseId, 1, i1);
        String p2 = saveToDisk(baseId, 2, i2);
        String p3 = saveToDisk(baseId, 3, i3);
        
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
                    if (pathStr.contains("_1.")) {
                        Files.deleteIfExists(getThumbPath(pathStr));
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
        Path thumbPath = getThumbPath(img1Path);
        byte[] thumb = readBytes(thumbPath.toString());
        if (thumb.length > 0) {
            return thumb;
        }

        byte[] full = readBytes(img1Path);
        if (full.length == 0) {
            return new byte[0];
        }

        try {
            ImagePayload payload = normalizeImage(full);
            Files.write(thumbPath, payload.thumbBytes);
            return payload.thumbBytes;
        } catch (Exception e) {
            System.err.println("Failed to synthesize thumbnail for: " + img1Path + " - " + e.getMessage());
            return new byte[0];
        }
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

    private String saveToDisk(String baseId, int index, byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            ImagePayload payload = normalizeImage(data);

            String filename = baseId + "_" + index + ".jpg";
            Path path = Paths.get(IMAGES_DIR, filename);
            Files.write(path, payload.fullBytes);
            Path thumbPath = Paths.get(THUMBS_DIR, baseId + "_" + index + "_thumb.jpg");
            Files.write(thumbPath, payload.thumbBytes);

            return path.toString();
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to process image for storage: " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
            return null;
        }
    }

    private Path getThumbPath(String imagePath) {
        Path source = Paths.get(imagePath);
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot >= 0 ? fileName.substring(0, dot) : fileName;
        return Paths.get(THUMBS_DIR, baseName + "_thumb.jpg");
    }

    private ImagePayload normalizeImage(byte[] input) {
        if (input.length > Constants.MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image must be 2 MB or smaller.");
        }

        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(input));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read image: " + e.getMessage(), e);
        }
        if (decoded == null) {
            throw new IllegalArgumentException("Unsupported image format. Use JPG, JPEG, or PNG.");
        }

        if (decoded.getWidth() > Constants.MAX_IMAGE_WIDTH || decoded.getHeight() > Constants.MAX_IMAGE_HEIGHT) {
            throw new IllegalArgumentException(
                "Image dimensions must be at most " +
                Constants.MAX_IMAGE_WIDTH +
                "x" +
                Constants.MAX_IMAGE_HEIGHT +
                " pixels."
            );
        }

        String format = detectFormatName(input);
        if (format != null && !Constants.SUPPORTED_IMAGE_FORMATS.contains(format.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported image format. Use JPG, JPEG, or PNG.");
        }

        BufferedImage rgb = new BufferedImage(decoded.getWidth(), decoded.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(decoded, 0, 0, java.awt.Color.WHITE, null);
        } finally {
            g.dispose();
        }

        BufferedImage thumb = createThumbnail(rgb, 360);
        return new ImagePayload(writeJpeg(rgb), writeJpeg(thumb));
    }

    private BufferedImage createThumbnail(BufferedImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return source;
        }

        double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    private byte[] writeJpeg(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "jpg", out)) {
                throw new IllegalStateException("Could not encode image as JPEG");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode image as JPEG", e);
        }
    }

    private String detectFormatName(byte[] input) {
        try (javax.imageio.stream.ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            if (stream == null) {
                return null;
            }
            java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }
            javax.imageio.ImageReader reader = readers.next();
            try {
                return reader.getFormatName();
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not inspect image format: " + e.getMessage(), e);
        }
    }

    private static final class ImagePayload {
        private final byte[] fullBytes;
        private final byte[] thumbBytes;

        private ImagePayload(byte[] fullBytes, byte[] thumbBytes) {
            this.fullBytes = fullBytes;
            this.thumbBytes = thumbBytes;
        }
    }
}
