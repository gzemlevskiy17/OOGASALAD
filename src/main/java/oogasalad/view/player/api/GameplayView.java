package oogasalad.view.player.api;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

/**
 * View contract consumed by gameplay coordination and controllers. Provides the layered scene graph
 * for the active level and bindings for HUD-driven pause and reset actions.
 *
 * @author Iurii Beliaev
 */
public interface GameplayView {

  /** Root Parent of the gameplay screen, suitable as a Scene root or navigator child. */
  Parent getRoot();

  /** Layer where the simulated world draws; typically behind HUD overlays within the root stack. */
  Pane getGameLayer();

  /** Invoked when the user requests pause through the gameplay chrome. */
  void setOnPause(Runnable onPause);

  /** Invoked when the user requests a full simulation reset through the gameplay chrome. */
  void setOnReset(Runnable onReset);
}
