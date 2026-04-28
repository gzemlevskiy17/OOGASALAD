package oogasalad.view.core.internal;

import java.util.function.Consumer;
import javafx.scene.Parent;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.api.AppScreen;

/**
 * Base AppScreen wiring: subclasses build the JavaFX subtree once via createRoot(), and expose
 * the same Parent for the shared scene forever. Screens that need navigation or menu actions
 * use the injected outcome consumer handed to subclasses through the protected field.
 *
 * @author Iurii Beliaev
 */

public abstract class AbstractAppScreen implements AppScreen {

  private final Parent root;
  protected final Consumer<Outcome> onOutcome;

  protected AbstractAppScreen(Consumer<Outcome> onOutcome) {
    this.onOutcome = onOutcome;
    this.root = createRoot();
  }

  /** Builds the scene graph for this screen; called once from the constructor. */
  protected abstract Parent createRoot();

  @Override
  public Parent getRoot() {
    return this.root;
  }
}
