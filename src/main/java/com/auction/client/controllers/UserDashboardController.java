package com.auction.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

public class UserDashboardController {

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.AuctionItem
  > marketTable;

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.AuctionItem
  > myListingsTable;

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.Bid
  > myBidsTable;

  @FXML
  private javafx.scene.control.TableColumn<com.auction.shared.models.Bid, String> myBidsAmountColumn;

  @FXML
  private javafx.scene.control.TableView<
    com.auction.shared.models.AuctionItem
  > wonAuctionsTable;

  @FXML
  private javafx.scene.control.TextField titleField;

  @FXML
  private javafx.scene.control.TextArea descArea;

  @FXML
  private javafx.scene.control.TextField categoryField;

  @FXML
  private javafx.scene.control.TextField priceField;

  @FXML
  private javafx.scene.control.TextField endTimeField;
  @FXML
  private javafx.scene.control.DatePicker startDatePicker;
  @FXML
  private javafx.scene.control.TextField startTimeField;
  @FXML
  private javafx.scene.control.DatePicker endDatePicker;
  @FXML
  private javafx.scene.control.ChoiceBox<String> startModeChoice;

  @FXML
  private javafx.scene.control.TextField minIncrementField;
  @FXML
  private javafx.scene.control.TextField capEndMinutesField;

  @FXML
  private Button createAuctionButton;

  @FXML
  private javafx.scene.control.Label imagesLabel;

  @FXML
  private javafx.scene.control.Label statusLabel;

  @FXML
  private Label marketCountLabel;

  @FXML
  private Label listingsCountLabel;

  @FXML
  private Label bidsCountLabel;

  @FXML
  private Label winsCountLabel;

  @FXML
  private Label totalSalesLabel;

  @FXML
  private javafx.scene.control.ChoiceBox<String> listingsStatusChoice;

  @FXML
  private javafx.scene.control.TableColumn<com.auction.shared.models.AuctionItem, String> endedAtColumn;

  @FXML
  private javafx.scene.control.TableColumn<com.auction.shared.models.AuctionItem, Number> marketCurrentBidColumn;

  @FXML
  private javafx.scene.control.TableColumn<com.auction.shared.models.AuctionItem, Number> myListingsCurrentBidColumn;

  @FXML
  private javafx.scene.control.TableColumn<com.auction.shared.models.AuctionItem, Number> wonAuctionsCurrentBidColumn;

  private static final String LISTING_FILTER_ALL = "All";
  private static final String LISTING_FILTER_SCHEDULED = "Scheduled";
  private static final String LISTING_FILTER_ACTIVE = "Active";
  private static final String LISTING_FILTER_SOLD = "Sold";
  private static final String LISTING_FILTER_EXPIRED = "Expired";
  private static final String LISTING_FILTER_CANCELLED = "Cancelled";

  private java.util.List<com.auction.shared.models.AuctionItem> allMyListings = java.util.List.of();

  private Integer editingAuctionId = null;
  private Integer reschedulingAuctionId = null;

  private byte[] img1Bytes;
  private byte[] img2Bytes;
  private byte[] img3Bytes;

  @FXML
  public void initialize() {
    configureStartModeChoice();
    configureEndedAtColumn();
    configureCurrencyColumn(marketCurrentBidColumn);
    configureCurrencyColumn(myListingsCurrentBidColumn);
    configureCurrencyColumn(wonAuctionsCurrentBidColumn);
    configureListingsFilter();
    try {
      if (totalSalesLabel != null && (totalSalesLabel.getText() == null || totalSalesLabel.getText().isBlank())) {
        totalSalesLabel.setText(com.auction.shared.Constants.formatCents(0L));
      }
    } catch (Exception ignored) {}
    // Default seller min increment percent to 5 for convenience
    try { if (minIncrementField != null && (minIncrementField.getText() == null || minIncrementField.getText().isBlank())) minIncrementField.setText("5"); } catch (Exception ignored) {}
    try { if (capEndMinutesField != null && (capEndMinutesField.getText() == null || capEndMinutesField.getText().isBlank())) capEndMinutesField.setText(String.valueOf(com.auction.shared.Constants.SNIPE_CAP_DEFAULT_MINUTES)); } catch (Exception ignored) {}
    updateEditMode(false, null);
    refreshDashboard();
  }

