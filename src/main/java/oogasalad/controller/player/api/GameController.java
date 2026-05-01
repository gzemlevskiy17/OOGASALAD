package oogasalad.controller.player.api;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.scene.input.KeyCode;
import oogasalad.controller.core.api.Outcome;
import oogasalad.model.player.api.GameplayScore;

/**
 * High-level controller of the game player environment.
 *
 * @author Paulina Vvedenskaya
 */
public interface GameController {
  /** Starts the 60-FPS loop. On the first call, binds keys from config. */
  void startSimulation();

  /** Freezes the game loop without resetting it. Call startSimulation() to resume. */
  void pauseSimulation();

  /** Stops the game loop. Does not reset initialization state. */
  void stopSimulation();

  /** Stops the game loop and resets initialization so the next startSimulation() re-binds keys. */
  void resetSimulation();

  /**
   * Registers a named Action so it can be bound to keys from Settings at startup.
   * The actionName must match a key in the input_mapping block of game_config.json.
   */
  void registerAction(String actionName, Action action);

  /** Registers a callback to be fired when the game reaches a terminal outcome. */
  void setOnOutcome(Consumer<Outcome> callback);

  /**
   * Registers a callback fired each tick with {@code (elapsedSeconds, progressPercent)} so a
   * HUD can display live run stats. {@code progressPercent} is in {@code [0, 1]}. Pass null to
   * disable.
   */
  void setOnStatsTick(BiConsumer<Double, Double> callback);

  /**
   * Registers a callback fired once per run immediately after the tracker finalizes, carrying the
   * immutable {@link GameplayScore} snapshot. Fires before the terminal outcome callback so
   * listeners can inspect or persist the run before navigation. Pass null to disable.
   */
  void setOnTerminalScore(Consumer<GameplayScore> callback);

  /**
   * Forwards a tool button press to the InputRouter to activate the bound action.
   */
  void handleButtonPress(String buttonId);

  /**
   * Forwards a tool button release to the InputRouter to deactivate the bound action.
   */
  void handleButtonRelease(String buttonId);

  /**
   * Forwards a keyboard press to the InputRouter.
   */
  void handleKeyPress(KeyCode key);

  /**
   * Completes the current run through the normal terminal scoring and outcome path.
   */
  void completeRun(Outcome outcome);
}
