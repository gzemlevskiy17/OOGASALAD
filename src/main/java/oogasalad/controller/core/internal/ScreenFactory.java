package oogasalad.controller.core.internal;

import java.util.Map;
import java.util.function.Consumer;
import oogasalad.controller.accounts.api.AuthController;
import oogasalad.controller.accounts.internal.AuthControllerImpl;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.player.internal.GamePlayScreenCoordinator;
import oogasalad.controller.player.internal.GameplaySessionFactoryImpl;
import oogasalad.model.auth.api.AuthFactory;
import oogasalad.model.auth.api.AuthService;
import oogasalad.view.accounts.internal.CreateAccountScreen;
import oogasalad.view.accounts.internal.LoginScreen;
import oogasalad.view.accounts.internal.ProfileScreen;
import oogasalad.view.accounts.internal.UpdateProfileScreen;
import oogasalad.view.authoring.internal.AuthoringEntryScreenImpl;
import oogasalad.view.authoring.internal.AuthoringScreenImpl;
import oogasalad.view.core.api.GameMenuScreen;
import oogasalad.view.player.internal.LoseAppScreen;
import oogasalad.view.player.internal.PlayerEntryAppScreen;
import oogasalad.view.player.internal.VehicleBuilderAppScreen;
import oogasalad.view.player.internal.WinAppScreen;

/**
 * Creates and registers all application screens with the ScreenManager.
 */
public class ScreenFactory {

  public static final String MENU_SCREEN_KEY = "MENU";
  public static final String AUTHORING_ENTRY_SCREEN_KEY = "AUTHORING_ENTRY";
  public static final String PLAYER_ENTRY_APP_SCREEN_KEY = "PLAYER_ENTRY";
  public static final String AUTHORING_SCREEN_KEY = "AUTHORING_SCREEN";
  public static final String VEHICLE_BUILDER_APP_SCREEN_KEY = "VEHICLE_BUILDER_SCREEN";
  public static final String GAMEPLAY_APP_SCREEN_KEY = "GAMEPLAY_SCREEN";
  public static final String WIN_APP_SCREEN_KEY = "WIN_SCREEN";
  public static final String LOSE_APP_SCREEN_KEY = "LOSE_SCREEN";
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

  // LEVEL PATH MAP
  private static final Map<String, String> LEVEL_PATHS = Map.of(
      "forest_first", "oogasalad_team03/data/forest/first.json",
      "desert_first", "oogasalad_team03/data/desert/first.json",
      "moon_first", "oogasalad_team03/data/moon/first.json",
      "glacier_first", "oogasalad_team03/data/glacier/first.json",
      "mars_first", "oogasalad_team03/data/mars/first.json"
  );

  public ScreenFactory(ScreenManager screenManager) {
    this.screenManager = screenManager;
    this.authController = new AuthControllerImpl(
        authService,
        this::guardedOutcome,
        this::handleAuthError
    );
  }

  /**
   * Creates all application screens and registers them with the ScreenManager.
   */
  public void registerScreens() {
    registerNonAuthScreens();
    registerAuthScreens();
  }

  private void registerNonAuthScreens() {
    screenManager.registerScreen(MENU_SCREEN_KEY,
        new GameMenuScreen(this::guardedOutcome, authService.session()::isLoggedIn));
    screenManager.registerScreen(AUTHORING_ENTRY_SCREEN_KEY,
        new AuthoringEntryScreenImpl(this::guardedOutcome));
    screenManager.registerScreen(AUTHORING_SCREEN_KEY,
        new AuthoringScreenImpl(this::guardedOutcome));
    screenManager.registerScreen(PLAYER_ENTRY_APP_SCREEN_KEY,
        new PlayerEntryAppScreen(this::guardedOutcome));
    screenManager.registerScreen(VEHICLE_BUILDER_APP_SCREEN_KEY,
        new VehicleBuilderAppScreen(this::guardedOutcome));
    screenManager.registerScreen(GAMEPLAY_APP_SCREEN_KEY,
        new GamePlayScreenCoordinator(this::guardedOutcome, new GameplaySessionFactoryImpl()));
    screenManager.registerScreen(WIN_APP_SCREEN_KEY, new WinAppScreen(this::guardedOutcome));
    screenManager.registerScreen(LOSE_APP_SCREEN_KEY, new LoseAppScreen(this::guardedOutcome));
  }

  private void registerAuthScreens() {
    // Each screen binds its own error display before delegating to the controller,
    // so handleAuthError always reaches the screen that triggered the action.
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
    screenManager.registerScreen(PROFILE_SCREEN_KEY,
        new ProfileScreen(this::guardedOutcome, intent -> {
          activeErrorDisplay = msg -> {
          }; // profile screen has no error label; log only
          authController.handle(intent);
        }, authService.session()::currentUser));

    screenManager.registerScreen(LOGIN_SCREEN_KEY, loginScreen);
    screenManager.registerScreen(CREATE_ACCOUNT_SCREEN_KEY, createAccountScreen);
    screenManager.registerScreen(UPDATE_PROFILE_SCREEN_KEY, updateProfileScreen);
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
