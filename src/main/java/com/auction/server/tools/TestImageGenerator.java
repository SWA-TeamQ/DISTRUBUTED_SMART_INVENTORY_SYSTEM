package com.auction.server.tools;

import com.auction.shared.Constants;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * TestImageGenerator — Creates colorful placeholder images for seeded auctions.
 * Generates full-size images and thumbnails for visual testing.
 *
 * Run AFTER DemoSeeder to populate image directories.
 * Run with: mvn exec:java -Dexec.mainClass=com.auction.server.tools.TestImageGenerator
 */
public class TestImageGenerator {

  private static final int FULL_SIZE = 400;
  private static final int THUMB_SIZE = 100;
  private static final Random RANDOM = new Random(42); // Deterministic for reproducibility

  private static final String[] CATEGORIES = {
    "Electronics",
    "Furniture",
    "Art",
    "Collectibles",
    "Books",
    "Vintage",
  };

  private static final Color[] CATEGORY_COLORS = {
    new Color(0x3498DB), // Electronics - Blue
    new Color(0xE67E22), // Furniture - Orange
    new Color(0x9B59B6), // Art - Purple
    new Color(0xF39C12), // Collectibles - Gold
    new Color(0x2ECC71), // Books - Green
    new Color(0xE74C3C), // Vintage - Red
  };

  public static void main(String[] args) {
    System.out.println("🖼️  Test Image Generator Starting...");

    try {
      ensureDirectoriesExist();

      // Generate 6 unique placeholder images for the 6 seeded auctions
      generateAuctionImages(
        "Walkman",
        "Vintage Sony Walkman - Classic 1980s portable cassette player",
        0 // Electronics - Blue
      );

      generateAuctionImages(
        "Atari 2600",
        "Original Atari 2600 - Vintage gaming console with 10 games",
        0 // Electronics - Blue
      );

      generateAuctionImages(
        "Teak Chair",
        "Mid-Century Modern Teak Chair - Authentic 1960s Danish design",
        1 // Furniture - Orange
      );

      generateAuctionImages(
        "Oil Painting",
        "Abstract Oil Painting - Modern art piece 24x36 inches",
        2 // Art - Purple
      );

      generateAuctionImages(
        "Harry Potter",
        "Rare First Edition Harry Potter - Philosopher's Stone pristine",
        4 // Books - Green
      );

      generateAuctionImages(
        "Bookshelf",
        "Ikea Billy Bookshelf - 5-shelf walnut finish custom build",
        1 // Furniture - Orange
      );

      System.out.println("✅ Image generation complete!");
      System.out.println("\n📁 Generated:");
      System.out.println(
        "  - " + Constants.IMAGES_DIR + " (full-size: 400x400px)"
      );
      System.out.println(
        "  - " + Constants.THUMBS_DIR + " (thumbnails: 100x100px)"
      );
      System.out.println("\n🎯 Next steps:");
      System.out.println("  1. Run server: mvn exec:java");
      System.out.println("  2. Login as bella-247 (pass: pass123)");
      System.out.println("  3. View gallery → thumbnails should display");
      System.out.println("  4. Click auction → full images should load");
    } catch (Exception e) {
      System.err.println("❌ Image generation failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void ensureDirectoriesExist() throws IOException {
    Files.createDirectories(Paths.get(Constants.IMAGES_DIR));
    Files.createDirectories(Paths.get(Constants.THUMBS_DIR));
  }

  private static void generateAuctionImages(
    String title,
    String description,
    int categoryIndex
  ) throws IOException {
    Color categoryColor = CATEGORY_COLORS[categoryIndex %
    CATEGORY_COLORS.length];
    String category = CATEGORIES[categoryIndex % CATEGORIES.length];

    System.out.println("\n📸 " + title);

    // Create 3 images for this auction
    for (int i = 1; i <= 3; i++) {
      String fullFileName = title.replaceAll("\\s+", "_") + "_" + i + ".jpg";
      String thumbFileName =
        title.replaceAll("\\s+", "_") + "_" + i + "_thumb.jpg";

      // Full-size image
      BufferedImage fullImage = createPlaceholderImage(
        FULL_SIZE,
        FULL_SIZE,
        categoryColor,
        category + " - Image " + i,
        title
      );
      String fullPath = Constants.IMAGES_DIR + "/" + fullFileName;
      ImageIO.write(fullImage, "jpg", new File(fullPath));
      System.out.println("  ✓ Full image " + i + ": " + fullPath);

      // Thumbnail
      BufferedImage thumbImage = createPlaceholderImage(
        THUMB_SIZE,
        THUMB_SIZE,
        categoryColor,
        category,
        title
      );
      String thumbPath = Constants.THUMBS_DIR + "/" + thumbFileName;
      ImageIO.write(thumbImage, "jpg", new File(thumbPath));
      System.out.println("  ✓ Thumbnail " + i + ": " + thumbPath);
    }
  }

  private static BufferedImage createPlaceholderImage(
    int width,
    int height,
    Color baseColor,
    String title,
    String subtitle
  ) {
    BufferedImage img = new BufferedImage(
      width,
      height,
      BufferedImage.TYPE_INT_RGB
    );
    Graphics2D g = img.createGraphics();

    // Enable anti-aliasing
    g.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON
    );
    g.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    );

    // Background gradient
    GradientPaint gradient = new GradientPaint(
      0,
      0,
      baseColor,
      width,
      height,
      darken(baseColor, 0.3f)
    );
    g.setPaint(gradient);
    g.fillRect(0, 0, width, height);

    // Decorative pattern overlay
    g.setColor(new Color(255, 255, 255, 30));
    int step = width / 8;
    for (int x = 0; x < width; x += step) {
      g.drawLine(x, 0, x + step, height);
    }

    // Center circle accent
    g.setColor(new Color(255, 255, 255, 80));
    int circleSize = (int) (width * 0.4);
    g.fillOval(
      (width - circleSize) / 2,
      (height - circleSize) / 2,
      circleSize,
      circleSize
    );

    // Add emoji/icon based on category
    String emoji = getEmojiForCategory(title);
    g.setFont(new Font("Arial", Font.BOLD, width / 4));
    g.setColor(Color.WHITE);
    FontMetrics fm = g.getFontMetrics();
    int emojiX = (width - fm.stringWidth(emoji)) / 2;
    int emojiY = (height / 3) + fm.getAscent();
    g.drawString(emoji, emojiX, emojiY);

    // Title text
    g.setFont(new Font("Arial", Font.BOLD, Math.max(12, width / 16)));
    g.setColor(Color.WHITE);
    fm = g.getFontMetrics();
    drawCenteredText(g, title, width / 2, (int) (height * 0.65), fm);

    // Subtitle text (smaller)
    g.setFont(new Font("Arial", Font.PLAIN, Math.max(10, width / 20)));
    g.setColor(new Color(255, 255, 255, 200));
    fm = g.getFontMetrics();
    drawCenteredText(g, subtitle, width / 2, (int) (height * 0.82), fm);

    g.dispose();
    return img;
  }

