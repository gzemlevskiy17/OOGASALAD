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
  private final TextField usernameField = new TextField();
  private final PasswordField passwordField = new PasswordField();
  private final Label avatarPathLabel = new Label();
  private final Label statusLabel = new Label();
  private String selectedPath = "";

  public UpdateProfileScreen(Consumer<Outcome> onNav, Consumer<AccountOutcome> onAction) {
    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());
    layout.getStyleClass().add("screen-root");

    layout.setTop(buildBackButton(onNav));

    Label title = new Label(PROPS.getString("title"));
    title.getStyleClass().add("accounts-title");

    statusLabel.getStyleClass().add("accounts-error");

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        title, buildUsernameRow(onAction), buildPasswordRow(onAction),
        buildAvatarRow(onAction), statusLabel);
    center.setAlignment(Pos.CENTER);
    center.getStyleClass().add("accounts-form-wide");
    layout.setCenter(center);

    this.root = layout;
  }

  private Button buildBackButton(Consumer<Outcome> onNav) {
    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onNav.accept(Outcome.OPEN_PROFILE));
    BorderPane.setMargin(backButton, new Insets(18, 0, 0, 18));
    return backButton;
  }

  private HBox buildUsernameRow(Consumer<AccountOutcome> onAction) {
    usernameField.setPromptText(PROPS.getString("usernameLabel"));
    Button saveUsernameButton = new Button(PROPS.getString("saveUsernameButton"));
    saveUsernameButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    saveUsernameButton.setOnAction(e -> onAction.accept(
        new AccountOutcome.UpdateUsernameRequested(usernameField.getText())));
    HBox usernameRow = new HBox(10, usernameField, saveUsernameButton);
    usernameRow.setAlignment(Pos.CENTER);
    return usernameRow;
  }

  private HBox buildPasswordRow(Consumer<AccountOutcome> onAction) {
    passwordField.setPromptText(PROPS.getString("passwordLabel"));
    Button savePasswordButton = new Button(PROPS.getString("savePasswordButton"));
    savePasswordButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    savePasswordButton.setOnAction(e -> onAction.accept(
        new AccountOutcome.UpdatePasswordRequested(passwordField.getText())));
    HBox passwordRow = new HBox(10, passwordField, savePasswordButton);
    passwordRow.setAlignment(Pos.CENTER);
    return passwordRow;
  }

  private HBox buildAvatarRow(Consumer<AccountOutcome> onAction) {
    avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
    avatarPathLabel.getStyleClass().add("accounts-path-label");
    Button browseButton = new Button(PROPS.getString("browseButton"));
    browseButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    browseButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle(PROPS.getString("chooseAvatarTitle"));
      File file = chooser.showOpenDialog(null);
      if (file != null) {
        selectedPath = file.getAbsolutePath();
        avatarPathLabel.setText(file.getName());
      }
    });
    Button saveAvatarButton = new Button(PROPS.getString("saveButton"));
    saveAvatarButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    saveAvatarButton.setOnAction(e -> onAction.accept(
        new AccountOutcome.UpdateAvatarRequested(selectedPath)));
    HBox avatarRow = new HBox(10, avatarPathLabel, browseButton, saveAvatarButton);
    avatarRow.setAlignment(Pos.CENTER);
    return avatarRow;
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
    avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
    selectedPath = "";
  }

  public void displayError(String message) {
    statusLabel.setText(message);
  }
}
