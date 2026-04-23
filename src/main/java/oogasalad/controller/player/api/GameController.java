package oogasalad.controller.player.api;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;

/**
 * High-level controller of the game player environment.
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
   * Registers a named Action so it can be bound to keys from GameConfig at startup.
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
   * Forwards a tool button press to the InputRouter to activate/deactivate the bound action.
   */
  void handleButtonPress(String buttonId);
}