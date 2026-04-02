package oogasalad.view.core.internal;

import javafx.scene.Parent;
import javafx.scene.Scene;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.view.core.api.AppScreen;

public abstract class AbstractAppScreen implements AppScreen {
  private static final double SCENE_WIDTH = 1000;
  private static final double SCENE_HEIGHT = 700;

  private final Parent root;
  private Scene scene;
  protected final ScreenManager screenManager; // Not sure if this is the best practice

  protected AbstractAppScreen(ScreenManager screenManager) {
    this.screenManager = screenManager;
    this.root = createRoot();
  }

  protected abstract Parent createRoot();

  /**
   * Returns the cached JavaFX scene.
   *
   * @return the {@link Scene}, or null if not yet created
   */
  public Parent getRoot() {
    return this.root;
  }






}
