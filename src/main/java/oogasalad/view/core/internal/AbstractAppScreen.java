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
    this.scene = createScene(getWidth(), getHeight());
  }

  private double getHeight() {
    return SCENE_HEIGHT;
  }

  private double getWidth() {
    return SCENE_WIDTH;
  }

  protected abstract Parent createRoot();

  protected Scene createScene(double width, double height) {
    if (scene == null) {
      scene = new Scene(root, width, height);
    }

    return scene;

  }

  /**
   * Returns the cached JavaFX scene.
   *
   * @return the {@link Scene}, or null if not yet created
   */
  public Scene getScene() {
    return scene;
  }






}
