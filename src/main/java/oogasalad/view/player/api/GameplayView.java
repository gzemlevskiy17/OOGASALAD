package oogasalad.view.player.api;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

public interface GameplayView {
  Parent getRoot();
  Pane getGameLayer();
  void setOnPause(Runnable onPause);
  void setOnReset(Runnable onReset);
}
