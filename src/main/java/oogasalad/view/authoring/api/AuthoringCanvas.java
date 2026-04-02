package oogasalad.view.authoring.api;

import java.util.Collection;
import javafx.scene.Node;

/** Contract for the visual grid where blocks and game elements are placed. */
public interface AuthoringCanvas {
  /** @return the visual root of the grid to be embedded in the editor UI. */
  Node getVisualRoot();

  /** @return the current occupied cells represented as placed objects. */
  Collection<PlacedObject> getPlacedObjects();

  /** Refreshes the visual state of the grid to reflect the current model. */
  void refreshGrid();

  /** Swaps the factory used to create placed objects (called by the controller). */
  void setPlacedObjectFactory(PlacedObjectFactory factory);

  /**
   * Places a block at the given cell, spanning multiple cells based on the current factory.
   */
  void placeBlock(int col, int row);
}
