package oogasalad.view.accounts;

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
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.AuthService;
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

  public CreateAccountScreen(Consumer<Outcome> onOutcome, AuthService authService) {
    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());
    layout.getStyleClass().add("screen-root");

    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.OPEN_LOGIN));
    BorderPane.setMargin(backButton, new Insets(18, 0, 0, 18));
    layout.setTop(backButton);

    Label title = new Label(PROPS.getString("title"));
    title.getStyleClass().add("accounts-title");

    usernameField.setPromptText(PROPS.getString("usernamePlaceholder"));
    usernameField.setPrefWidth(Double.parseDouble(PROPS.getString("fieldWidth")));

    passwordField.setPromptText(PROPS.getString("passwordPlaceholder"));
    passwordField.setPrefWidth(Double.parseDouble(PROPS.getString("fieldWidth")));

    confirmPasswordField.setPromptText(PROPS.getString("confirmPasswordPlaceholder"));
    confirmPasswordField.setPrefWidth(Double.parseDouble(PROPS.getString("fieldWidth")));

    avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
    avatarPathLabel.getStyleClass().add("accounts-path-label");

    Button browseButton = new Button(PROPS.getString("browseButton"));
    browseButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    browseButton.setOnAction(e -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle(PROPS.getString("chooseAvatarTitle"));
      chooser.getExtensionFilters().add(
          new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
      File file = chooser.showOpenDialog(layout.getScene() != null ? layout.getScene().getWindow() : null);
      if (file != null) {
        selectedAvatarPath = file.getAbsolutePath();
        avatarPathLabel.setText(file.getName());
      }
    });

    HBox avatarRow = new HBox(8,
        new Label(PROPS.getString("avatarLabel")), avatarPathLabel, browseButton);
    avatarRow.setAlignment(Pos.CENTER_LEFT);

    errorLabel.getStyleClass().add("accounts-error");

    Button createButton = new Button(PROPS.getString("createButton"));
    createButton.setPrefWidth(Double.parseDouble(PROPS.getString("buttonWidth")));
    createButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    createButton.setOnAction(e -> {
      if (!passwordField.getText().equals(confirmPasswordField.getText())) {
        errorLabel.setText(PROPS.getString("passwordMismatch"));
        return;
      }
      AuthResult result = authService.register(
          usernameField.getText(), passwordField.getText(), selectedAvatarPath);
      if (result instanceof AuthResult.Success) {
        authService.logout();
        onOutcome.accept(Outcome.OPEN_LOGIN);
      } else if (result instanceof AuthResult.Failure f) {
        errorLabel.setText(f.reason());
      }
    });

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        title,
        new Label(PROPS.getString("usernameLabel")), usernameField,
        new Label(PROPS.getString("passwordLabel")), passwordField,
        new Label(PROPS.getString("confirmPasswordLabel")), confirmPasswordField,
        avatarRow, createButton, errorLabel);
    center.setAlignment(Pos.CENTER);
    center.setPadding(new Insets(Double.parseDouble(PROPS.getString("padding"))));
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
    avatarPathLabel.setText(PROPS.getString("noAvatarSelected"));
    selectedAvatarPath = "";
    errorLabel.setText("");
  }
}
