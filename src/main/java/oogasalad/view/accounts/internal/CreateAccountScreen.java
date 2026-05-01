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

/**
 * Account registration screen that collects username, password, and optional avatar selection.
 *
 * @author Paulina Vvedenskaya
 */
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
    layout.getStyleClass().add("screen-root");

    layout.setTop(buildBackButton(onNav));

    Label title = new Label(PROPS.getString("title"));
    title.getStyleClass().add("accounts-title");

    usernameField.setPromptText(PROPS.getString("usernamePlaceholder"));
    passwordField.setPromptText(PROPS.getString("passwordPlaceholder"));
    confirmPasswordField.setPromptText(PROPS.getString("confirmPasswordPlaceholder"));
    errorLabel.getStyleClass().add("accounts-error");

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        title, usernameField, passwordField, confirmPasswordField,
        buildAvatarRow(), buildCreateButton(onAction), errorLabel);
    center.setAlignment(Pos.CENTER);
    center.getStyleClass().add("accounts-form");
    layout.setCenter(center);

    this.root = layout;
  }

  private Button buildBackButton(Consumer<Outcome> onNav) {
    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onNav.accept(Outcome.OPEN_LOGIN));
    BorderPane.setMargin(backButton, new Insets(18, 0, 0, 18));
    return backButton;
  }

  private HBox buildAvatarRow() {
    avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
    avatarPathLabel.getStyleClass().add("accounts-path-label");
    Button browseButton = new Button(PROPS.getString("browseButton"));
    browseButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    browseButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle(PROPS.getString("chooseAvatarTitle"));
      File file = chooser.showOpenDialog(null);
      if (file != null) {
        selectedAvatarPath = file.getAbsolutePath();
        avatarPathLabel.setText(file.getName());
      }
    });
    HBox avatarRow = new HBox(10, avatarPathLabel, browseButton);
    avatarRow.setAlignment(Pos.CENTER);
    return avatarRow;
  }

  private Button buildCreateButton(Consumer<AccountOutcome> onAction) {
    Button createButton = new Button(PROPS.getString("createButton"));
    createButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    createButton.setOnAction(e -> {
      if (!passwordField.getText().equals(confirmPasswordField.getText())) {
        errorLabel.setText(PROPS.getString("passwordMismatch"));
        return;
      }
      onAction.accept(new AccountOutcome.RegisterRequested(
          usernameField.getText(), passwordField.getText(), selectedAvatarPath));
    });
    return createButton;
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
    avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
    selectedAvatarPath = "";
  }

  public void displayError(String message) {
    errorLabel.setText(message);
  }
}
