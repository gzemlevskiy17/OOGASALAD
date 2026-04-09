package oogasalad.view.accounts.internal;

import java.io.File;
import java.util.ResourceBundle;
import java.util.function.Consumer;
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

public class CreateAccountScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.CreateAccountScreen");

  private final Parent root;
  private final TextField usernameField = new TextField();
  private final PasswordField passwordField = new PasswordField();
  private final PasswordField confirmPasswordField = new PasswordField();
  private final Label avatarPathLabel = new Label();
  private final Label errorLabel = new Label();
  private String selectedAvatarPath = "";

  public CreateAccountScreen(Consumer<Outcome> onNav, Consumer<AccountOutcome> onAction) {
    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());

    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.setOnAction(e -> onNav.accept(Outcome.OPEN_LOGIN));
    layout.setTop(backButton);

    double fieldWidth = Double.parseDouble(PROPS.getString("fieldWidth"));
    usernameField.setPromptText(PROPS.getString("usernamePlaceholder"));
    usernameField.setMaxWidth(fieldWidth);
    passwordField.setPromptText(PROPS.getString("passwordPlaceholder"));
    passwordField.setMaxWidth(fieldWidth);
    confirmPasswordField.setPromptText(PROPS.getString("confirmPasswordPlaceholder"));
    confirmPasswordField.setMaxWidth(fieldWidth);
    avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
    avatarPathLabel.getStyleClass().add("accounts-path-label");
    errorLabel.getStyleClass().add("accounts-error");

    Button browseButton = new Button(PROPS.getString("browseButton"));
    browseButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      File file = chooser.showOpenDialog(null);
      if (file != null) {
        selectedAvatarPath = file.getAbsolutePath();
        avatarPathLabel.setText(file.getName());
      }
    });

    Button createButton = new Button(PROPS.getString("createButton"));
    createButton.setOnAction(e -> {
      if (!passwordField.getText().equals(confirmPasswordField.getText())) {
        errorLabel.setText(PROPS.getString("passwordMismatch"));
        return;
      }
      onAction.accept(new AccountOutcome.RegisterRequested(
          usernameField.getText(), passwordField.getText(), selectedAvatarPath));
    });

    VBox center = new VBox(10, usernameField, passwordField, confirmPasswordField,
        new HBox(avatarPathLabel, browseButton), createButton, errorLabel);
    center.setAlignment(Pos.CENTER);
    center.getStyleClass().add("accounts-form");
    layout.setCenter(center);

    this.root = layout;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  @Override
  public void onEnter() {
    usernameField.clear();
    passwordField.clear();
    confirmPasswordField.clear();
    errorLabel.setText("");
  }

  public void displayError(String message) {
    errorLabel.setText(message);
  }
}