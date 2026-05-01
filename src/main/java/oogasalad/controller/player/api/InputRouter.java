package oogasalad.controller.player.api;

import javafx.scene.input.KeyCode;

/**
 * Routes hardware input to abstract Actions.
 *
 * @author Paulina Vvedenskaya
 * @author Iurii Beliaev
 */
public interface InputRouter {
  /** Registers a KeyCode-to-Action binding. */
  void bind(KeyCode key, Action action);

  /**
   * Registers a button ID-to-Action binding for press events.
   */
  void bind(String buttonId, Action action);

  /**
   * Registers a button ID-to-Action binding for release events.
   */
  void bindRelease(String buttonId, Action action);

  /** Looks up the Action bound to key and calls execute(), if any. */
  void handleKeyPress(KeyCode key);

  /** Looks up the Action bound to buttonId and calls execute(), if any. */
  void handleButtonPress(String buttonId);

  /**
   * Looks up the release Action bound to buttonId and calls execute(), if any.
   */
  void handleButtonRelease(String buttonId);
}