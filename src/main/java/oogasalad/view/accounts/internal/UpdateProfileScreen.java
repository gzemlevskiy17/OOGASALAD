package oogasalad.view.accounts.internal;

import java.io.File;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.accounts.api.AccountOutcome;
import oogasalad.view.core.api.AppScreen;

public class UpdateProfileScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.UpdateProfileScreen");

  private final Parent root;
  private final Label statusLabel = new Label();
  private String selectedPath = "";

  public UpdateProfileScreen(Consumer<Outcome> onNav, Consumer<AccountOutcome> onAction) {
    BorderPane layout = new BorderPane();

    Button browseButton = new Button(PROPS.getString("browseButton"));
    browseButton.setOnAction(e -> {
      File file = new FileChooser().showOpenDialog(null);
      if (file != null) {
        selectedPath = file.getAbsolutePath();
      }
    });

    Button saveButton = new Button(PROPS.getString("saveButton"));
    saveButton.setOnAction(
        e -> onAction.accept(new AccountOutcome.UpdateAvatarRequested(selectedPath)));

    layout.setCenter(new VBox(10, browseButton, saveButton, statusLabel));
    this.root = layout;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void onEnter() {
    statusLabel.setText("");
  }

  public void displayError(String message) {
    statusLabel.setText(message);
  }
}