package oogasalad.view.core.api;

import javafx.scene.Parent;

/**
 * One navigable UI state in the application. Implementations contribute a root node to the shared
 * Scene when the navigator activates this screen.
 */
public interface AppScreen {

  /** Root node representing this screen. */
  Parent getRoot();

  /**
   * Called whenever this screen becomes active after navigation; default implementation does nothing.
   */
  default void onEnter() {
  }
}
