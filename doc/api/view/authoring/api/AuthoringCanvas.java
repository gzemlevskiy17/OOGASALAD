package oogasalad.view.authoring.api;

import javafx.scene.Node;

/** Contract for the visual grid where blocks and game elements are placed. */
public interface AuthoringCanvas {
  /** @return the visual root of the grid to be embedded in the editor UI. */
  Node getVisualRoot();

  /** Refreshes the visual state of the grid to reflect the current model. */
  void refreshGrid();
}