package oogasalad.controller.core.internal;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import oogasalad.controller.accounts.api.AuthController;
import oogasalad.controller.accounts.internal.AuthControllerImpl;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.player.internal.GamePlayScreenCoordinator;
import oogasalad.controller.player.internal.GameplaySessionFactoryImpl;
import oogasalad.controller.player.internal.TerminalResultHandler;
import oogasalad.data.api.LevelId;
import oogasalad.data.api.LevelLockedException;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.data.internal.LevelProgression;
import oogasalad.data.internal.LevelScanner;
import oogasalad.model.auth.api.AuthFactory;
import oogasalad.model.auth.api.AuthService;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.model.player.api.PlayHistoryRepository;
import oogasalad.model.player.api.ScoreCalculator;
import oogasalad.model.player.api.UserProgression;
import oogasalad.model.player.api.UserProgressionRepository;
import oogasalad.model.player.internal.ScoreCalculatorImpl;
import oogasalad.model.player.internal.SqlitePlayHistoryRepo;
import oogasalad.model.player.internal.SqliteUserProgressionRepo;
import oogasalad.view.accounts.internal.CreateAccountScreen;
import oogasalad.view.accounts.internal.LoginScreen;
import oogasalad.view.accounts.internal.ProfileScreen;
import oogasalad.view.accounts.internal.UpdateProfileScreen;
import oogasalad.view.authoring.internal.AuthoringEntryScreenImpl;
import oogasalad.view.authoring.internal.AuthoringScreenImpl;
import oogasalad.view.core.internal.GameMenuScreen;
import oogasalad.view.player.internal.LoseAppScreen;
import oogasalad.view.player.internal.PlayerEntryAppScreen;
import oogasalad.view.player.internal.VehicleBuilderAppScreen;
import oogasalad.view.player.internal.WinAppScreen;

