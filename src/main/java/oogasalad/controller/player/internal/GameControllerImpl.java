package oogasalad.controller.player.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.GameplayStatsTracker;
import oogasalad.model.player.api.LossConditionChecker;
import oogasalad.model.player.api.WinConditionChecker;
import oogasalad.view.core.api.Camera;
import oogasalad.view.core.api.CameraBehavior;
import oogasalad.view.core.api.Renderer;

/**
 * Drives the gameplay simulation at 60 FPS using a JavaFX Timeline.
 * <p>
 * Each frame: updates the camera position via camera.updateCameraBehavior(), advances physics via GameEngine,
 * redraws via Renderer, then checks win/loss conditions. Reads key bindings from GameConfig and
 * registers them with InputRouter on the first call to startSimulation().
 * <p>
 * When a terminal outcome is reached, fires the callback registered via setOnOutcome().
 * Does not know or reference ScreenManager — navigation is the caller's concern.
 * <p>
 * Assumes GameConfig returns comma-separated key names for dot-path lookups, e.g.
 * getSetting("input_mapping.activate_engine") returns "W,UP".
 */
public class GameControllerImpl implements GameController {

  private static final double FRAME_DURATION_MS = 1000.0 / 60.0;
  private static final double FRAME_DURATION_SECONDS = 1.0 / 60.0;

  private final GameEngine engine;
  private final World world;
  private final Renderer renderer;
  private final Camera camera;
  private final WinConditionChecker winChecker;
  private final LossConditionChecker lossChecker;
  private final InputRouter inputRouter;
  private final GameConfig config;
  private final GameplayStatsTracker tracker;

  /** Maps config action names (e.g. "activate_engine") to their Action instances. */
  private final Map<String, Action> actionRegistry = new HashMap<>();

  private final Timeline gameLoop;

  /**
   * True after the first startSimulation() call; prevents re-registering key bindings on every
   * pause/resume cycle.
   */
  private boolean initialized = false;

  private Consumer<Outcome> onOutcome;
  private BiConsumer<Double, Double> onStatsTick;
  private Consumer<GameplayScore> onTerminalScore;

  /**
   * Constructs the controller and builds the game loop (does not start it).
   *
   * @param engine         advances physics each frame
   * @param world          holds all GameObjects and Joints
   * @param renderer       draws the current world state
   * @param camera         defines the visible viewport
   * @param cameraBehavior updates the camera position each frame
   * @param winChecker     evaluated each frame to detect victory
   * @param lossChecker    evaluated each frame to detect failure
   * @param inputRouter    receives key bindings loaded from config
   * @param config         source of input_mapping and other game settings
   */
  public GameControllerImpl(GameEngine engine, World world, Renderer renderer, Camera camera,
      CameraBehavior cameraBehavior, WinConditionChecker winChecker,
      LossConditionChecker lossChecker, InputRouter inputRouter, GameConfig config,
      GameplayStatsTracker tracker) {
    this.engine = engine;
    this.world = world;
    this.renderer = renderer;
    this.camera = camera;
    this.winChecker = winChecker;
    this.lossChecker = lossChecker;
    this.inputRouter = inputRouter;
    this.config = config;
    this.tracker = tracker;
    this.gameLoop = buildGameLoop();
  }

  @Override
  public void registerAction(String actionName, Action action) {
    actionRegistry.put(actionName, action);
  }

  @Override
  public void handleButtonPress(String buttonId) {
    inputRouter.handleButtonPress(buttonId);
  }

  @Override
  public void setOnOutcome(Consumer<Outcome> callback) {
    this.onOutcome = callback;
  }

  @Override
  public void setOnStatsTick(BiConsumer<Double, Double> callback) {
    this.onStatsTick = callback;
  }

  @Override
  public void setOnTerminalScore(Consumer<GameplayScore> callback) {
    this.onTerminalScore = callback;
  }

  /**
   * Binds keys from GameConfig on first call, then starts or resumes the loop. Safe to call on
   * both initial start and resume-after-pause.
   */
  @Override
  public void startSimulation() {
    if (!initialized) {
      addActionsToInputMap();
      initialized = true;
    }
    gameLoop.play();
  }

  /** Freezes the game loop without resetting it. Call startSimulation() to resume. */
  @Override
  public void pauseSimulation() {
    gameLoop.pause();
  }

  /** Stops the game loop. Does not reset initialization state. */
  @Override
  public void stopSimulation() {
    gameLoop.stop();
  }

  /** Stops the game loop and resets initialization so the next startSimulation() re-binds keys. */
  @Override
  public void resetSimulation() {
    gameLoop.stop();
    initialized = false;
  }

  private Timeline buildGameLoop() {
    KeyFrame frame = new KeyFrame(Duration.millis(FRAME_DURATION_MS), e -> tick());
    Timeline loop = new Timeline(frame);
    loop.setCycleCount(Timeline.INDEFINITE);
    return loop;
  }

  /**
   * One frame of gameplay: update camera, advance physics, redraw, then check win/loss. Called
   * automatically by the Timeline every ~16.67ms.
   */
  private void tick() {
    engine.step(FRAME_DURATION_SECONDS);
    tracker.sample(world, FRAME_DURATION_SECONDS);
    if (onStatsTick != null) {
      onStatsTick.accept(tracker.currentElapsedSeconds(), tracker.currentProgressPercent());
    }
    camera.updateBehavior();
    renderer.render(camera, world.getObjects());
    checkWinLoss();
  }

  private void checkWinLoss() {
    if (winChecker.hasWon(world)) {
      finalizeAndFire(true, Outcome.GAME_WON);
    } else if (lossChecker.hasLost(world)) {
      finalizeAndFire(false, Outcome.GAME_LOST);
    }
  }

  private void finalizeAndFire(boolean won, Outcome outcome) {
    stopSimulation();
    GameplayScore score = tracker.finish(won);
    if (onTerminalScore != null) {
      onTerminalScore.accept(score);
    }
    if (onOutcome != null) {
      onOutcome.accept(outcome);
    }
  }

  /**
   * Reads the input_mapping block from GameConfig and binds each registered Action to its
   * configured KeyCodes via InputRouter. Skips any action name not found in config or any
   * unrecognised key string.
   */
  private void addActionsToInputMap() {
    for (Map.Entry<String, Action> entry : actionRegistry.entrySet()) {
      String actionName = entry.getKey();
      Action action = entry.getValue();
      String keysValue = config.getSetting("input_mapping." + actionName);
      if (keysValue == null || keysValue.isBlank()) {
        continue;
      }
      for (String keyName : keysValue.split(",")) {
        try {
          KeyCode code = KeyCode.valueOf(keyName.trim().toUpperCase());
          inputRouter.bind(code, action);
        } catch (IllegalArgumentException ignored) {
          // unrecognised key name in config — skip silently
        }
      }
    }
  }
}
