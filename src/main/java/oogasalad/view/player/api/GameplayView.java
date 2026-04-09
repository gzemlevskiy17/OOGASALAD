package oogasalad.view.player.api;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;
/**
 * View contract consumed by gameplay coordination/controller code.
 * Implementations expose render surface and UI control hooks.
 *
 * @author Iurii Beliaev
 */
public interface GameplayView {
  Parent getRoot();
  Pane getGameLayer();
  void setOnPause(Runnable onPause);
  void setOnReset(Runnable onReset);
}
