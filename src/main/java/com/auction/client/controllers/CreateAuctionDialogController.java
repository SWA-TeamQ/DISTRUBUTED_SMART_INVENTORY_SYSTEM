package com.auction.client.controllers;

import com.auction.shared.Constants;
import com.auction.shared.models.AuctionItem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class CreateAuctionDialogController {

  private static final Pattern DECIMAL_PATTERN = Pattern.compile(
    "\\d*(?:\\.\\d{0,2})?"
  );
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d*");

  @FXML
  private TextField titleField;

  @FXML
  private TextArea descriptionField;

  @FXML
  private TextField categoryField;

  @FXML
  private TextField startingPriceField;

  @FXML
  private TextField durationField;

  @FXML
  private Label imageSummaryLabel;

  @FXML
  private Label validationLabel;

  private byte[] image1Bytes;
  private byte[] image2Bytes;
  private byte[] image3Bytes;
  private String image1Name;
  private String image2Name;
  private String image3Name;

  @FXML
  public void initialize() {
    startingPriceField.setTextFormatter(
      new TextFormatter<>(change -> {
        String proposed = change.getControlNewText();
        return DECIMAL_PATTERN.matcher(proposed).matches() ? change : null;
      })
    );

    durationField.setTextFormatter(
      new TextFormatter<>(change -> {
        String proposed = change.getControlNewText();
        return INTEGER_PATTERN.matcher(proposed).matches() ? change : null;
      })
    );

    updateImageSummary();
    validationLabel.setText("Provide the auction details and optional images.");
  }

  @FXML
  private void handlePickImage1() {
    chooseImage(1);
  }

  @FXML
  private void handlePickImage2() {
    chooseImage(2);
  }

  @FXML
  private void handlePickImage3() {
    chooseImage(3);
  }

  public boolean validateFields() {
    String title = safeTrim(titleField.getText());
    String description = safeTrim(descriptionField.getText());
    String category = safeTrim(categoryField.getText());
    String priceText = safeTrim(startingPriceField.getText());
    String durationText = safeTrim(durationField.getText());

    if (title.isEmpty()) {
      return fail("Title is required.");
    }
    if (description.isEmpty()) {
      return fail("Description is required.");
    }
    if (category.isEmpty()) {
      return fail("Category is required.");
    }
    if (priceText.isEmpty()) {
      return fail("Starting price is required.");
    }
    if (durationText.isEmpty()) {
      return fail("Duration is required.");
    }

    try {
      double price = Double.parseDouble(priceText);
      int duration = Integer.parseInt(durationText);
      if (price <= 0) {
        return fail("Starting price must be greater than zero.");
      }
      if (duration <= 0) {
        return fail("Duration must be greater than zero.");
      }
    } catch (NumberFormatException ex) {
      return fail("Price and duration must be numeric.");
    }

    validationLabel.setText("Ready to create the auction.");
    return true;
  }

  public AuctionItem buildAuctionDraft(String sellerUsername) {
    long startingPriceCents = Math.round(
      Double.parseDouble(safeTrim(startingPriceField.getText())) * 100.0
    );
    int durationMinutes = Integer.parseInt(safeTrim(durationField.getText()));
    Instant startTime = Instant.now();
    Instant endTime = startTime.plus(Duration.ofMinutes(durationMinutes));

    AuctionItem item = new AuctionItem();
    item.setTitle(safeTrim(titleField.getText()));
    item.setDescription(safeTrim(descriptionField.getText()));
    item.setCategory(safeTrim(categoryField.getText()));
    item.setStartingPriceCents(startingPriceCents);
    item.setCurrentBidCents(startingPriceCents);
    item.setHighestBidderUsername(null);
    item.setSellerUsername(sellerUsername);
    item.setStartTime(startTime.toString());
    item.setEndTime(endTime.toString());
    item.setCapEndTime(
      endTime
        .plus(Duration.ofMinutes(Constants.SNIPE_CAP_DEFAULT_MINUTES))
        .toString()
    );
    item.setStatus(Constants.STATUS_ACTIVE);
    return item;
  }

  public byte[] getImage1Bytes() {
    return image1Bytes;
  }

  public byte[] getImage2Bytes() {
    return image2Bytes;
  }

  public byte[] getImage3Bytes() {
    return image3Bytes;
  }

  private void chooseImage(int slot) {
    FileChooser chooser = new FileChooser();
    chooser
      .getExtensionFilters()
      .addAll(
        new FileChooser.ExtensionFilter(
          "Image Files",
          "*.png",
          "*.jpg",
          "*.jpeg",
          "*.gif",
          "*.webp"
        )
      );
    Window window =
      titleField.getScene() == null ? null : titleField.getScene().getWindow();
    File selected = chooser.showOpenDialog(window);
    if (selected == null) {
      return;
    }

    try {
      byte[] bytes = Files.readAllBytes(selected.toPath());
      switch (slot) {
        case 1 -> {
          image1Bytes = bytes;
          image1Name = selected.getName();
        }
        case 2 -> {
          image2Bytes = bytes;
          image2Name = selected.getName();
        }
        case 3 -> {
          image3Bytes = bytes;
          image3Name = selected.getName();
        }
        default -> {
          return;
        }
      }
      updateImageSummary();
    } catch (IOException ex) {
      validationLabel.setText("Failed to load image: " + ex.getMessage());
    }
  }

  private boolean fail(String message) {
    validationLabel.setText(message);
    return false;
  }

  private void updateImageSummary() {
    StringBuilder summary = new StringBuilder();
    appendImage(summary, 1, image1Name);
    appendImage(summary, 2, image2Name);
    appendImage(summary, 3, image3Name);
    imageSummaryLabel.setText(
      summary.length() == 0 ? "No images selected" : summary.toString()
    );
  }

  private void appendImage(StringBuilder summary, int slot, String fileName) {
    if (fileName == null) {
      return;
    }
    if (summary.length() > 0) {
      summary.append(" | ");
    }
    summary.append("Image ").append(slot).append(": ").append(fileName);
  }

  private String safeTrim(String value) {
    return value == null ? "" : value.trim();
  }
}
