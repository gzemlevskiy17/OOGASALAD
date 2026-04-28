package oogasalad.controller.player.internal;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.scene.input.KeyCode;
import oogasalad.controller.player.api.GameController;
import oogasalad.controller.player.internal.cheats.CheatInputHandler;
import oogasalad.model.player.api.GameplayScore;

/**
 * Lightweight lifecycle wrapper around a {@link GameController} for a single gameplay run.
 * It exposes semantic session controls used by the screen coordinator.
 *
 * @author Iurii Beliaev
 * @author Emilio Garcia
 */
public class GameplaySession {
  private final GameController controller;
  private final CheatInputHandler cheatInputHandler;

  /**
   * Creates a session bound to a concrete controller.
   *
   * @param controller gameplay controller that drives simulation lifecycle
   */
  public GameplaySession(GameController controller) {
    this(controller, new CheatInputHandler(List.of()));
  }

  /**
   * Creates a session bound to a controller and cheat-key input handler.
   *
   * @param controller gameplay controller that drives simulation lifecycle
   * @param cheatInputHandler receives key press/release state for configured cheat chords
   */
  public GameplaySession(GameController controller, CheatInputHandler cheatInputHandler) {
    this.controller = controller;
    this.cheatInputHandler = cheatInputHandler;
  }

  /** Starts or resumes simulation. */
  public void start() { controller.startSimulation(); }

  /** Pauses simulation without discarding session state. */
  public void pause() {
    cheatInputHandler.reset();
    controller.pauseSimulation();
  }

  /** Stops simulation and resets controller state as defined by the controller implementation. */
  public void reset() {
    cheatInputHandler.reset();
    controller.resetSimulation();
  }

  /** Stops simulation immediately. */
  public void stop()  {
    cheatInputHandler.reset();
    controller.stopSimulation();
  }

  /**
   * Forwards a keyboard press to normal input bindings and cheat chord tracking.
   */
  public void handleKeyPress(KeyCode key) {
    controller.handleKeyPress(key);
    cheatInputHandler.handleKeyPressed(key);
  }

  /**
   * Forwards a keyboard release to cheat chord tracking.
   */
  public void handleKeyRelease(KeyCode key) {
    cheatInputHandler.handleKeyReleased(key);
  }

  /**
   * Forwards a tool button press to the controller's input router.
   */
  public void handleButtonPress(String buttonId) {
    controller.handleButtonPress(buttonId);
  }

  /**
   * Subscribes to per-tick {@code (elapsedSeconds, progressPercent)} updates for HUD display.
   * Pass null to disable.
   */
  public void setOnStatsTick(BiConsumer<Double, Double> callback) {
    controller.setOnStatsTick(callback);
  }

  /**
   * Subscribes to the one-shot terminal score produced when the run ends. Fires before the
   * outcome callback so consumers can persist or compare the score before navigation.
   */
  public void setOnTerminalScore(Consumer<GameplayScore> callback) {
    controller.setOnTerminalScore(callback);
  }
}
