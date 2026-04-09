package oogasalad.view.accounts;

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
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.auth.api.AuthResult;
import oogasalad.model.auth.api.AuthService;
import oogasalad.view.core.api.AppScreen;

public class LoginScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.LoginScreen");

  private final Parent root;
  private final TextField usernameField = new TextField();
  private final PasswordField passwordField = new PasswordField();
  private final Label errorLabel = new Label();

  public LoginScreen(Consumer<Outcome> onOutcome, AuthService authService) {
    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());
    layout.getStyleClass().add("screen-root");

    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onOutcome.accept(Outcome.BACK_TO_MENU));
    BorderPane.setMargin(backButton, new Insets(18, 0, 0, 18));
    layout.setTop(backButton);

    Label title = new Label(PROPS.getString("title"));
    title.getStyleClass().add("accounts-title");

    usernameField.setPromptText(PROPS.getString("usernamePlaceholder"));
    usernameField.setPrefWidth(Double.parseDouble(PROPS.getString("fieldWidth")));

    passwordField.setPromptText(PROPS.getString("passwordPlaceholder"));
    passwordField.setPrefWidth(Double.parseDouble(PROPS.getString("fieldWidth")));

    errorLabel.getStyleClass().add("accounts-error");

    Button loginButton = new Button(PROPS.getString("loginButton"));
    loginButton.setPrefWidth(Double.parseDouble(PROPS.getString("buttonWidth")));
    loginButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    loginButton.setOnAction(e -> {
      AuthResult result = authService.login(usernameField.getText(), passwordField.getText());
      if (result instanceof AuthResult.Success) {
        passwordField.clear();
        errorLabel.setText("");
        onOutcome.accept(Outcome.BACK_TO_MENU);
      } else if (result instanceof AuthResult.Failure(String reason)) {
        errorLabel.setText(reason);
      }
    });

    Button createButton = new Button(PROPS.getString("createAccountLink"));
    createButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    createButton.setOnAction(e -> onOutcome.accept(Outcome.OPEN_CREATE_ACCOUNT));

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        title,
        new Label(PROPS.getString("usernameLabel")), usernameField,
        new Label(PROPS.getString("passwordLabel")), passwordField,
        loginButton, createButton, errorLabel);
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
    errorLabel.setText("");
  }
}