  private static void drawCenteredText(
    Graphics2D g,
    String text,
    int x,
    int y,
    FontMetrics fm
  ) {
    String[] lines = text.split("\\s+", 3);
    String truncated = String.join(" ", lines);
    if (truncated.length() > 20) {
      truncated = truncated.substring(0, 17) + "...";
    }
    int textX = x - fm.stringWidth(truncated) / 2;
    g.drawString(truncated, textX, y);
  }

  private static String getEmojiForCategory(String title) {
    if (
      title.toLowerCase().contains("walkman") ||
      title.toLowerCase().contains("atari") ||
      title.toLowerCase().contains("electronics")
    ) {
      return "📱";
    } else if (
      title.toLowerCase().contains("chair") ||
      title.toLowerCase().contains("furniture") ||
      title.toLowerCase().contains("bookshelf")
    ) {
      return "🪑";
    } else if (
      title.toLowerCase().contains("painting") ||
      title.toLowerCase().contains("art")
    ) {
      return "🎨";
    } else if (
      title.toLowerCase().contains("harry") ||
      title.toLowerCase().contains("book")
    ) {
      return "📖";
    }
    return "📦";
  }

  private static Color darken(Color c, float factor) {
    return new Color(
      Math.max(0, (int) (c.getRed() * (1 - factor))),
      Math.max(0, (int) (c.getGreen() * (1 - factor))),
      Math.max(0, (int) (c.getBlue() * (1 - factor)))
    );
  }
}
