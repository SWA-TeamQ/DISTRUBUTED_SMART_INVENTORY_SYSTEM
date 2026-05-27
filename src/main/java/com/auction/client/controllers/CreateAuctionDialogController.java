package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.models.AuctionItem;
import java.time.Duration;
import java.time.Instant;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateAuctionDialogController {

  @FXML
  private TextField titleField;

  @FXML
  private TextField categoryField;

  @FXML
  private TextField priceField;

  @FXML
  private TextField durationField;

  @FXML
  private TextArea descArea;

  @FXML
  private Label errorLabel;

  private boolean created = false;
  private Stage dialogStage;

  public void setDialogStage(Stage dialogStage) {
    this.dialogStage = dialogStage;
  }

  public boolean isCreated() {
    return created;
  }

  @FXML
  private void handleCancel() {
    dialogStage.close();
  }

  @FXML
  private void handleCreate() {
    try {
      if (
        titleField.getText().trim().isEmpty() ||
        categoryField.getText().trim().isEmpty() ||
        priceField.getText().trim().isEmpty() ||
        durationField.getText().trim().isEmpty() ||
        descArea.getText().trim().isEmpty()
      ) {
        errorLabel.setText("All fields are required.");
        return;
      }

      double price = Double.parseDouble(priceField.getText().trim());
      if (price < 0) {
        errorLabel.setText("Price cannot be negative.");
        return;
      }
      long cents = (long) (price * 100);

      int minutes = Integer.parseInt(durationField.getText().trim());
      if (minutes <= 0) {
        errorLabel.setText("Duration must be at least 1 minute.");
        return;
      }
      Instant end = Instant.now().plus(Duration.ofMinutes(minutes));

      AuctionItem item = new AuctionItem(
        0,
        titleField.getText().trim(),
        descArea.getText().trim(),
        categoryField.getText().trim(),
        cents,
        ClientContext.getInstance().getUsername(),
        Instant.now().toString(),
        end.toString(),
        null
      );

      ClientContext ctx = ClientContext.getInstance();
            int id = ctx.getRmiProvider().getService().createAuction(item, null, null, null, ctx.getSessionToken());
      System.out.println("Auction created successfully with ID: " + id);
      created = true;
      dialogStage.close();
    } catch (NumberFormatException e) {
      errorLabel.setText("Invalid number format. Check price/duration.");
    } catch (Exception e) {
      errorLabel.setText("Error creating auction: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
