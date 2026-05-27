package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

  @FXML
  private TextField usernameField;

  @FXML
  private PasswordField passwordField;

  @FXML
  private Label statusLabel;

  @FXML
  private TitledPane adminErrorPanel;

  @FXML
  private TextArea adminErrorDetails;

  @FXML
  public void initialize() {
    if (statusLabel != null) {
      statusLabel.managedProperty().bind(statusLabel.visibleProperty());
      statusLabel.setVisible(false);
    }
    if (adminErrorPanel != null) {
      adminErrorPanel.setManaged(false);
      adminErrorPanel.setVisible(false);
    }
  }

  @FXML
  private void handleNavigateToRegister() {
    try {
      ClientContext.getInstance().getViewLoader().loadView("registration.fxml");
    } catch (Exception e) {
      showError("Navigation error: " + e.getMessage());
    }
  }

  @FXML
  private void handleLogin() {
    String user = usernameField.getText().trim();
    String pass = passwordField.getText().trim();

    if (user.isEmpty() || pass.isEmpty()) {
      showError("Enter username and password.");
      return;
    }

    try {
      var ctx = ClientContext.getInstance();
      var service = ctx.getRmiProvider().getService();
      String token = service.login(user, pass);

      ctx.setSessionToken(token);
      ctx.setUsername(user);
      ctx.setUserRole(service.getMyRole(token));

      String view = Constants.ADMIN.equals(ctx.getUserRole())
        ? "admin_panel.fxml"
        : "user_dashboard.fxml";
      ctx.getViewLoader().loadView(view);
    } catch (AuctionException e) {
      showError(e.getMessage());
    } catch (Exception e) {
      showError("System error: " + e.getMessage());
      showDebug(e);
    }
  }

  private void showError(String msg) {
    if (statusLabel != null) {
      statusLabel.setText(msg);
      statusLabel.setVisible(true);
    }
  }

  private void showDebug(Exception e) {
    if (adminErrorPanel != null) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      adminErrorDetails.setText(sw.toString());
      adminErrorPanel.setManaged(true);
      adminErrorPanel.setVisible(true);
    }
  }
}
