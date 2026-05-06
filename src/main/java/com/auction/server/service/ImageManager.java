package com.auction.server.service;

import com.auction.shared.Constants;

/**
 * Handles image storage, retrieval, and LQIP thumbnail generation.
 * Uses java.awt.image.BufferedImage (no external dependency).
 *
 * Storage layout:
 *   resources/images/<auctionId>_<index>.jpg  (full resolution)
 *   resources/thumbs/<auctionId>_1.jpg        (40x40 blurred thumbnail, image 1 only)
 */
public class ImageManager {

    /**
     * Save a full-resolution image and generate thumbnail if index == 1.
     * @param auctionId auction this image belongs to
     * @param imageIndex 1, 2, or 3
     * @param imageData raw bytes (JPEG)
     * @return filename stored, or null if imageData is null/empty
     */
    public String saveImage(int auctionId, int imageIndex, byte[] imageData) {
        // TODO: validate size <= MAX_IMAGE_SIZE_BYTES
        // TODO: write to IMAGES_DIR/<auctionId>_<index>.jpg
        // TODO: if index == 1, generate 40x40 blurred thumb -> THUMBS_DIR
        return null;
    }

    /**
     * Load thumbnail bytes for gallery display.
     * @return thumbnail bytes, or placeholder if not found
     */
    public byte[] loadThumbnail(int auctionId, int imageIndex) {
        // TODO: read from THUMBS_DIR, return placeholder if missing
        return new byte[0];
    }

    /**
     * Load full-resolution image bytes.
     * @return image bytes, or placeholder if not found
     */
    public byte[] loadFullImage(int auctionId, int imageIndex) {
        // TODO: read from IMAGES_DIR, return placeholder if missing
        return new byte[0];
    }
}