  private void configureCurrencyColumn(javafx.scene.control.TableColumn<com.auction.shared.models.AuctionItem, Number> column) {
    try {
      if (column == null) return;
      column.setCellFactory(col -> new javafx.scene.control.TableCell<com.auction.shared.models.AuctionItem, Number>() {
        @Override
        protected void updateItem(Number item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null) {
            setText(null);
            return;
          }
          setText(com.auction.shared.Constants.formatCents(item.longValue()));
        }
      });
    } catch (Exception ignored) {}
  }

  private void configureStartModeChoice() {
    if (startModeChoice == null) return;
    if (startModeChoice.getItems().isEmpty()) {
      startModeChoice.getItems().addAll("Automatic", "Manual");
    }
    if (startModeChoice.getValue() == null) startModeChoice.setValue("Automatic");
  }

  private void configureEndedAtColumn() {
    try {
      if (endedAtColumn == null) return;
      endedAtColumn.setCellFactory(col -> new javafx.scene.control.TableCell<com.auction.shared.models.AuctionItem, String>() {
        private final java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);
          if (empty || item == null || item.isBlank()) {
            setText(null);
            return;
          }
          try {
            java.time.Instant inst = java.time.Instant.parse(item);
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.systemDefault());
            setText(zdt.format(fmt));
          } catch (Exception e) {
            setText(item);
          }
        }
      });
    } catch (Exception ignored) {}
  }

  private void configureListingsFilter() {
    if (listingsStatusChoice == null) return;

    if (listingsStatusChoice.getItems().isEmpty()) {
      listingsStatusChoice
        .getItems()
        .addAll(
          LISTING_FILTER_ALL,
          LISTING_FILTER_SCHEDULED,
          LISTING_FILTER_ACTIVE,
          LISTING_FILTER_SOLD,
          LISTING_FILTER_EXPIRED,
          LISTING_FILTER_CANCELLED
        );
    }

    if (listingsStatusChoice.getValue() == null) {
      listingsStatusChoice.setValue(LISTING_FILTER_ALL);
    }

    listingsStatusChoice
      .getSelectionModel()
      .selectedItemProperty()
      .addListener((obs, oldValue, newValue) -> applyListingsFilter());
  }

  @FXML
  private void handlePrepareEditAuction() {
    com.auction.shared.models.AuctionItem selected = myListingsTable == null ? null : myListingsTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      if (statusLabel != null) statusLabel.setText("Select one of your scheduled listings to edit.");
      return;
    }

    if (!com.auction.shared.Constants.STATUS_SCHEDULED.equalsIgnoreCase(selected.getStatus())) {
      if (statusLabel != null) statusLabel.setText("Only scheduled listings can be edited before they start.");
      return;
    }

    if (selected.getSellerUsername() != null && !selected.getSellerUsername().equalsIgnoreCase(com.auction.client.core.ClientContext.getInstance().getUsername())) {
      if (statusLabel != null) statusLabel.setText("You can only edit your own listings.");
      return;
    }

    populateEditForm(selected);
    updateEditMode(true, selected.getId());
    if (statusLabel != null) statusLabel.setText("Editing auction #" + selected.getId());
  }

  @FXML
  private void handleCancelEditMode() {
    clearEditMode();
  }

  private void updateEditMode(boolean editing, Integer auctionId) {
    editingAuctionId = editing ? auctionId : null;
    if (editing) {
      reschedulingAuctionId = null;
    }
    if (createAuctionButton != null) {
      createAuctionButton.setText(editing ? "Save Changes" : reschedulingAuctionId != null ? "Create Rescheduled Auction" : "Create Auction");
    }
  }

  private void updateRescheduleMode(boolean rescheduling, Integer auctionId) {
    reschedulingAuctionId = rescheduling ? auctionId : null;
    if (rescheduling) {
      editingAuctionId = null;
    }
    if (createAuctionButton != null) {
      createAuctionButton.setText(rescheduling ? "Create Rescheduled Auction" : editingAuctionId != null ? "Save Changes" : "Create Auction");
    }
  }

  private void clearEditMode() {
    updateEditMode(false, null);
    updateRescheduleMode(false, null);
    clearAuctionForm();
    if (statusLabel != null) statusLabel.setText("Edit cancelled.");
  }

  private void clearAuctionForm() {
    if (titleField != null) titleField.clear();
    if (descArea != null) descArea.clear();
    if (categoryField != null) categoryField.clear();
    if (priceField != null) priceField.clear();
    if (startDatePicker != null) startDatePicker.setValue(null);
    if (startTimeField != null) startTimeField.clear();
    if (endDatePicker != null) endDatePicker.setValue(null);
    if (endTimeField != null) endTimeField.clear();
    if (startModeChoice != null) startModeChoice.setValue("Automatic");
    if (minIncrementField != null) minIncrementField.setText("5");
    if (capEndMinutesField != null) capEndMinutesField.setText(String.valueOf(com.auction.shared.Constants.SNIPE_CAP_DEFAULT_MINUTES));
    img1Bytes = img2Bytes = img3Bytes = null;
    if (imagesLabel != null) imagesLabel.setText("No images selected");
  }

  private void populateEditForm(com.auction.shared.models.AuctionItem item) {
    if (item == null) return;
    if (titleField != null) titleField.setText(item.getTitle());
    if (descArea != null) descArea.setText(item.getDescription());
    if (categoryField != null) categoryField.setText(item.getCategory());
    if (priceField != null) priceField.setText(String.format(java.util.Locale.ROOT, "%.2f", item.getStartingPriceCents() / 100.0));

    try {
      java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
      if (item.getStartTime() != null && !item.getStartTime().isBlank()) {
        java.time.LocalDateTime startDateTime = java.time.LocalDateTime.ofInstant(java.time.Instant.parse(item.getStartTime()), zoneId);
        if (startDatePicker != null) startDatePicker.setValue(startDateTime.toLocalDate());
        if (startTimeField != null) startTimeField.setText(startDateTime.toLocalTime().withSecond(0).withNano(0).toString());
      }
      if (item.getEndTime() != null && !item.getEndTime().isBlank()) {
        java.time.LocalDateTime endDateTime = java.time.LocalDateTime.ofInstant(java.time.Instant.parse(item.getEndTime()), zoneId);
        if (endDatePicker != null) endDatePicker.setValue(endDateTime.toLocalDate());
        if (endTimeField != null) endTimeField.setText(endDateTime.toLocalTime().withSecond(0).withNano(0).toString());
      }
      if (item.getCapEndTime() != null && !item.getCapEndTime().isBlank() && item.getEndTime() != null && !item.getEndTime().isBlank()) {
        java.time.Duration capOffset = java.time.Duration.between(java.time.Instant.parse(item.getEndTime()), java.time.Instant.parse(item.getCapEndTime()));
        long capMinutes = Math.max(0L, capOffset.toMinutes());
        if (capEndMinutesField != null) capEndMinutesField.setText(String.valueOf(capMinutes));
      }
    } catch (Exception ignored) {}

    if (startModeChoice != null) {
      startModeChoice.setValue(com.auction.shared.Constants.START_MODE_MANUAL.equalsIgnoreCase(item.getStartMode()) ? "Manual" : "Automatic");
    }
    if (minIncrementField != null) {
      long pct = Math.round(item.getMinIncrementPercent() * 100.0);
      minIncrementField.setText(String.valueOf(pct));
    }
    if (imagesLabel != null) {
      imagesLabel.setText("Current images retained unless changed.");
    }
    img1Bytes = img2Bytes = img3Bytes = null;
  }

  private void populateRescheduleForm(com.auction.shared.models.AuctionItem item) {
    populateEditForm(item);
    try {
      java.time.Instant now = java.time.Instant.now();
      java.time.Instant suggestedStart = now.plus(java.time.Duration.ofMinutes(5));
      java.time.Duration duration = java.time.Duration.ofHours(1);
      if (item != null && item.getStartTime() != null && !item.getStartTime().isBlank() && item.getEndTime() != null && !item.getEndTime().isBlank()) {
        java.time.Duration original = java.time.Duration.between(
          java.time.Instant.parse(item.getStartTime()),
          java.time.Instant.parse(item.getEndTime())
        );
        if (!original.isNegative() && !original.isZero()) {
          duration = original;
        }
      }
      java.time.Instant suggestedEnd = suggestedStart.plus(duration);
      java.time.ZoneId zone = java.time.ZoneId.systemDefault();
      java.time.LocalDateTime startLocal = java.time.LocalDateTime.ofInstant(suggestedStart, zone);
      java.time.LocalDateTime endLocal = java.time.LocalDateTime.ofInstant(suggestedEnd, zone);
      if (startDatePicker != null) startDatePicker.setValue(startLocal.toLocalDate());
      if (startTimeField != null) startTimeField.setText(startLocal.toLocalTime().withSecond(0).withNano(0).toString());
      if (endDatePicker != null) endDatePicker.setValue(endLocal.toLocalDate());
      if (endTimeField != null) endTimeField.setText(endLocal.toLocalTime().withSecond(0).withNano(0).toString());
    } catch (Exception ignored) {}
    if (statusLabel != null) {
      statusLabel.setText("Reschedule mode: future start/end suggested. Update if needed.");
    }
    if (createAuctionButton != null) {
      createAuctionButton.setText("Create Rescheduled Auction");
    }
  }

  private com.auction.shared.models.AuctionItem buildAuctionFromForm() {
    long cents = (long) (Double.parseDouble(priceField.getText()) * 100);
    java.time.Instant now = java.time.Instant.now();
    java.time.Instant startInstant = now;
    if (startDatePicker != null && startDatePicker.getValue() != null && startTimeField != null && !startTimeField.getText().isBlank()) {
      java.time.LocalDate d = startDatePicker.getValue();
      java.time.LocalTime t = java.time.LocalTime.parse(startTimeField.getText());
      startInstant = java.time.ZonedDateTime.of(d, t, java.time.ZoneId.systemDefault()).toInstant();
    }

    java.time.Instant endInstant = startInstant.plus(java.time.Duration.ofHours(1));
    if (endDatePicker != null && endDatePicker.getValue() != null && endTimeField != null && !endTimeField.getText().isBlank()) {
      java.time.LocalDate ed = endDatePicker.getValue();
      java.time.LocalTime et = java.time.LocalTime.parse(endTimeField.getText());
      endInstant = java.time.ZonedDateTime.of(ed, et, java.time.ZoneId.systemDefault()).toInstant();
    }

    com.auction.shared.models.AuctionItem item = new com.auction.shared.models.AuctionItem();
    item.setTitle(titleField.getText());
    item.setDescription(descArea.getText());
    item.setCategory(categoryField.getText());
    item.setStartingPriceCents(cents);
    item.setCurrentBidCents(cents);
    item.setSellerUsername(com.auction.client.core.ClientContext.getInstance().getUsername());
    item.setStartTime(startInstant.toString());
    item.setEndTime(endInstant.toString());

    if (capEndMinutesField != null && capEndMinutesField.getText() != null && !capEndMinutesField.getText().isBlank()) {
      int minutes = Integer.parseInt(capEndMinutesField.getText().trim());
      if (minutes < 0 || minutes > 24 * 60) {
        throw new IllegalArgumentException("Snipe cap minutes must be between 0 and 1440");
      }
      item.setCapEndTime(endInstant.plus(java.time.Duration.ofMinutes(minutes)).toString());
    } else {
      item.setCapEndTime(null);
    }

    String mode = startModeChoice == null ? "Automatic" : startModeChoice.getValue();
    if ("Manual".equalsIgnoreCase(mode)) {
      item.setStartMode(com.auction.shared.Constants.START_MODE_MANUAL);
      item.setStatus(com.auction.shared.Constants.STATUS_SCHEDULED);
    } else {
      item.setStartMode(com.auction.shared.Constants.START_MODE_AUTO);
      item.setStatus(startInstant.isAfter(now) ? com.auction.shared.Constants.STATUS_SCHEDULED : com.auction.shared.Constants.STATUS_ACTIVE);
    }

    if (minIncrementField != null && minIncrementField.getText() != null && !minIncrementField.getText().isBlank()) {
      double pct = Double.parseDouble(minIncrementField.getText().trim());
      if (pct < 0 || pct > 100) {
        throw new IllegalArgumentException("Min increment percent must be between 0 and 100");
      }
      item.setMinIncrementPercent(pct / 100.0);
    }

    return item;
  }

  private void refreshDashboard() {
    javafx.concurrent.Task<DashboardSnapshot> task = new javafx.concurrent.Task<>() {
      @Override
      protected DashboardSnapshot call() throws Exception {
        com.auction.client.core.ClientContext context =
          com.auction.client.core.ClientContext.getInstance();
        com.auction.shared.interfaces.IAuctionService service = context
          .getRmiProvider()
          .getService();
        java.util.List<com.auction.shared.models.AuctionItem> activeAuctions =
          service.getActiveAuctions();
        java.util.List<com.auction.shared.models.AuctionItem> mine =
          service.getActiveAuctionsBySeller(
            context.getUsername(),
            context.getSessionToken()
          );
        java.util.List<com.auction.shared.models.Bid> bids = service.getMyBids(
          context.getSessionToken()
        );
        java.util.List<com.auction.shared.models.AuctionItem> won =
          service.getMyWonAuctions(context.getSessionToken());
        return new DashboardSnapshot(activeAuctions, mine, bids, won);
      }
    };

    task.setOnSucceeded(evt -> {
      DashboardSnapshot snapshot = task.getValue();
      if (snapshot == null) {
        if (statusLabel != null) statusLabel.setText("Dashboard refresh returned no data.");
        return;
      }

      marketTable.getItems().setAll(snapshot.activeAuctions);
      allMyListings = snapshot.mine;
      applyListingsFilter();
      myBidsTable.getItems().setAll(snapshot.bids);
      wonAuctionsTable.getItems().setAll(snapshot.won);

      long totalSalesCents = 0L;
      for (com.auction.shared.models.AuctionItem item : snapshot.mine) {
        if (item != null && com.auction.shared.Constants.STATUS_SOLD.equals(item.getStatus())) {
          totalSalesCents += Math.max(0L, item.getCurrentBidCents());
        }
      }

      if (totalSalesLabel != null) totalSalesLabel.setText(com.auction.shared.Constants.formatCents(totalSalesCents));

      if (marketCountLabel != null) marketCountLabel.setText(String.valueOf(snapshot.activeAuctions.size()));
      if (listingsCountLabel != null) listingsCountLabel.setText(String.valueOf(snapshot.mine.size()));
      if (bidsCountLabel != null) bidsCountLabel.setText(String.valueOf(snapshot.bids.size()));
      if (winsCountLabel != null) winsCountLabel.setText(String.valueOf(snapshot.won.size()));
      if (statusLabel != null) statusLabel.setText("Dashboard refreshed successfully.");
    });

    task.setOnFailed(evt -> {
      Throwable error = task.getException();
      if (statusLabel != null) {
        statusLabel.setText("Failed to load dashboard: " + (error == null ? "unknown error" : error.getMessage()));
      }
    });

    Thread thread = new Thread(task, "dashboard-refresh");
    thread.setDaemon(true);
    thread.start();
  }

  private void applyListingsFilter() {
    String selected = listingsStatusChoice == null
      ? LISTING_FILTER_ALL
      : listingsStatusChoice.getValue();

    if (
      selected == null ||
      selected.isBlank() ||
      LISTING_FILTER_ALL.equalsIgnoreCase(selected)
    ) {
      myListingsTable.getItems().setAll(allMyListings);
      return;
    }

    java.util.List<com.auction.shared.models.AuctionItem> filtered =
      allMyListings
        .stream()
        .filter(item -> item != null && selected.equalsIgnoreCase(item.getStatus()))
        .toList();
    myListingsTable.getItems().setAll(filtered);
  }

  @FXML
  private void handleRefreshDashboard() {
    refreshDashboard();
  }

  @FXML
  private void handleOpenGallery() {
    try {
      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      context.setPreviousViewName("user_dashboard.fxml");
      context.getViewLoader().loadView("gallery.fxml");
    } catch (Exception e) {
      if (statusLabel != null) {
        statusLabel.setText("Unable to open gallery: " + e.getMessage());
      }
      e.printStackTrace();
    }
  }

  @FXML
  private void handleOpenAuctionDetail() {
    com.auction.shared.models.AuctionItem selected = null;
    
    if (marketTable.getSelectionModel().getSelectedItem() != null) {
        selected = marketTable.getSelectionModel().getSelectedItem();
    } else if (myListingsTable.getSelectionModel().getSelectedItem() != null) {
        selected = myListingsTable.getSelectionModel().getSelectedItem();
    } else if (wonAuctionsTable.getSelectionModel().getSelectedItem() != null) {
        selected = wonAuctionsTable.getSelectionModel().getSelectedItem();
    }

    if (selected != null) {
        try {
          com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
          context.setCurrentAuctionId(selected.getId());
          context.setPreviousViewName("user_dashboard.fxml");
          Object controller = context.getViewLoader().loadView("auction_detail.fxml");
          if (controller instanceof com.auction.client.controllers.AuctionDetailController detailController) {
            detailController.setReturnViewName("user_dashboard.fxml");
          }
        } catch (java.io.IOException e) {
          throw new RuntimeException(e);
        }
    } else {
        if (statusLabel != null) statusLabel.setText("Please select an auction first.");
    }
  }

  @FXML
  private void handleCreateAuction() {
    try {
      com.auction.shared.models.AuctionItem item = buildAuctionFromForm();
      if (!java.time.Instant.parse(item.getEndTime()).isAfter(java.time.Instant.parse(item.getStartTime()))) {
        statusLabel.setText("End time must be after start time");
        return;
      }

      if (item.getStartMode() == null || item.getStartMode().isBlank()) {
        item.setStartMode(com.auction.shared.Constants.START_MODE_AUTO);
      }

      if (reschedulingAuctionId != null) {
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant startInstant = java.time.Instant.parse(item.getStartTime());
        if (!startInstant.isAfter(now)) {
          statusLabel.setText("Rescheduled auction start time must be in the future.");
          return;
        }
        item.setStatus(com.auction.shared.Constants.STATUS_SCHEDULED);
        item.setRelistedFrom(reschedulingAuctionId);
      }

      if (editingAuctionId != null) {
        com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
        context.getRmiProvider().getService().updateAuction(editingAuctionId, item, img1Bytes, img2Bytes, img3Bytes, context.getSessionToken());
        statusLabel.setText("Updated auction #" + editingAuctionId);
      } else {
          com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
          com.auction.shared.interfaces.IAuctionService service = context.getRmiProvider().getService();

          if (reschedulingAuctionId != null) {
            // Use server-side relist to copy parent image paths (preserves images),
            // then update the newly created child with any changed fields or newly provided images.
            java.time.Instant now = java.time.Instant.now();
            java.time.Instant requestedEnd = java.time.Instant.parse(item.getEndTime());
            java.time.Instant bootstrapMinEnd = now.plus(java.time.Duration.ofMinutes(6));
            java.time.Instant bootstrapEnd = requestedEnd.isAfter(bootstrapMinEnd) ? requestedEnd : bootstrapMinEnd;
            service.relistAuction(reschedulingAuctionId, bootstrapEnd.toString(), context.getSessionToken());

            // Find the newly created child auction by matching relistedFrom and seller.
            com.auction.shared.models.AuctionItem child = null;
            // Try a few times to find the newly-created child (small race window)
            for (int attempt = 0; attempt < 5 && child == null; attempt++) {
              java.util.List<com.auction.shared.models.AuctionItem> all = service.getAllAuctions();
              child = all.stream()
                .filter(a -> a != null && a.getRelistedFrom() != null && context.getUsername().equalsIgnoreCase(a.getSellerUsername())
                  && (a.getRelistedFrom().equals(reschedulingAuctionId) || a.getRelistedFrom().intValue() == reschedulingAuctionId.intValue()))
                .max(java.util.Comparator.comparingInt(a -> a.getId()))
                .orElse(null);
              if (child == null) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
              }
            }

            if (child == null) {
              statusLabel.setText("Reschedule succeeded but could not locate new auction.");
              refreshDashboard();
              updateRescheduleMode(false, null);
              clearAuctionForm();
              return;
            }

            int newId = child.getId();
            // Push any field edits and image replacements to the newly created scheduled auction.
            service.updateAuction(newId, item, img1Bytes, img2Bytes, img3Bytes, context.getSessionToken());

            statusLabel.setText("Created rescheduled auction #" + newId);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Auction Created");
            alert.setHeaderText("Rescheduled auction created successfully");
            alert.setContentText("Your rescheduled auction #" + newId + " is ready.");
            alert.showAndWait();
            refreshDashboard();
            selectCreatedAuction(newId);

            updateRescheduleMode(false, null);
            clearAuctionForm();
            return;
          } else {
            int id = service.createAuction(
              item,
              img1Bytes,
              img2Bytes,
              img3Bytes,
              context.getSessionToken()
            );
            statusLabel.setText("Created auction #" + id);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Auction Created");
            alert.setHeaderText("Auction created successfully");
            alert.setContentText("Your auction #" + id + " is ready.");
            alert.showAndWait();
            refreshDashboard();
            selectCreatedAuction(id);

            clearAuctionForm();
            return;
          }
      }

      refreshDashboard();
      clearEditMode();
    } catch (IllegalArgumentException e) {
      statusLabel.setText(e.getMessage());
    } catch (Exception e) {
      statusLabel.setText(editingAuctionId != null ? "Error saving: " + e.getMessage() : "Error creating: " + e.getMessage());
    }
  }

  @FXML
  private void handleCancelAuction() {
    com.auction.shared.models.AuctionItem selected = myListingsTable
      .getSelectionModel()
      .getSelectedItem();
    if (selected != null) {
      try {
        com.auction.client.core.ClientContext context =
          com.auction.client.core.ClientContext.getInstance();
        context
          .getRmiProvider()
          .getService()
          .cancelAuction(selected.getId(), context.getSessionToken());
        refreshDashboard();
      } catch (Exception e) {
        statusLabel.setText("Cancel failed: " + e.getMessage());
      }
    }
  }

  @FXML
  private void handleRelistAuction() {
    com.auction.shared.models.AuctionItem selected = myListingsTable
      .getSelectionModel()
      .getSelectedItem();
    if (selected != null) {
      if (!com.auction.shared.Constants.STATUS_CANCELLED.equalsIgnoreCase(selected.getStatus()) &&
          !com.auction.shared.Constants.STATUS_EXPIRED.equalsIgnoreCase(selected.getStatus())) {
        if (statusLabel != null) statusLabel.setText("Only cancelled or expired listings can be rescheduled.");
        return;
      }

      if (selected.getSellerUsername() != null && !selected.getSellerUsername().equalsIgnoreCase(com.auction.client.core.ClientContext.getInstance().getUsername())) {
        if (statusLabel != null) statusLabel.setText("You can only reschedule your own listings.");
        return;
      }

      boolean alreadyRelisted = allMyListings != null && allMyListings.stream()
        .anyMatch(a -> a != null && a.getRelistedFrom() != null && a.getRelistedFrom().intValue() == selected.getId());
      if (alreadyRelisted) {
        if (statusLabel != null) statusLabel.setText("This listing was already rescheduled and is kept as history.");
        return;
      }

      populateRescheduleForm(selected);
      updateRescheduleMode(true, selected.getId());
      if (statusLabel != null) statusLabel.setText("Rescheduling listing #" + selected.getId());
    }
  }

  @FXML
  private void handleStartAuction() {
    com.auction.shared.models.AuctionItem selected = null;
    if (marketTable.getSelectionModel().getSelectedItem() != null) {
      selected = marketTable.getSelectionModel().getSelectedItem();
    } else if (myListingsTable.getSelectionModel().getSelectedItem() != null) {
      selected = myListingsTable.getSelectionModel().getSelectedItem();
    } else if (wonAuctionsTable.getSelectionModel().getSelectedItem() != null) {
      selected = wonAuctionsTable.getSelectionModel().getSelectedItem();
    }

    if (selected != null) {
      try {
        com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
        context.getRmiProvider().getService().startAuction(selected.getId(), context.getSessionToken());
        refreshDashboard();
        if (com.auction.shared.Constants.START_MODE_MANUAL.equalsIgnoreCase(selected.getStartMode())) {
          showManualLaunchDialog(selected.getId());
        }
      } catch (Exception e) {
        if (statusLabel != null) statusLabel.setText("Start failed: " + e.getMessage());
      }
    } else {
      if (statusLabel != null) statusLabel.setText("Please select an auction first.");
    }
  }

  @FXML
  private void handleExportCSV() {
    try {
      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      byte[] csv = context
        .getRmiProvider()
        .getService()
        .exportAuctionsToCSV(context.getSessionToken());

      javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
      fileChooser.setTitle("Save Auction CSV Export");
      fileChooser.getExtensionFilters().add(
        new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
      );
      fileChooser.setInitialFileName("my_auctions_export.csv");

      java.io.File exportsDir = new java.io.File("exports");
      if (!exportsDir.exists() && !exportsDir.mkdirs()) {
        throw new IOException("Unable to create exports directory");
      }
      fileChooser.setInitialDirectory(exportsDir);

      java.io.File file = fileChooser.showSaveDialog(marketTable.getScene().getWindow());
      if (file == null) {
        if (statusLabel != null) statusLabel.setText("Export cancelled.");
        return;
      }

      String fileName = file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".csv")
        ? file.getName()
        : file.getName() + ".csv";
      java.io.File targetFile = new java.io.File(exportsDir, fileName);

      java.nio.file.Files.write(targetFile.toPath(), csv);
      if (statusLabel != null) statusLabel.setText("Exported to " + targetFile.getAbsolutePath());
    } catch (Exception e) {
      if (statusLabel != null) statusLabel.setText("Export failed: " + e.getMessage());
    }
  }

  @FXML
  private void handlePickImg1() {
    byte[] selected = pickImage("Image 1");
    if (selected != null) img1Bytes = selected;
    updateImagesLabel();
  }

  @FXML
  private void handlePickImg2() {
    byte[] selected = pickImage("Image 2");
    if (selected != null) img2Bytes = selected;
    updateImagesLabel();
  }

  @FXML
  private void handlePickImg3() {
    byte[] selected = pickImage("Image 3");
    if (selected != null) img3Bytes = selected;
    updateImagesLabel();
  }

  private byte[] pickImage(String label) {
    javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
    fc.getExtensionFilters().add(
      new javafx.stage.FileChooser.ExtensionFilter(
        "Images (JPG, JPEG, PNG)",
        "*.jpg",
        "*.jpeg",
        "*.png"
      )
    );
    File file = fc.showOpenDialog(marketTable.getScene().getWindow());
    if (file == null) {
      return null;
    }

    try {
      validateImageFile(file);
      return Files.readAllBytes(file.toPath());
    } catch (Exception e) {
      showImageRejected(label, e.getMessage());
      return null;
    }
  }

  private void validateImageFile(File file) throws IOException {
    long size = Files.size(file.toPath());
    if (size > com.auction.shared.Constants.MAX_IMAGE_SIZE_BYTES) {
      throw new IOException("Image must be 2 MB or smaller.");
    }

    try (javax.imageio.stream.ImageInputStream input = javax.imageio.ImageIO.createImageInputStream(file)) {
      if (input == null) {
        throw new IOException("Could not read image file.");
      }

      Iterator<javax.imageio.ImageReader> readers = javax.imageio.ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new IOException("Unsupported image format. Use JPG, JPEG, or PNG.");
      }

      javax.imageio.ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        String format = reader.getFormatName();
        if (format == null || !com.auction.shared.Constants.SUPPORTED_IMAGE_FORMATS.contains(format.toLowerCase())) {
          throw new IOException("Unsupported image format. Use JPG, JPEG, or PNG.");
        }

        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width > com.auction.shared.Constants.MAX_IMAGE_WIDTH || height > com.auction.shared.Constants.MAX_IMAGE_HEIGHT) {
          throw new IOException(
            "Image dimensions must be at most " +
            com.auction.shared.Constants.MAX_IMAGE_WIDTH +
            "x" +
            com.auction.shared.Constants.MAX_IMAGE_HEIGHT +
            " pixels."
          );
        }
      } finally {
        reader.dispose();
      }
    }
  }

  private void showImageRejected(String label, String reason) {
    String message = label + " rejected: " + reason;
    if (statusLabel != null) {
      statusLabel.setText(message);
    }

    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Invalid Image");
    alert.setHeaderText(label + " was not accepted");
    alert.setContentText(reason + " Allowed: JPG, JPEG, PNG; max size 2 MB; max dimensions " + com.auction.shared.Constants.MAX_IMAGE_WIDTH + "x" + com.auction.shared.Constants.MAX_IMAGE_HEIGHT + ".");
    alert.showAndWait();
  }

  private void updateImagesLabel() {
    int count = 0;
    if (img1Bytes != null) count++;
    if (img2Bytes != null) count++;
    if (img3Bytes != null) count++;
    imagesLabel.setText(count + " images selected");
  }

  private void selectCreatedAuction(int id) {
    try {
      javafx.collections.ObservableList<com.auction.shared.models.AuctionItem> items = marketTable.getItems();
      if (items != null) {
        for (com.auction.shared.models.AuctionItem item : items) {
          if (item != null && item.getId() == id) {
            marketTable.getSelectionModel().select(item);
            marketTable.scrollTo(item);
            return;
          }
        }
      }

      items = myListingsTable.getItems();
      if (items != null) {
        for (com.auction.shared.models.AuctionItem item : items) {
          if (item != null && item.getId() == id) {
            myListingsTable.getSelectionModel().select(item);
            myListingsTable.scrollTo(item);
            return;
          }
        }
      }

      if (allMyListings != null) {
        for (com.auction.shared.models.AuctionItem item : allMyListings) {
          if (item != null && item.getId() == id) {
            myListingsTable.getSelectionModel().select(item);
            myListingsTable.scrollTo(item);
            return;
          }
        }
      }
    } catch (Exception ignored) {
    }
  }

  private void showManualLaunchDialog(int auctionId) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Auction Launched");
    alert.setHeaderText("Auction launched successfully");
    alert.setContentText("Your listing is now live and accepting bids.");

    ButtonType viewAuction = new ButtonType("View auction", ButtonBar.ButtonData.OK_DONE);
    ButtonType dashboard = new ButtonType("Go to Dashboard", ButtonBar.ButtonData.CANCEL_CLOSE);
    alert.getButtonTypes().setAll(viewAuction, dashboard);

    java.util.Optional<ButtonType> choice = alert.showAndWait();
    if (choice.isPresent() && choice.get() == viewAuction) {
      openAuctionDetail(auctionId, "user_dashboard.fxml");
    }
  }

  private void openAuctionDetail(int auctionId, String returnView) {
    try {
      com.auction.client.core.ClientContext context = com.auction.client.core.ClientContext.getInstance();
      context.setCurrentAuctionId(auctionId);
      context.setPreviousViewName(returnView);
      Object controller = context.getViewLoader().loadView("auction_detail.fxml");
      if (controller instanceof com.auction.client.controllers.AuctionDetailController detailController) {
        detailController.setReturnViewName(returnView);
      }
    } catch (Exception e) {
      if (statusLabel != null) {
        statusLabel.setText("Unable to open auction detail: " + e.getMessage());
      }
    }
  }

  private static final class DashboardSnapshot {
    private final java.util.List<com.auction.shared.models.AuctionItem> activeAuctions;
    private final java.util.List<com.auction.shared.models.AuctionItem> mine;
    private final java.util.List<com.auction.shared.models.Bid> bids;
    private final java.util.List<com.auction.shared.models.AuctionItem> won;

    private DashboardSnapshot(
      java.util.List<com.auction.shared.models.AuctionItem> activeAuctions,
      java.util.List<com.auction.shared.models.AuctionItem> mine,
      java.util.List<com.auction.shared.models.Bid> bids,
      java.util.List<com.auction.shared.models.AuctionItem> won
    ) {
      this.activeAuctions = activeAuctions == null ? java.util.List.of() : activeAuctions;
      this.mine = mine == null ? java.util.List.of() : mine;
      this.bids = bids == null ? java.util.List.of() : bids;
      this.won = won == null ? java.util.List.of() : won;
    }
  }

  @FXML
  private void handleLogout() {
    try {
      com.auction.client.core.ClientContext context =
        com.auction.client.core.ClientContext.getInstance();
      context.getRmiProvider().getService().logout(context.getSessionToken());
      context.clearSession();
      context.getViewLoader().loadView("login.fxml");
    } catch (Exception e) {
      statusLabel.setText("Logout failed: " + e.getMessage());
    }
  }
}
