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
import oogasalad.view.accounts.api.AccountOutcome.RegisterRequested;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

public class CreateAccountScreenTest extends DukeApplicationTest {

  private final List<AccountOutcome> capturedActions = new ArrayList<>();
  private final List<Outcome> capturedNavs = new ArrayList<>();
  private CreateAccountScreen screen;

  @Override
  public void start(Stage stage) {
    screen = new CreateAccountScreen(capturedNavs::add, capturedActions::add);
    stage.setScene(new Scene(screen.getRoot(), 600, 500));
    stage.show();
  }

  // ── back button ───────────────────────────────────────────────────────────

  @Test
  void backButton_navigatesToLoginScreen() {
    clickOn("Back to Login");
    assertEquals(1, capturedNavs.size());
    assertEquals(Outcome.OPEN_LOGIN, capturedNavs.get(0));
  }

  // ── create button ─────────────────────────────────────────────────────────

  @Test
  void createButton_matchingPasswords_firesRegisterRequested() {
    // Three PasswordFields and one TextField — index order in the scene
    TextField userField = lookup(".text-field").query();

    // The two PasswordFields are the password and confirm fields
    List<PasswordField> pwFields = lookup(".password-field").<PasswordField>queryAll()
        .stream().toList();

    writeInputTo(userField, "bob");
    writeInputTo(pwFields.get(0), "pass123");
    writeInputTo(pwFields.get(1), "pass123");

    clickOn("Create Account");

    assertEquals(1, capturedActions.size());
    assertInstanceOf(RegisterRequested.class, capturedActions.get(0));
    AccountOutcome.RegisterRequested req =
        (AccountOutcome.RegisterRequested) capturedActions.get(0);
    assertEquals("bob", req.user());
    assertEquals("pass123", req.pass());
  }

  @Test
  void createButton_mismatchedPasswords_showsErrorAndDoesNotFire() {
    List<PasswordField> pwFields = lookup(".password-field").<PasswordField>queryAll()
        .stream().toList();

    writeInputTo(pwFields.get(0), "pass123");
    writeInputTo(pwFields.get(1), "different");

    clickOn("Create Account");

    assertTrue(capturedActions.isEmpty(), "No action should be fired on password mismatch");
    Label errorLabel = lookup(".accounts-error").query();
    assertEquals("Passwords do not match", errorLabel.getText());
  }

  @Test
  void createButton_mismatchedPasswords_navNotFired() {
    List<PasswordField> pwFields = lookup(".password-field").<PasswordField>queryAll()
        .stream().toList();
    writeInputTo(pwFields.get(0), "abc");
    writeInputTo(pwFields.get(1), "xyz");
    clickOn("Create Account");
    assertTrue(capturedNavs.isEmpty());
  }

  // ── displayError ─────────────────────────────────────────────────────────

  @Test
  void displayError_showsMessageInErrorLabel() {
    runAsJFXAction(() -> screen.displayError("Username already taken"));
    Label errorLabel = lookup(".accounts-error").query();
    assertEquals("Username already taken", errorLabel.getText());
  }

  // ── onEnter ──────────────────────────────────────────────────────────────

  @Test
  void onEnter_clearsAllTextFields() {
    TextField userField = lookup(".text-field").query();
    List<PasswordField> pwFields = lookup(".password-field").<PasswordField>queryAll()
        .stream().toList();

    writeInputTo(userField, "carol");
    writeInputTo(pwFields.get(0), "pw");
    writeInputTo(pwFields.get(1), "pw");

    runAsJFXAction(() -> screen.onEnter());

    assertEquals("", userField.getText());
    assertEquals("", pwFields.get(0).getText());
    assertEquals("", pwFields.get(1).getText());
  }

  @Test
  void onEnter_clearsErrorLabel() {
    runAsJFXAction(() -> screen.displayError("some error"));
    runAsJFXAction(() -> screen.onEnter());
    Label errorLabel = lookup(".accounts-error").query();
    assertEquals("", errorLabel.getText());
  }
}
