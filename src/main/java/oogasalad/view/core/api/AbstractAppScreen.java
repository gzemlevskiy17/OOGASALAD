package oogasalad.view.core.api;

import java.util.function.Consumer;
import javafx.scene.Parent;
import oogasalad.controller.core.api.Outcome;

/**
 * Base screen that builds its JavaFX subtree once in the constructor and reuses that root thereafter.
 * Subclasses read navigation intents from the protected onOutcome consumer.
 *
 * @author Iurii Beliaev
 */

public abstract class AbstractAppScreen implements AppScreen {

  private final Parent root;
  /** Emits structured navigation outcomes to the owning screen manager. */
  protected final Consumer<Outcome> onOutcome;

  /**
   * Captures outcome callback then builds UI via createRoot before any subclass constructor body runs further.
   */
  protected AbstractAppScreen(Consumer<Outcome> onOutcome) {
    this.onOutcome = onOutcome;
    this.root = createRoot();
  }

  /** Builds the scene graph subtree for this screen; invoked exactly once from the constructor. */
  protected abstract Parent createRoot();

  /** Returns the subtree built during construction. */
  @Override
  public Parent getRoot() {
    return this.root;
  }
}
