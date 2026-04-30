package oogasalad.controller.core.internal;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Scene;
import javafx.stage.Stage;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.view.core.api.AppScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Registry-based screen manager. Swaps JavaFX Scenes on the Stage by name. */
public class ScreenManagerImpl implements ScreenManager {

  private static final Logger LOG = LogManager.getLogger(ScreenManagerImpl.class);

  private final Map<String, AppScreen> registry = new HashMap<>();
  private final Stage stage;
  private Scene sharedScene;

  public ScreenManagerImpl(Stage stage) {
    this.stage = stage;
  }

  @Override
  public void registerScreen(String name, AppScreen screen) {
    if (screen == null) {
      throw new NullPointerException("screen must not be null");
    }
    registry.put(name, screen);
  }

  @Override
  public void switchScreen(String screenName) {
    AppScreen screen = registry.get(screenName);
    if (screen == null) {
      throw new IllegalArgumentException("No screen registered under: " + screenName);
    }
    LOG.info("Switching to screen '{}'", screenName);
    screen.onEnter();
    if (sharedScene == null) {
      sharedScene = new Scene(screen.getRoot());
      stage.setScene(sharedScene);
    } else {
      sharedScene.setRoot(screen.getRoot());
    }
  }

  @Override
  public void onOutcome(Outcome outcome) {
    switchScreen(outcome.screenKey);
  }

  @Override
  public Scene getSharedScene() {
    return sharedScene;
  }
}
