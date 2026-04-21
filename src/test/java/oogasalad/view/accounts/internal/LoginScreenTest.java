package oogasalad.view.accounts.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.accounts.api.AccountOutcome;
import oogasalad.view.accounts.api.AccountOutcome.LoginRequested;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

public class LoginScreenTest extends DukeApplicationTest {

  private final List<AccountOutcome> capturedActions = new ArrayList<>();
  private final List<Outcome> capturedNavs = new ArrayList<>();
  private LoginScreen screen;

  @Override
  public void start(Stage stage) {
    screen = new LoginScreen(capturedNavs::add, capturedActions::add);
    stage.setScene(new Scene(screen.getRoot(), 600, 400));
    stage.show();
  }

  // ── login button ──────────────────────────────────────────────────────────

  @Test
  void loginButton_withCredentials_firesLoginRequested() {
    TextField userField = lookup(".text-field").query();
    PasswordField passField = lookup(".password-field").query();

    writeInputTo(userField, "alice");
    writeInputTo(passField, "secret");
    clickOn("Login");

    assertEquals(1, capturedActions.size());
    AccountOutcome action = capturedActions.get(0);
    assertInstanceOf(LoginRequested.class, action);
    AccountOutcome.LoginRequested req = (AccountOutcome.LoginRequested) action;
    assertEquals("alice", req.user());
    assertEquals("secret", req.pass());
  }

  @Test
  void loginButton_emptyFields_stillFiresLoginRequested() {
    // The screen itself does not validate — that's the controller's job.
    // Clicking login with empty fields must still emit the intent.
    clickOn("Login");
    assertEquals(1, capturedActions.size());
    assertInstanceOf(LoginRequested.class, capturedActions.get(0));
  }

  // ── back button ───────────────────────────────────────────────────────────

  @Test
  void backButton_navigatesBackToMenu() {
    clickOn("Back to Menu");
    assertEquals(1, capturedNavs.size());
    assertEquals(Outcome.BACK_TO_MENU, capturedNavs.get(0));
  }

  // ── create account button ─────────────────────────────────────────────────

  @Test
  void createAccountButton_navigatesToCreateAccountScreen() {
    clickOn("Create Account");
    assertEquals(1, capturedNavs.size());
    assertEquals(Outcome.OPEN_CREATE_ACCOUNT, capturedNavs.get(0));
  }

  // ── displayError ─────────────────────────────────────────────────────────

  @Test
  void displayError_showsMessageInErrorLabel() {
    runAsJFXAction(() -> screen.displayError("Invalid credentials"));
    Label errorLabel = lookup(".accounts-error").query();
    assertEquals("Invalid credentials", errorLabel.getText());
  }

  @Test
  void displayError_canBeCalledMultipleTimes_showsLatest() {
    runAsJFXAction(() -> screen.displayError("first"));
    runAsJFXAction(() -> screen.displayError("second"));
    Label errorLabel = lookup(".accounts-error").query();
    assertEquals("second", errorLabel.getText());
  }

  // ── onEnter ──────────────────────────────────────────────────────────────

  @Test
  void onEnter_clearsUsernameAndPasswordFields() {
    TextField userField = lookup(".text-field").query();
    PasswordField passField = lookup(".password-field").query();

    writeInputTo(userField, "alice");
    writeInputTo(passField, "secret");

    runAsJFXAction(() -> screen.onEnter());

    assertEquals("", userField.getText());
    assertEquals("", passField.getText());
  }

  @Test
  void onEnter_clearsExistingErrorMessage() {
    runAsJFXAction(() -> screen.displayError("some error"));
    runAsJFXAction(() -> screen.onEnter());
    Label errorLabel = lookup(".accounts-error").query();
    assertEquals("", errorLabel.getText());
  }
}
