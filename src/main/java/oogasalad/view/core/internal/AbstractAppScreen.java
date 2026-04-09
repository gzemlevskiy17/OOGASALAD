package oogasalad.view.core.internal;

import java.util.function.Consumer;
import javafx.scene.Parent;
import oogasalad.controller.core.api.Outcome;
import oogasalad.view.core.api.AppScreen;

/**
 * The abstract class which all AppScreens inherit from.
 * Children of this class override createRoot()
 * @author Iurii Beliaev
 */

public abstract class AbstractAppScreen implements AppScreen {

  private final Parent root;
  protected final Consumer<Outcome> onOutcome;

  protected AbstractAppScreen(Consumer<Outcome> onOutcome) {
    this.onOutcome = onOutcome;
    this.root = createRoot();
  }

  protected abstract Parent createRoot();

  @Override
  public Parent getRoot() {
    return this.root;
  }
}
