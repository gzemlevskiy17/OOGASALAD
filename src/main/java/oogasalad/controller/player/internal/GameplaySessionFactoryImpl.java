package oogasalad.controller.player.internal;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.api.GameplaySessionFactory;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.controller.player.internal.cheats.CheatInputHandler;
import oogasalad.controller.player.internal.cheats.CheatKeyBinder;
import oogasalad.controller.player.internal.cheats.CheatRuntimeContext;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.internal.LevelLoaderImpl;
import oogasalad.model.engine.CollisionDetectorImpl;
import oogasalad.model.engine.GameEngineImpl;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.VehicleComponent;
import oogasalad.model.player.api.GameplayStatsTracker;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.ScoreService;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.model.player.api.WinConditionChecker;
import oogasalad.model.player.internal.FallOffWorldLossChecker;
import oogasalad.model.player.internal.FinishLineWinChecker;
import oogasalad.model.player.internal.GameplayStatsTrackerImpl;
import oogasalad.model.player.internal.ScoreServiceImpl;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.Renderer;
import oogasalad.view.core.internal.CameraImpl;
import oogasalad.view.core.internal.FollowPlayerCameraBehavior;
import oogasalad.view.core.internal.RendererImpl;
import oogasalad.view.player.internal.GamePlayAppScreen;

/**
 * Default factory that assembles the full gameplay runtime graph (world, engine, camera,
 * renderer, input, win/loss checks, and controller) for one playable session.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */
public class GameplaySessionFactoryImpl implements GameplaySessionFactory {

  private static final double INITIAL_CAMERA_X = 0.0;
  private static final double INITIAL_CAMERA_Y = 0.0;
  private static final double CAMERA_OFFSET_X = 0.0;
  private static final double CAMERA_OFFSET_Y = 0.0;
  private static final double CAMERA_FOLLOW_SMOOTHING = 0.2;

  private final GameplayWorldBuilder worldBuilder;
  private final LevelLoader levelLoader;
  private final Supplier<String> levelPathSupplier;
  private final CameraTargetResolver cameraTargetResolver = new CameraTargetResolver();
  private final DriveActionBinder driveActionBinder = new DriveActionBinder();
  private final ButtonForceController buttonForceController = new ButtonForceController();
  private final VehicleHudAttacher vehicleHudAttacher = new VehicleHudAttacher();
  private final GameplayCameraViewportController cameraViewportController =
      new GameplayCameraViewportController();

  public GameplaySessionFactoryImpl(
      LevelLoader levelLoader,
      GameLoader gameLoader,
      Supplier<String> levelPathSupplier
  ) {
    this.levelLoader = levelLoader;
    this.levelPathSupplier = levelPathSupplier;
    this.worldBuilder = new GameplayWorldBuilder(levelLoader, gameLoader, levelPathSupplier);
  }

  static Map<Renderable, Renderable> wheelConnectorRotationSources(World world) {
    Map<Renderable, Renderable> sources = new IdentityHashMap<>();
    for (Joint joint : world.getJoints()) {
      GameComponent a = joint.getComponentA();
      GameComponent b = joint.getComponentB();
      if (isWheel(a) && !isWheel(b)) {
        sources.put(a, b);
      } else if (isWheel(b) && !isWheel(a)) {
        sources.put(b, a);
      }
    }
    return sources;
  }

  private static boolean isWheel(GameComponent component) {
    return component instanceof VehicleComponent vehicleComponent && vehicleComponent.isWheel();
  }

