package oogasalad.controller.core.api;

import oogasalad.view.core.api.AppScreen;

/**
 * Orchestrates the transition between high-level application workflows.
 */
public interface ScreenManager {
  /**
   * Transitions to a new workflow.
   * @param screenName The key for the target screen.
   * @param payload Optional data (e.g., Level path) to pass to the next mode.
   */
  void switchScreen(String screenName, Object payload);

  /** Registers a screen (e.g., "GAMEPLAY", "EDITOR"). */
  void registerScreen(String name, AppScreen screen);
}