/**
 * Creates and registers all application screens with the ScreenManager.
 *
 * @author Paulina Vvedenskaya
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
  private final PlayHistoryRepository playHistoryRepo = new SqlitePlayHistoryRepo();
  private final ScoreCalculator scoreCalculator = new ScoreCalculatorImpl();
  private final UserProgressionRepository userProgressionRepo = new SqliteUserProgressionRepo();

  // Player screens stored as fields so the vehicle layout can be read before gameplay starts
  private VehicleBuilderAppScreen vehicleBuilderScreen;

  // Gameplay + result screens kept as fields so terminal stats can be pushed into Win/Lose screens
  // before navigating to them.
  private GamePlayScreenCoordinator gameplayCoordinator;
  private WinAppScreen winScreen;
  private LoseAppScreen loseScreen;
  private TerminalResultHandler terminalResultHandler;

  // Auth screens stored as fields so lambdas can reference them after construction
  private LoginScreen loginScreen;
  private CreateAccountScreen createAccountScreen;
  private UpdateProfileScreen updateProfileScreen;

  // Routes auth errors to whichever screen triggered the action
  private Consumer<String> activeErrorDisplay = msg -> {
  };

  // Level catalog: scan the filesystem once, feed the result into LevelProgression so
  // ordering/unlock logic can query it. The "others" list (root-level JSONs that don't fit
  // the world/level shape) is passed to PlayerEntryAppScreen for its sandbox bucket. Selected
  // level is held as a LevelId; the path is computed only at the moment the gameplay session
  // asks for it.
  private final LevelScanner.ScanResult scanResult =
      new LevelScanner().scan(Path.of("data", "levels"));
  private final LevelProgression progression =
      new LevelProgression(scanResult.worldsToLevels());

  private final AtomicReference<LevelId> selectedLevel = new AtomicReference<>(defaultLevel());

  private LevelId defaultLevel() {
    return progression.all().isEmpty() ? null : progression.all().get(0);
  }

  public ScreenFactory(ScreenManager screenManager) {
    this.screenManager = screenManager;
    this.authController = new AuthControllerImpl(
        authService,
        this::guardedOutcome,
        this::handleAuthError
    );
  }

  private void onLevelSelected(String worldKey, String levelKey) {
    LevelId id = new LevelId(worldKey, levelKey);
    if (PlayerEntryAppScreen.OTHERS_WORLD_KEY.equals(worldKey)) {
      // "Others" levels are sandboxed — they live outside progression rules. Skip the
      // unlock check and the cursor save; just remember the selection so the path supplier
      // can resolve it to data/levels/<basename>.json at session creation time.
      selectedLevel.set(id);
      return;
    }
    if (!progression.all().contains(id)) {
      // TODO: PlayerEntryAppScreen now greys out levels missing from disk, so reaching this
      // throw should be impossible from the UI. The entry screen's try/catch only catches
      // LevelLockedException, so an IllegalArgumentException here would bubble up unhandled.
      // If we ever expose a way to enter a level outside the entry screen (URL handler, restart,
      // etc.), wrap this in a user-facing exception or extend the entry screen's catch.
      throw new IllegalArgumentException("Unknown level: " + id);
    }
    String username = currentUsernameOrNull();
    if (username != null) {
      LevelId furthest = furthestReachedForUser(username);
      if (!progression.isUnlocked(id, furthest)) {
        throw new LevelLockedException(id, furthest);
      }
    }
    selectedLevel.set(id);
    saveLastClickedCursor(username, id);
  }

  /**
   * Resolves the currently-selected level to a JSON path. Standard worlds delegate to
   * {@link LevelProgression#pathFor}; the "Others" sandbox bypasses the world subdirectory and
   * reads files directly from the levels root.
   */
  private String pathForSelectedLevel() {
    LevelId id = selectedLevel.get();
    if (id == null) {
      return null;
    }
    if (PlayerEntryAppScreen.OTHERS_WORLD_KEY.equals(id.worldKey())) {
      return "data/levels/" + id.levelKey() + ".json";
    }
    return progression.pathFor(id);
  }

  private void saveLastClickedCursor(String username, LevelId clicked) {
    if (username == null) {
      return;
    }
    userProgressionRepo.saveLastClicked(username, clicked);
  }

  private LevelId furthestReachedForUser(String username) {
    return userProgressionRepo.findByUser(username)
        .map(UserProgression::furthestReached)
        .orElse(null);
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
        new PlayerEntryAppScreen(this::guardedOutcome, this::onLevelSelected, progression,
            scanResult.others()));

    vehicleBuilderScreen = new VehicleBuilderAppScreen(this::guardedOutcome);
    screenManager.registerScreen(VEHICLE_BUILDER_APP_SCREEN_KEY, vehicleBuilderScreen);

    LevelLoader levelLoader = new LevelLoaderImpl();
    GameplaySessionFactoryImpl sessionFactory = new GameplaySessionFactoryImpl(
        levelLoader,
        this::pathForSelectedLevel
    );

    gameplayCoordinator = new GamePlayScreenCoordinator(this::guardedOutcome, sessionFactory,
        vehicleBuilderScreen::getVehicleLayout);
    screenManager.registerScreen(GAMEPLAY_APP_SCREEN_KEY, gameplayCoordinator);

    winScreen = new WinAppScreen(this::guardedOutcome);
    loseScreen = new LoseAppScreen(this::guardedOutcome);
    screenManager.registerScreen(WIN_APP_SCREEN_KEY, winScreen);
    screenManager.registerScreen(LOSE_APP_SCREEN_KEY, loseScreen);

    terminalResultHandler = new TerminalResultHandler(
        scoreCalculator, playHistoryRepo, userProgressionRepo, progression,
        this::currentUsernameOrNull,
        winScreen::setStats, loseScreen::setStats);
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
   * Intercepts BACK_TO_MENU and redirects to the login screen when no player is logged in. Also
   * delegates terminal gameplay outcomes (win/loss) to {@link TerminalResultHandler} so the run
   * gets scored, recorded, and surfaced on the result screen before navigation.
   */
  private void guardedOutcome(Outcome outcome) {
    if (terminalResultHandler != null && gameplayCoordinator != null) {
      terminalResultHandler.handle(
          outcome, selectedLevel.get(), gameplayCoordinator.getLastGameplayScore());
    }
    if (outcome == Outcome.BACK_TO_MENU && !authService.session().isLoggedIn()) {
      screenManager.onOutcome(Outcome.OPEN_LOGIN);
    } else {
      screenManager.onOutcome(outcome);
    }
  }

  private String currentUsernameOrNull() {
    if (!authService.session().isLoggedIn()) {
      return null;
    }
    PlayerProfile user = authService.session().currentUser();
    return user != null ? user.username() : null;
  }

  private void handleAuthError(String message) {
    activeErrorDisplay.accept(message);
  }
}
