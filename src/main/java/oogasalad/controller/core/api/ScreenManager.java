package oogasalad.controller.core.api;

import oogasalad.view.core.api.AppScreen;

/**
 * Orchestrates the transition between high-level application workflows.
 */
public interface ScreenManager {
  /**
   * Transitions to a new workflow.
   * @param screenName The key for the target screen.
   */
  void switchScreen(String screenName);

  /** Registers a screen (e.g., "GAMEPLAY", "EDITOR"). */
  void registerScreen(String name, AppScreen screen);

  /**
   * Handles a terminal outcome from a controller and transitions to the appropriate screen.
   * This is the single place where outcomes are mapped to screen names.
   */
  void onOutcome(Outcome outcome);
}
