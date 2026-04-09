package oogasalad.view.core.api;

import javafx.scene.Parent;

/** Contract for a visual state. Must provide a root node for the shared Scene. */
public interface AppScreen {
  /** @return the root node representing this screen's UI. */
  Parent getRoot();

  /** Called whenever the screen becomes active. */
  default void onEnter() {
  }
}
