package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.interfaces.IAuctionService;
import com.auction.shared.models.AuctionItem;
import com.auction.shared.models.User;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class AdminPanelController {

  @FXML
  private VBox contentArea;

  @FXML
  public void initialize() {
    showUsers();
  }

  @FXML
  private void showUsers() {
    try {
      IAuctionService service = getService();
      List<User> users = service.getAllUsers(
        ClientContext.getInstance().getSessionToken()
      );
      contentArea
        .getChildren()
        .setAll(
          buildHeader("Users", "All registered users in the system."),
          buildUsersTable(users)
        );
    } catch (Exception e) {
      showError("Failed to load users: " + e.getMessage());
    }
  }

  @FXML
  private void showAuctions() {
    try {
      IAuctionService service = getService();
      List<AuctionItem> auctions = service.getActiveAuctions();
      contentArea
        .getChildren()
        .setAll(
          buildHeader(
            "Auctions",
            "Active auctions visible to the admin account."
          ),
          buildAuctionsTable(auctions)
        );
    } catch (Exception e) {
      showError("Failed to load auctions: " + e.getMessage());
    }
  }

  @FXML
  private void showLogs() {
    try {
      IAuctionService service = getService();
      List<String> logs = service.getAuditLogs(
        200,
        ClientContext.getInstance().getSessionToken()
      );
      VBox logsBox = new VBox(8);
      logsBox.getStyleClass().add("panel-card");
      logsBox
        .getChildren()
        .add(
          buildHeader(
            "Audit Logs",
            "Most recent administrative and system events."
          )
        );

      if (logs.isEmpty()) {
        logsBox.getChildren().add(new Label("No audit logs available."));
      } else {
        for (String line : logs) {
          Label entry = new Label(line);
          entry.setWrapText(true);
          entry.getStyleClass().add("section-copy");
          logsBox.getChildren().add(entry);
        }
      }

      contentArea.getChildren().setAll(logsBox);
    } catch (AuctionException e) {
      showError("Failed to load logs: " + e.getMessage());
    } catch (Exception e) {
      showError("Failed to load logs: " + e.getMessage());
    }
  }

  @FXML
  private void handleRefreshDashboard() {
    showUsers();
  }

  @FXML
  private void handleLogout() {
    try {
      ClientContext ctx = ClientContext.getInstance();
      ctx.getRmiProvider().getService().logout(ctx.getSessionToken());
      ctx.clearSession();
      ctx.getViewLoader().loadView("login.fxml");
    } catch (Exception e) {
      showError("Logout failed: " + e.getMessage());
    }
  }

  private IAuctionService getService() throws Exception {
    return ClientContext.getInstance().getRmiProvider().getService();
  }

  private Node buildHeader(String title, String subtitle) {
    VBox header = new VBox(4);
    header.getStyleClass().add("panel-card");

    Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("section-title");

    Label subtitleLabel = new Label(subtitle);
    subtitleLabel.getStyleClass().add("section-copy");
    subtitleLabel.setWrapText(true);

    header.getChildren().addAll(titleLabel, subtitleLabel);
    return header;
  }

  private TableView<User> buildUsersTable(List<User> users) {
    TableView<User> table = new TableView<>();
    table.getStyleClass().add("data-table");

    TableColumn<User, String> usernameCol = new TableColumn<>("Username");
    usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
    usernameCol.setPrefWidth(180);

    TableColumn<User, String> roleCol = new TableColumn<>("Role");
    roleCol.setCellValueFactory(new PropertyValueFactory<>("roleType"));
    roleCol.setPrefWidth(100);

    TableColumn<User, String> createdCol = new TableColumn<>("Created At");
    createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    createdCol.setPrefWidth(220);

    table.getColumns().addAll(usernameCol, roleCol, createdCol);
    table.getItems().setAll(users);
    return table;
  }

  private TableView<AuctionItem> buildAuctionsTable(
    List<AuctionItem> auctions
  ) {
    TableView<AuctionItem> table = new TableView<>();
    table.getStyleClass().add("auction-table");

    TableColumn<AuctionItem, Integer> idCol = new TableColumn<>("ID");
    idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
    idCol.setPrefWidth(70);

    TableColumn<AuctionItem, String> titleCol = new TableColumn<>("Title");
    titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
    titleCol.setPrefWidth(200);

    TableColumn<AuctionItem, String> sellerCol = new TableColumn<>("Seller");
    sellerCol.setCellValueFactory(new PropertyValueFactory<>("sellerUsername"));
    sellerCol.setPrefWidth(140);

    TableColumn<AuctionItem, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    statusCol.setPrefWidth(100);

    TableColumn<AuctionItem, Long> bidCol = new TableColumn<>("Current Bid");
    bidCol.setCellValueFactory(new PropertyValueFactory<>("currentBidCents"));
    bidCol.setPrefWidth(130);
    bidCol.setCellFactory(col ->
      new javafx.scene.control.TableCell<>() {
        @Override
        protected void updateItem(Long value, boolean empty) {
          super.updateItem(value, empty);
          setText(empty || value == null ? null : Constants.formatCents(value));
        }
      }
    );

    table.getColumns().addAll(idCol, titleCol, sellerCol, statusCol, bidCol);
    table.getItems().setAll(auctions);
    return table;
  }

  private void showError(String message) {
    contentArea.getChildren().setAll(buildHeader("Admin Panel", message));
  }
}
