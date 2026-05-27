package com.auction.server.tools;

import com.auction.shared.Constants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/**
 * SeedTestImages — Generates colorful placeholder images for the demo seeded auctions.
 * Call this after running DemoSeeder to populate image directories.
 *
 * Run with: mvn exec:java -Dexec.mainClass=com.auction.server.tools.SeedTestImages
 */
public class SeedTestImages {

  private static final int THUMB_SIZE = Constants.THUMBNAIL_SIZE;
  private static final int FULL_SIZE = 400;

  private static final String[] ITEMS = {
    "Walkman|Electronics|📱|0x3498DB",
    "Atari 2600|Electronics|🕹️|0x3498DB",
    "Teak Chair|Furniture|🪑|0xE67E22",
    "Oil Painting|Art|🎨|0x9B59B6",
    "Harry Potter|Books|📖|0x2ECC71",
    "Bookshelf|Furniture|📚|0xE67E22",
  };

  public static void main(String[] args) {
    System.out.println("🖼️  Generating Test Images...");

    try {
      Files.createDirectories(Paths.get(Constants.IMAGES_DIR));
      Files.createDirectories(Paths.get(Constants.THUMBS_DIR));

      for (int idx = 0; idx < ITEMS.length; idx++) {
        String[] parts = ITEMS[idx].split("\\|");
        String title = parts[0];
        String category = parts[1];
        String emoji = parts[2];
        Color color = Color.decode(parts[3]);

        generateImages(idx + 1, title, category, emoji, color);
      }

      System.out.println("\n✅ All images generated!");
      System.out.println("📁 Images: " + Constants.IMAGES_DIR);
      System.out.println("📁 Thumbnails: " + Constants.THUMBS_DIR);
    } catch (Exception e) {
      System.err.println("❌ Failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void generateImages(
    int auctionId,
    String title,
    String category,
    String emoji,
    Color baseColor
  ) throws Exception {
    System.out.println("\n📸 " + title + " (ID: " + auctionId + ")");

    for (int img = 1; img <= 3; img++) {
      // Generate full-size
      BufferedImage fullImage = createImage(
        FULL_SIZE,
        baseColor,
        emoji,
        title + " #" + img
      );
      String fullName =
        Constants.IMAGES_DIR + "/auction_" + auctionId + "_img_" + img + ".jpg";
      ImageIO.write(fullImage, "jpg", new File(fullName));
      System.out.println("  ✓ Full: " + fullName);

      // Generate thumbnail
      BufferedImage thumbImage = createImage(
        THUMB_SIZE,
        baseColor,
        emoji,
        category
      );
      String thumbName =
        Constants.THUMBS_DIR +
        "/auction_" +
        auctionId +
        "_img_" +
        img +
        "_thumb.jpg";
      ImageIO.write(thumbImage, "jpg", new File(thumbName));
      System.out.println("  ✓ Thumb: " + thumbName);
    }
  }

  private static BufferedImage createImage(
    int size,
    Color baseColor,
    String emoji,
    String text
  ) {
    BufferedImage img = new BufferedImage(
      size,
      size,
      BufferedImage.TYPE_INT_RGB
    );
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON
    );

    // Gradient background
    GradientPaint gp = new GradientPaint(
      0,
      0,
      baseColor,
      size,
      size,
      darken(baseColor, 0.3f)
    );
    g.setPaint(gp);
    g.fillRect(0, 0, size, size);

    // Pattern overlay
    g.setColor(new Color(255, 255, 255, 30));
    for (int x = 0; x < size; x += size / 8) {
      g.drawLine(x, 0, x + size / 8, size);
    }

    // Center circle
    g.setColor(new Color(255, 255, 255, 80));
    int circleSize = size / 2;
    g.fillOval(
      (size - circleSize) / 2,
      (size - circleSize) / 2,
      circleSize,
      circleSize
    );

    // Emoji
    g.setFont(new Font("Arial", Font.BOLD, size / 3));
    g.setColor(Color.WHITE);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(emoji, (size - fm.stringWidth(emoji)) / 2, size / 3);

    // Text
    if (size > 50) {
      g.setFont(new Font("Arial", Font.PLAIN, Math.max(8, size / 16)));
      g.setColor(new Color(255, 255, 255, 200));
      fm = g.getFontMetrics();
      String displayText =
        text.length() > 12 ? text.substring(0, 10) + ".." : text;
      g.drawString(
        displayText,
        (size - fm.stringWidth(displayText)) / 2,
        (int) (size * 0.8)
      );
    }

    g.dispose();
    return img;
  }

  private static Color darken(Color c, float factor) {
    return new Color(
      Math.max(0, (int) (c.getRed() * (1 - factor))),
      Math.max(0, (int) (c.getGreen() * (1 - factor))),
      Math.max(0, (int) (c.getBlue() * (1 - factor)))
    );
  }
}
