package oogasalad.controller.core.internal;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import oogasalad.controller.accounts.api.AuthController;
import oogasalad.controller.accounts.internal.AuthControllerImpl;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.player.internal.GamePlayScreenCoordinator;
import oogasalad.controller.player.internal.GameplaySessionFactoryImpl;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.model.auth.api.AuthFactory;
import oogasalad.model.auth.api.AuthService;
import oogasalad.model.auth.api.PlayerProfile;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.PlayHistoryEntry;
import oogasalad.model.player.api.PlayHistoryRepository;
import oogasalad.model.player.api.ScoreCalculator;
import oogasalad.model.player.internal.ScoreCalculatorImpl;
import oogasalad.model.player.internal.SqlitePlayHistoryRepo;
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

  // Player screens stored as fields so the vehicle layout can be read before gameplay starts
  private VehicleBuilderAppScreen vehicleBuilderScreen;

  // Gameplay + result screens kept as fields so terminal stats can be pushed into Win/Lose screens
  // before navigating to them.
  private GamePlayScreenCoordinator gameplayCoordinator;
  private WinAppScreen winScreen;
  private LoseAppScreen loseScreen;

  // Auth screens stored as fields so lambdas can reference them after construction
  private LoginScreen loginScreen;
  private CreateAccountScreen createAccountScreen;
  private UpdateProfileScreen updateProfileScreen;

  // Routes auth errors to whichever screen triggered the action
  private Consumer<String> activeErrorDisplay = msg -> {
  };

  // LEVEL PATH MAP
  private static final Map<String, String> LEVEL_PATHS = Map.of(
      "forest_first", "data/levels/forest/first.json",
      "desert_first", "data/levels/desert/first.json",
      "moon_first", "data/levels/moon/first.json",
      "glacier_first", "data/levels/glacier/first.json",
      "mars_first", "data/levels/mars/first.json"
  );

  private final AtomicReference<String> selectedLevelPath =
      new AtomicReference<>(LEVEL_PATHS.get("forest_first"));

  public ScreenFactory(ScreenManager screenManager) {
    this.screenManager = screenManager;
    this.authController = new AuthControllerImpl(
        authService,
        this::guardedOutcome,
        this::handleAuthError
    );
  }

  private void onLevelSelected(String worldKey, String levelKey) {
    String mapKey = worldKey + "_" + levelKey;
    String levelPath = LEVEL_PATHS.get(mapKey);
    if (levelPath == null) {
      throw new IllegalArgumentException("No level file mapped for: " + mapKey);
    }
    selectedLevelPath.set(levelPath);
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
        new PlayerEntryAppScreen(this::guardedOutcome, this::onLevelSelected));

    vehicleBuilderScreen = new VehicleBuilderAppScreen(this::guardedOutcome);
    screenManager.registerScreen(VEHICLE_BUILDER_APP_SCREEN_KEY, vehicleBuilderScreen);

    LevelLoader levelLoader = new LevelLoaderImpl();
    GameplaySessionFactoryImpl sessionFactory = new GameplaySessionFactoryImpl(
        levelLoader,
        selectedLevelPath::get
    );

    gameplayCoordinator = new GamePlayScreenCoordinator(this::guardedOutcome, sessionFactory,
        vehicleBuilderScreen::getVehicleLayout);
    screenManager.registerScreen(GAMEPLAY_APP_SCREEN_KEY, gameplayCoordinator);

    winScreen = new WinAppScreen(this::guardedOutcome);
    loseScreen = new LoseAppScreen(this::guardedOutcome);
    screenManager.registerScreen(WIN_APP_SCREEN_KEY, winScreen);
    screenManager.registerScreen(LOSE_APP_SCREEN_KEY, loseScreen);
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
   * pushes terminal gameplay stats into the Win/Lose screens before navigation so they can render
   * the elapsed time and progress of the run that just ended.
   */
  private void guardedOutcome(Outcome outcome) {
    pushGameplayStatsIfTerminal(outcome);
    if (outcome == Outcome.BACK_TO_MENU && !authService.session().isLoggedIn()) {
      screenManager.onOutcome(Outcome.OPEN_LOGIN);
    } else {
      screenManager.onOutcome(outcome);
    }
  }

  private void pushGameplayStatsIfTerminal(Outcome outcome) {
    if (gameplayCoordinator == null
        || (outcome != Outcome.GAME_WON && outcome != Outcome.GAME_LOST)) {
      return;
    }
    GameplayScore runScore = gameplayCoordinator.getLastGameplayScore();
    if (runScore == null) {
      return;
    }
    double currentScore = scoreCalculator.score(runScore);
    double displayProgress = outcome == Outcome.GAME_WON ? 1.0 : runScore.progressPercent();

    String userId = currentUsernameOrNull();
    RecordFlags flags = computeNewRecordFlags(userId, runScore, currentScore, displayProgress);

    if (outcome == Outcome.GAME_WON && winScreen != null) {
      winScreen.setStats(runScore.elapsedSeconds(), currentScore, displayProgress,
          flags.newTime, flags.newScore, flags.newProgress);
    } else if (outcome == Outcome.GAME_LOST && loseScreen != null) {
      loseScreen.setStats(runScore.elapsedSeconds(), currentScore, displayProgress,
          flags.newTime, flags.newScore, flags.newProgress);
    }

    if (userId != null) {
      playHistoryRepo.save(new PlayHistoryEntry(
          null, userId, runScore.levelId(),
          runScore.distanceTraveled(), runScore.elapsedSeconds(), displayProgress,
          currentScore, runScore.won(), Instant.now()));
    }
  }

  private String currentUsernameOrNull() {
    if (!authService.session().isLoggedIn()) {
      return null;
    }
    PlayerProfile user = authService.session().currentUser();
    return user != null ? user.username() : null;
  }

  private RecordFlags computeNewRecordFlags(String userId, GameplayScore runScore,
      double currentScore, double currentProgress) {
    if (userId == null) {
      return RecordFlags.all();
    }
    List<PlayHistoryEntry> priorRunsForLevel = playHistoryRepo.findAllByUser(userId).stream()
        .filter(e -> e.levelId().equals(runScore.levelId()))
        .toList();
    if (priorRunsForLevel.isEmpty()) {
      return RecordFlags.all();
    }
    double bestTime = priorRunsForLevel.stream()
        .mapToDouble(PlayHistoryEntry::elapsedSeconds).min().orElse(Double.POSITIVE_INFINITY);
    double bestScore = priorRunsForLevel.stream()
        .mapToDouble(PlayHistoryEntry::score).max().orElse(Double.NEGATIVE_INFINITY);
    double bestProgress = priorRunsForLevel.stream()
        .mapToDouble(PlayHistoryEntry::progressPercent).max().orElse(Double.NEGATIVE_INFINITY);
    return new RecordFlags(
        runScore.elapsedSeconds() < bestTime,
        currentScore > bestScore,
        currentProgress > bestProgress);
  }

  private record RecordFlags(boolean newTime, boolean newScore, boolean newProgress) {
    static RecordFlags all() {
      return new RecordFlags(true, true, true);
    }
  }

  private void handleAuthError(String message) {
    activeErrorDisplay.accept(message);
  }
}
