package oogasalad.controller.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import oogasalad.view.core.api.AppScreen;
import util.DukeApplicationTest;

/*
 Tests ScreenManagerImpl.
 Extends DukeApplicationTest because switchScreen calls stage.setScene(),
 which requires the JavaFX thread. DukeApplicationTest handles setup and teardown.
 FakeScreen is hand-written because Mockito cannot construct a real JavaFX Scene.
*/
class ScreenManagerImplTest extends DukeApplicationTest {

  private Stage stage;
  private ScreenManagerImpl screenManager;

  @Override
  public void start(Stage stage) {
    this.stage = stage;
    this.screenManager = new ScreenManagerImpl(stage);
    stage.show();
  }

  @Test
  void switchScreen_unknownName_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> screenManager.switchScreen("GAMEPLAY", null));
  }

  @Test
  void switchScreen_afterRegisteringDifferentScreen_throwsIllegalArgumentException() {
    screenManager.registerScreen("MENU", new FakeScreen());

    assertThrows(IllegalArgumentException.class,
        () -> screenManager.switchScreen("GAMEPLAY", null));
  }

  @Test
  void registerScreen_nullScreen_throwsNullPointerException() {
    assertThrows(NullPointerException.class,
        () -> screenManager.registerScreen("MENU", null));
  }

  @Test
  void switchScreen_registeredName_setsCorrectSceneOnStage() {
    FakeScreen screen = new FakeScreen();
    screenManager.registerScreen("RESULT", screen);

    interact(() -> screenManager.switchScreen("RESULT", null));

    assertEquals(screen.getScene(), stage.getScene());
  }

  @Test
  void switchScreen_betweenTwoScreens_setsCorrectSceneEachTime() {
    FakeScreen menuScreen     = new FakeScreen();
    FakeScreen gameplayScreen = new FakeScreen();
    screenManager.registerScreen("MENU",     menuScreen);
    screenManager.registerScreen("GAMEPLAY", gameplayScreen);

    interact(() -> screenManager.switchScreen("MENU", null));
    assertEquals(menuScreen.getScene(), stage.getScene());

    interact(() -> screenManager.switchScreen("GAMEPLAY", null));
    assertEquals(gameplayScreen.getScene(), stage.getScene());
  }

  private static class FakeScreen implements AppScreen {
    private final Scene scene = new Scene(new Pane(), 400, 300);

    @Override
    public Scene getScene() {
      return scene;
    }
  }
}
