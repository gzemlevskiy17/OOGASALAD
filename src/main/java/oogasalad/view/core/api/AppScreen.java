package oogasalad.view.core.api;

import javafx.scene.Scene;

/** Contract for a visual state. Must provide a Scene. */
public interface AppScreen {
  /** @return the JavaFX Scene associated with this screen. */
  Scene getScene();
}
