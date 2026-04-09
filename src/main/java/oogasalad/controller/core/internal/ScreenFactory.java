package oogasalad.controller.core.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import oogasalad.controller.accounts.api.AuthController;
import oogasalad.controller.accounts.internal.AuthControllerImpl;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.model.auth.api.AuthFactory;
import oogasalad.model.auth.api.AuthService;
import oogasalad.view.accounts.internal.CreateAccountScreen;
import oogasalad.view.accounts.internal.LoginScreen;
import oogasalad.view.accounts.internal.ProfileScreen;
import oogasalad.view.accounts.internal.UpdateProfileScreen;
import oogasalad.view.authoring.internal.AuthoringEntryScreenImpl;
import oogasalad.view.authoring.internal.AuthoringScreenImpl;
import oogasalad.view.core.api.AppScreen;
import oogasalad.view.core.api.GameMenuScreen;
import oogasalad.view.player.internal.GamePlayAppScreen;
import oogasalad.view.player.internal.PlayerEntryAppScreen;
import oogasalad.view.player.internal.VehicleBuilderAppScreen;

/**
 * Creates and collects application screens for registration with the ScreenManager.
 */
public class ScreenFactory {

  public static final String MENU_SCREEN_KEY = "MENU";
  public static final String AUTHORING_ENTRY_SCREEN_KEY = "AUTHORING_ENTRY";
  public static final String PLAYER_ENTRY_APP_SCREEN_KEY = "PLAYER_ENTRY";
  public static final String AUTHORING_SCREEN_KEY = "AUTHORING_SCREEN";
  public static final String VEHICLE_BUILDER_APP_SCREEN_KEY = "VEHICLE_BUILDER_SCREEN";
  public static final String GAMEPLAY_APP_SCREEN_KEY = "GAMEPLAY_SCREEN";
  public static final String LOGIN_SCREEN_KEY = "LOGIN";
  public static final String CREATE_ACCOUNT_SCREEN_KEY = "CREATE_ACCOUNT";
  public static final String PROFILE_SCREEN_KEY = "PROFILE";
  public static final String UPDATE_PROFILE_SCREEN_KEY = "UPDATE_PROFILE";

  private final ScreenManager screenManager;
  private final AuthService authService = AuthFactory.createService();
  private final AuthController authController;

  // Auth screens stored as fields so lambdas can reference them after construction
  private LoginScreen loginScreen;
  private CreateAccountScreen createAccountScreen;
  private UpdateProfileScreen updateProfileScreen;

  // Routes auth errors to whichever screen triggered the action
  private Consumer<String> activeErrorDisplay = msg -> {
  };

  public ScreenFactory(ScreenManager screenManager) {
    this.screenManager = screenManager;
    this.authController = new AuthControllerImpl(
        authService,
        this::guardedOutcome,
        this::handleAuthError
    );
  }

  /**
   *
   */
  public Map<String, AppScreen> createScreens() {
    Map<String, AppScreen> screens = new HashMap<>();

    // Non-auth screens — menu gets a live isLoggedIn supplier, not the service itself
    screens.put(MENU_SCREEN_KEY,
        new GameMenuScreen(this::guardedOutcome, authService.session()::isLoggedIn));
    screens.put(AUTHORING_ENTRY_SCREEN_KEY, new AuthoringEntryScreenImpl(this::guardedOutcome));
    screens.put(AUTHORING_SCREEN_KEY, new AuthoringScreenImpl(this::guardedOutcome));
    screens.put(PLAYER_ENTRY_APP_SCREEN_KEY, new PlayerEntryAppScreen(this::guardedOutcome));
    screens.put(VEHICLE_BUILDER_APP_SCREEN_KEY, new VehicleBuilderAppScreen(this::guardedOutcome));
    screens.put(GAMEPLAY_APP_SCREEN_KEY, new GamePlayAppScreen(this::guardedOutcome));

    // Auth screens — each wraps the action consumer to bind its own error display before
    // delegating to the controller, so handleAuthError reaches the right screen.
    loginScreen = new LoginScreen(this::guardedOutcome, intent -> {
      activeErrorDisplay = loginScreen::displayError;
      authController.handle(intent);
    });
    createAccountScreen = new CreateAccountScreen(this::guardedOutcome, intent -> {
      activeErrorDisplay = createAccountScreen::displayError;
      authController.handle(intent);
    });
    updateProfileScreen = new UpdateProfileScreen(this::guardedOutcome, intent -> {
      activeErrorDisplay = updateProfileScreen::displayError;
      authController.handle(intent);
    });

    // ProfileScreen gets a live supplier for the current user — no direct model access in view
    screens.put(PROFILE_SCREEN_KEY,
        new ProfileScreen(this::guardedOutcome, intent -> {
          activeErrorDisplay = msg -> {
          }; // profile screen has no error label; log only
          authController.handle(intent);
        }, authService.session()::currentUser));

    screens.put(LOGIN_SCREEN_KEY, loginScreen);
    screens.put(CREATE_ACCOUNT_SCREEN_KEY, createAccountScreen);
    screens.put(UPDATE_PROFILE_SCREEN_KEY, updateProfileScreen);

    return screens;
  }

  /**
   * Intercepts BACK_TO_MENU and redirects to the login screen when no player is logged in. All
   * other outcomes pass through unchanged.
   */
  private void guardedOutcome(Outcome outcome) {
    if (outcome == Outcome.BACK_TO_MENU && !authService.session().isLoggedIn()) {
      screenManager.onOutcome(Outcome.OPEN_LOGIN);
    } else {
      screenManager.onOutcome(outcome);
    }
  }

  private void handleAuthError(String message) {
    activeErrorDisplay.accept(message);
  }
}
