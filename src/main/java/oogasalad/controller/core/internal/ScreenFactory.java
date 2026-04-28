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
import oogasalad.data.api.GameLoader;
import oogasalad.data.internal.LevelAttributes;
import oogasalad.data.api.LevelId;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelLockedException;
import oogasalad.data.internal.GameLoaderImpl;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.data.internal.LevelPaths;
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
import oogasalad.view.authoring.internal.GameAuthoringScreenImpl;
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
  public static final String GAME_AUTHORING_SCREEN_KEY = "GAME_AUTHORING_SCREEN";
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
  private final LevelLoader levelLoader = new LevelLoaderImpl();
  private final GameLoader gameLoader = new GameLoaderImpl();

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

  private Consumer<String> activeErrorDisplay = msg -> {
  };

  // Initial level catalog for progression. Entry screens rescan dynamically so authored levels
  // appear in the UI immediately after being created.
  private final LevelScanner.ScanResult scanResult =
      new LevelScanner(false).scan(Path.of("data", "levels"));
  private final LevelProgression progression =
      new LevelProgression(scanResult.worldsToLevels());

  private final AtomicReference<LevelId> selectedLevel = new AtomicReference<>(defaultLevel());

  // Override slot for paths that don't belong to LevelProgression at all — e.g., a level the
  // authoring screen just saved and wants to play. When non-null, takes precedence over
  // selectedLevel in pathForSelectedLevel(). Cleared whenever the user picks a regular level
  // via the entry screen so the override doesn't outlast the authoring session.
  private final AtomicReference<String> selectedLevelPath = new AtomicReference<>();

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
      selectedLevelPath.set(null);
      return;
    }
    String username = currentUsernameOrNull();
    if (username != null && progression.all().contains(id)) {
      LevelId furthest = furthestReachedForUser(username);
      if (!progression.isUnlocked(id, furthest)) {
        throw new LevelLockedException(id, furthest);
      }
    }
    selectedLevel.set(id);
    selectedLevelPath.set(null);
    saveLastClickedCursor(username, id);
  }

  /**
   * Resolves the currently-selected level to a JSON path. Order of precedence:
   * <ol>
   *   <li>{@link #selectedLevelPath} override (set by the authoring screen for a freshly-saved
   *       level whose path doesn't fit any progression world);</li>
   *   <li>"Others" sandbox: read directly from {@code data/levels/<basename>.json};</li>
   *   <li>Standard worlds: delegate to {@link LevelProgression#pathFor}.</li>
   * </ol>
   */
  private String pathForSelectedLevel() {
    String overridePath = selectedLevelPath.get();
    if (overridePath != null) {
      return overridePath;
    }
    LevelId id = selectedLevel.get();
    if (id == null) {
      return null;
    }
    if (PlayerEntryAppScreen.OTHERS_WORLD_KEY.equals(id.worldKey())) {
      return "data/levels/others/" + id.levelKey() + ".json";
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

  private LevelScanner.ScanResult scanLevels() {
    return new LevelScanner(false).scan(Path.of("data", "levels"));
  }

  private void registerNonAuthScreens() {
    AuthoringScreenImpl authoringScreen = new AuthoringScreenImpl(this::guardedOutcome, selectedLevelPath::set);
    screenManager.registerScreen(MENU_SCREEN_KEY,
        new GameMenuScreen(this::guardedOutcome, authController::handle,
            authService.session()::isLoggedIn));
    screenManager.registerScreen(AUTHORING_ENTRY_SCREEN_KEY,
        new AuthoringEntryScreenImpl(this::guardedOutcome, this::scanLevels,
            worldKey -> configureAuthoringForWorld(authoringScreen, worldKey),
            path -> {
              configureAuthoringForLevel(authoringScreen, path);
              authoringScreen.loadLevelFromPath(path);
            }));
    screenManager.registerScreen(GAME_AUTHORING_SCREEN_KEY,
        new GameAuthoringScreenImpl(this::guardedOutcome, authoringScreen::loadLevelFromPath,
            authoringScreen::setGameConfig));
    screenManager.registerScreen(AUTHORING_SCREEN_KEY, authoringScreen);
    screenManager.registerScreen(PLAYER_ENTRY_APP_SCREEN_KEY,
        new PlayerEntryAppScreen(this::guardedOutcome, this::onLevelSelected,
            this::scanLevels));

    vehicleBuilderScreen = new VehicleBuilderAppScreen(this::guardedOutcome,
        new oogasalad.view.core.internal.SpriteCache(),
        new oogasalad.view.core.internal.PaletteSpriteRegistry(),
        new oogasalad.model.player.internal.DefaultBuildRulesRegistry());
    screenManager.registerScreen(VEHICLE_BUILDER_APP_SCREEN_KEY, vehicleBuilderScreen);

    GameplaySessionFactoryImpl sessionFactory = new GameplaySessionFactoryImpl(
        levelLoader,
        gameLoader,
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

  private void configureAuthoringForWorld(AuthoringScreenImpl authoringScreen, String worldKey) {
    if (worldKey == null || worldKey.isBlank()) {
      authoringScreen.setGameConfig(null);
      return;
    }
    String representativeLevelPath =
        Path.of("data", "levels", worldKey, "level.json").toString();
    try {
      authoringScreen.setGameConfig(
          gameLoader.loadGame(LevelPaths.gameFileForLevel(representativeLevelPath, worldKey)));
    } catch (RuntimeException e) {
      authoringScreen.setGameConfig(null);
    }
  }

  private void configureAuthoringForLevel(AuthoringScreenImpl authoringScreen, String levelPath) {
    try {
      LevelAttributes attrs = levelLoader.loadAttributes(levelPath);
      authoringScreen.setGameConfig(
          gameLoader.loadGame(LevelPaths.gameFileForLevel(levelPath, attrs.getGameType())));
    } catch (RuntimeException e) {
      authoringScreen.setGameConfig(null);
    }
  }

  private void registerAuthScreens() {
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

    screenManager.registerScreen(PROFILE_SCREEN_KEY,
        new ProfileScreen(this::guardedOutcome, intent -> {
          activeErrorDisplay = msg -> {
          };
          authController.handle(intent);
        }, authService.session()::currentUser));

    screenManager.registerScreen(LOGIN_SCREEN_KEY, loginScreen);
    screenManager.registerScreen(CREATE_ACCOUNT_SCREEN_KEY, createAccountScreen);
    screenManager.registerScreen(UPDATE_PROFILE_SCREEN_KEY, updateProfileScreen);
  }

  /**
   * Intercepts BACK_TO_MENU and redirects to the login screen when no player is logged in. Also
   * delegates terminal gameplay outcomes (win/loss) to {@link TerminalResultHandler} so the run
   * gets scored, recorded, and surfaced on the result screen before navigation. Intercepts
   * OPEN_NEXT_LEVEL to advance the cursor before routing — without this, "Continue" on the win
   * screen would replay the level the user just won.
   */
  private void guardedOutcome(Outcome outcome) {
    if (terminalResultHandler != null && gameplayCoordinator != null) {
      terminalResultHandler.handle(
          outcome, selectedLevel.get(), gameplayCoordinator.getLastGameplayScore());
    }
    if (outcome == Outcome.OPEN_NEXT_LEVEL) {
      advanceToNextLevelAndRoute();
      return;
    }
    if (outcome == Outcome.OPEN_VEHICLE_BUILDER) {
      configureVehicleBuilderForSelectedLevel();
    }
    if (outcome == Outcome.SKIP_LOGIN) {
      screenManager.onOutcome(Outcome.BACK_TO_MENU);
      return;
    }
    if (outcome == Outcome.BACK_TO_MENU && !authService.session().isLoggedIn()) {
      screenManager.onOutcome(Outcome.OPEN_LOGIN);
    } else {
      screenManager.onOutcome(outcome);
    }
  }

  private void configureVehicleBuilderForSelectedLevel() {
    if (vehicleBuilderScreen == null) {
      return;
    }
    String levelPath = pathForSelectedLevel();
    if (levelPath == null || levelPath.isBlank()) {
      vehicleBuilderScreen.setAllowedComponentIds(java.util.List.of());
      vehicleBuilderScreen.setGameConfig(null);
      return;
    }
    LevelAttributes attributes = levelLoader.loadAttributes(levelPath);
    try {
      oogasalad.data.api.GameConfig config = gameLoader.loadGame(
          LevelPaths.gameFileForLevel(levelPath, attributes.getGameType()));
      vehicleBuilderScreen.setGameConfig(config);
    } catch (RuntimeException e) {
      vehicleBuilderScreen.setGameConfig(null);
    }
    vehicleBuilderScreen.setAllowedComponentIds(attributes.getElementsAllowed());
  }

  /**
   * Bumps {@link #selectedLevel} to {@code progression.nextAfter(current)} and routes to the
   * vehicle builder so the user can play it. If there is no next level (the user just cleared
   * the last one, or the current selection is unknown), routes to the player entry screen so
   * they can pick something else.
   */
  private void advanceToNextLevelAndRoute() {
    LevelId current = selectedLevel.get();
    java.util.Optional<LevelId> next =
        current != null ? progression.nextAfter(current) : java.util.Optional.empty();
    if (next.isPresent()) {
      selectedLevel.set(next.get());
      configureVehicleBuilderForSelectedLevel();
      screenManager.onOutcome(Outcome.OPEN_VEHICLE_BUILDER);
    } else {
      screenManager.onOutcome(Outcome.OPEN_PLAYER_ENTRY);
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
