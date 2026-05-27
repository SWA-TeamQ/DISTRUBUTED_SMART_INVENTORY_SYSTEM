package com.auction.client.controllers;

import com.auction.client.core.ClientContext;
import com.auction.shared.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegistrationController {

  @FXML
  private TextField usernameField;

  @FXML
  private PasswordField passwordField;

  @FXML
  private Label statusLabel;

  @FXML
  public void initialize() {
    if (statusLabel != null) {
      statusLabel.managedProperty().bind(statusLabel.visibleProperty());
      statusLabel.setVisible(false);
    }
  }

  @FXML
  private void handleNavigateToLogin() {
    try {
      ClientContext.getInstance().getViewLoader().loadView("login.fxml");
    } catch (Exception e) {
      showError("Navigation error: " + e.getMessage());
    }
  }

  @FXML
  private void handleRegister() {
    String u = usernameField.getText().trim();
    String p = passwordField.getText().trim();

    if (u.isEmpty() || p.isEmpty()) {
      showError("All fields are required.");
      return;
    }

    try {
      var ctx = ClientContext.getInstance();
      var service = ctx.getRmiProvider().getService();

      service.register(u, p, Constants.USER);
      String token = service.login(u, p);

      ctx.setSessionToken(token);
      ctx.setUsername(u);
      ctx.setUserRole(service.getMyRole(token));

      ctx.getViewLoader().loadView("user_dashboard.fxml");
    } catch (Exception e) {
      showError("Registration failed: " + e.getMessage());
    }
  }

  private void showError(String msg) {
    if (statusLabel != null) {
      statusLabel.setText(msg);
      statusLabel.setVisible(true);
    }
  }
}
