package oogasalad.controller.authoring.api;

import javafx.scene.input.MouseEvent;

/**
 * Handles the high-level orchestration of the level editor.
 */
public interface AuthoringController {
  /** Translates mouse clicks into grid placements or removals. */
  void handleGridInteraction(MouseEvent event);

  /** Commands the data layer to save the current progress. */
  void saveLevel(String fileName);

  /** Commands the data layer to load an existing level. */
  void loadLevel(String fileName);
}