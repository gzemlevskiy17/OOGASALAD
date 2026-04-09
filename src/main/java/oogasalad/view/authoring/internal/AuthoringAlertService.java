package oogasalad.view.authoring.internal;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Small UI helper for displaying authoring-environment alerts.
 *
 * <p>This service keeps JavaFX alert creation in the view layer so controllers
 * and model logic can continue to communicate failures through exceptions.
 *
 * @author Emilio Garcia
 */
public class AuthoringAlertService {

  public void showError(String title, String message) {
    show(AlertType.ERROR, title, message);
  }

  public void showWarning(String title, String message) {
    show(AlertType.WARNING, title, message);
  }

  public void showInfo(String title, String message) {
    show(AlertType.INFORMATION, title, message);
  }

  private void show(AlertType alertType, String title, String message) {
    Alert alert = new Alert(alertType);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