  @Override
  public GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome,
      VehicleLayout layout) {
    screen.getGameLayer().getChildren().clear();

    GameplayWorldBuilder.BuiltWorld built = worldBuilder.build(layout);
    World world = built.world();
    InputRouter inputRouter = buildInputRouter();
    configureInputControls(screen, inputRouter, world, built.gameConfig());
    showForceWarningsIfNeeded(screen, built);
    GameController controller = buildController(screen, built, inputRouter);
    controller.setOnOutcome(onOutcome);

    ScoreService scoreService = scoreService();
    CheatInputHandler cheatInputHandler = buildCheatInputHandler(
        built.gameConfig(), world, controller, scoreService);
    vehicleHudAttacher.attach(screen, built.placements());

    return new GameplaySession(controller, cheatInputHandler, scoreService);
  }

  private GameController buildController(GamePlayAppScreen screen,
      GameplayWorldBuilder.BuiltWorld built, InputRouter inputRouter) {
    World world = built.world();
    GameConfig gameConfig = built.gameConfig();
    GameplayConfigSettings gameplayConfig = new GameplayConfigSettings(gameConfig);
    GameEngine engine = new GameEngineImpl(world, new CollisionDetectorImpl());
    WinConditionChecker winChecker = buildWinChecker(gameConfig);
    LossConditionChecker lossChecker = buildLossChecker(gameplayConfig, built.bounds().height());
    CameraRuntime cameraRuntime = buildCameraRuntime(screen, world, built);
    Renderer renderer = buildRenderer(screen, gameConfig, world);
    GameplayStatsTracker tracker = new GameplayStatsTrackerImpl(
        levelPathSupplier.get(), mainCharacterId(gameConfig));

    GameController controller = new GameControllerImpl(
        engine, world, renderer, cameraRuntime.camera(), cameraRuntime.behavior(),
        winChecker, lossChecker, inputRouter, gameplayConfig.controllerSettings(), tracker);
    return controller;
  }

  private CheatInputHandler buildCheatInputHandler(GameConfig gameConfig, World world,
      GameController controller, ScoreService scoreService) {
    if (gameConfig == null || gameConfig.cheats().isEmpty()) {
      return new CheatInputHandler(List.of());
    }
    return CheatKeyBinder.defaults().bind(gameConfig,
        new CheatRuntimeContext(world, gameConfig, mainCharacterId(gameConfig),
            controller::completeRun, scoreService));
  }

  private ScoreService scoreService() {
    if (levelLoader instanceof LevelLoaderImpl loaderImpl) {
      return loaderImpl.getScore();
    }
    return new ScoreServiceImpl();
  }

  private CameraRuntime buildCameraRuntime(GamePlayAppScreen screen, World world,
      GameplayWorldBuilder.BuiltWorld built) {
    Camera camera = buildCamera(screen);
    LevelAttributes levelAttributes = built.levelAttributes();
    GameObject player = cameraTargetResolver.resolve(world, levelAttributes.getCameraFollows());
    CameraBehavior behavior = buildCameraBehavior(player, built.bounds());
    camera.setBehavior(behavior);
    cameraViewportController.wireResponsiveCamera(screen, camera);
    return new CameraRuntime(camera, behavior);
  }

  private void configureInputControls(GamePlayAppScreen screen, InputRouter inputRouter,
      World world, GameConfig gameConfig) {
    driveActionBinder.bind(inputRouter, world);
    buttonForceController.bind(inputRouter, gameConfig, world);
    Set<String> toolIds = new LinkedHashSet<>();
    toolIds.addAll(driveActionBinder.collectToolIds(world));
    toolIds.addAll(buttonForceController.collectButtonIds(gameConfig, world));
    screen.setAvailableTools(toolIds);
    screen.setOnToolActivated(toolId -> inputRouter.handleButtonPress(toolId));
    screen.setOnToolDeactivated(toolId -> inputRouter.handleButtonRelease(toolId));
  }

  private void showForceWarningsIfNeeded(GamePlayAppScreen screen,
      GameplayWorldBuilder.BuiltWorld built) {
    List<String> warnings = ForceViabilityChecker.check(
        built.gameConfig(), built.world().getPhysicsEnvironment().gravity());
    if (warnings.isEmpty()) {
      return;
    }
    VBox warningBox = new VBox(4);
    warningBox.setStyle("-fx-background-color: rgba(180,40,40,0.85); -fx-padding: 10; "
        + "-fx-background-radius: 8;");
    warningBox.setMaxWidth(500);
    warningBox.setMouseTransparent(true);
    Label header = new Label("Warning: button forces may be too weak to move the vehicle");
    header.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
    warningBox.getChildren().add(header);
    for (String warning : warnings) {
      Label line = new Label(warning);
      line.setStyle("-fx-text-fill: white; -fx-font-size: 11;");
      line.setWrapText(true);
      warningBox.getChildren().add(line);
    }
    VBox.setMargin(header, new Insets(0, 0, 4, 0));
    screen.attachOverlay(warningBox, Pos.TOP_CENTER);
  }

  private Camera buildCamera(GamePlayAppScreen screen) {
    return new CameraImpl(INITIAL_CAMERA_X, INITIAL_CAMERA_Y,
        cameraViewportController.initialViewWidth(screen),
        cameraViewportController.initialViewHeight(screen));
  }

  private CameraBehavior buildCameraBehavior(GameObject player,
      GameplayWorldBuilder.LevelBounds bounds) {
    return new FollowPlayerCameraBehavior(() -> player, CAMERA_OFFSET_X, CAMERA_OFFSET_Y,
        CAMERA_FOLLOW_SMOOTHING, bounds.width(), bounds.height());
  }

  private Renderer buildRenderer(GamePlayAppScreen screen, GameConfig gameConfig, World world) {
    GameplayRenderableFactory renderableFactory = new GameplayRenderableFactory(
        gameConfig, wheelConnectorRotationSources(world));
    Renderer objectRenderer = new RendererImpl(screen.getGameLayer(),
        renderable -> renderableFactory.createNode(renderable),
        (renderable, node) -> renderableFactory.updateNode(renderable, node));
    return (cameraRef, objects) -> objectRenderer.render(cameraRef, objects);
  }

  private WinConditionChecker buildWinChecker(GameConfig config) {
    return new FinishLineWinChecker(mainCharacterId(config));
  }

  private LossConditionChecker buildLossChecker(GameplayConfigSettings settings,
      double worldHeight) {
    List<String> tracked = settings.lossTrackedTags();
    String mainCharacterId = mainCharacterId(settings.gameConfig());
    if (tracked.isEmpty() && mainCharacterId != null && !mainCharacterId.isBlank()) {
      tracked = List.of(mainCharacterId);
    }
    return new FallOffWorldLossChecker(worldHeight, tracked);
  }

  private String mainCharacterId(GameConfig config) {
    if (config == null) {
      return null;
    }
    return config.mainCharacterId();
  }

  private InputRouterImpl buildInputRouter() {
    return new InputRouterImpl();
  }

  private record CameraRuntime(Camera camera, CameraBehavior behavior) {
  }

}
