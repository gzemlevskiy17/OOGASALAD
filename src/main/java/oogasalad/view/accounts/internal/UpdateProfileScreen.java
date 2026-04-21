package oogasalad.view.accounts.internal;

import java.io.File;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
  private final TextField usernameField = new TextField();
  private final PasswordField passwordField = new PasswordField();
  private String selectedPath = "";

  public UpdateProfileScreen(Consumer<Outcome> onNav, Consumer<AccountOutcome> onAction) {
    BorderPane layout = new BorderPane();
    layout.setPadding(new Insets(20));

    Label titleLabel = new Label(PROPS.getString("title"));
    titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
    BorderPane.setAlignment(titleLabel, Pos.CENTER);

    HBox usernameRow = new HBox(10, new Label(PROPS.getString("usernameLabel")),
        usernameField, makeSaveUsernameButton(onAction));
    usernameRow.setAlignment(Pos.CENTER);

    HBox passwordRow = new HBox(10, new Label(PROPS.getString("passwordLabel")),
        passwordField, makeSavePasswordButton(onAction));
    passwordRow.setAlignment(Pos.CENTER);

    HBox avatarRow = new HBox(10, new Label(PROPS.getString("avatarLabel")),
        makeBrowseButton(), makeSaveAvatarButton(onAction));
    avatarRow.setAlignment(Pos.CENTER);

    VBox centerBox = new VBox(15, usernameRow, passwordRow, avatarRow, statusLabel);
    centerBox.setAlignment(Pos.CENTER);

    layout.setTop(titleLabel);
    layout.setCenter(centerBox);
    this.root = layout;
  }

  private Button makeBrowseButton() {
    Button browseButton = new Button(PROPS.getString("browseButton"));
    browseButton.setOnAction(e -> {
      File file = new FileChooser().showOpenDialog(null);
      if (file != null) {
        selectedPath = file.getAbsolutePath();
      }
    });
    return browseButton;
  }

  private Button makeSaveAvatarButton(Consumer<AccountOutcome> onAction) {
    Button saveButton = new Button(PROPS.getString("saveButton"));
    saveButton.setOnAction(
        e -> onAction.accept(new AccountOutcome.UpdateAvatarRequested(selectedPath)));
    return saveButton;
  }

  private Button makeSaveUsernameButton(Consumer<AccountOutcome> onAction) {
    Button saveUsernameButton = new Button(PROPS.getString("saveUsernameButton"));
    saveUsernameButton.setOnAction(
        e -> onAction.accept(new AccountOutcome.UpdateUsernameRequested(usernameField.getText())));
    return saveUsernameButton;
  }

  private Button makeSavePasswordButton(Consumer<AccountOutcome> onAction) {
    Button savePasswordButton = new Button(PROPS.getString("savePasswordButton"));
    savePasswordButton.setOnAction(
        e -> onAction.accept(new AccountOutcome.UpdatePasswordRequested(passwordField.getText())));
    return savePasswordButton;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void onEnter() {
    statusLabel.setText("");
    usernameField.clear();
    passwordField.clear();
  }

  public void displayError(String message) {
    statusLabel.setText(message);
  }
}
