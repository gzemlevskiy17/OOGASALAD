package oogasalad.view.core.internal;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import oogasalad.view.core.api.AppScreen;

public abstract class AbstractAppScreen implements AppScreen {
  private final Parent root;
  private Scene scene;
  private final Stage stage;

  protected AbstractAppScreen(Stage stage) {
    this.stage = stage;
    this.root = createRoot();
  }

  protected abstract Parent createRoot();

  public Scene createScene(int width, int height) {
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

  /**
   * Returns the JavaFX stage associated with this scene.
   *
   * @return the {@link Stage}
   */
  public Stage getStage() {
    return this.stage;
  }

  /**
   * Returns the root node of this scene.
   *
   * @return the root {@link Parent} node
   */
  public Parent getRoot() {
    return root;
  }






}
