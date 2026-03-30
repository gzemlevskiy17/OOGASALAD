package oogasalad.controller.player.api;

import javafx.scene.input.KeyCode;

/** Routes hardware input to abstract Actions. */
public interface InputRouter {
  void bind(KeyCode key, Action action);
  void bind(String buttonId, Action action);
  void handleButtonPress(String buttonId);
}