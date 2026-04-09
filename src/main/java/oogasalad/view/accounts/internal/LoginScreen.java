package oogasalad.view.accounts.internal;

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
import oogasalad.view.accounts.api.AccountOutcome;
import oogasalad.view.core.api.AppScreen;

public class LoginScreen implements AppScreen {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.accounts.LoginScreen");

  private final Parent root;
  private final TextField usernameField = new TextField();
  private final PasswordField passwordField = new PasswordField();
  private final Label errorLabel = new Label();

  public LoginScreen(Consumer<Outcome> onNav, Consumer<AccountOutcome> onAction) {
    BorderPane layout = new BorderPane();
    layout.getStylesheets().add(
        getClass().getResource("/oogasalad/view/accounts/accounts.css").toExternalForm());
    layout.getStyleClass().add("screen-root");

    Button backButton = new Button(PROPS.getString("backButton"));
    backButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    backButton.setOnAction(e -> onNav.accept(Outcome.BACK_TO_MENU));
    BorderPane.setMargin(backButton, new Insets(18, 0, 0, 18));
    layout.setTop(backButton);

    Label title = new Label(PROPS.getString("title"));
    title.getStyleClass().add("accounts-title");

    usernameField.setPromptText(PROPS.getString("usernamePlaceholder"));
    passwordField.setPromptText(PROPS.getString("passwordPlaceholder"));
    errorLabel.getStyleClass().add("accounts-error");

    Button loginButton = new Button(PROPS.getString("loginButton"));
    loginButton.getStyleClass().addAll("menu-button", "menu-primary-button");
    loginButton.setOnAction(e -> onAction.accept(
        new AccountOutcome.LoginRequested(usernameField.getText(), passwordField.getText())
    ));

    Button createButton = new Button(PROPS.getString("createAccountLink"));
    createButton.getStyleClass().addAll("menu-button", "menu-secondary-button");
    createButton.setOnAction(e -> onNav.accept(Outcome.OPEN_CREATE_ACCOUNT));

    VBox center = new VBox(
        Double.parseDouble(PROPS.getString("spacing")),
        title, usernameField, passwordField, loginButton, createButton, errorLabel);
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
    errorLabel.setText("");
  }

  public void displayError(String message) {
    errorLabel.setText(message);
  }
}