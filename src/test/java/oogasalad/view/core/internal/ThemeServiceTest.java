package oogasalad.view.core.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

/**
 * Tests for {@link ThemeService}.
 *
 * @author Mason Wu
 */
class ThemeServiceTest extends DukeApplicationTest {

  private static final String DARK_THEME = "/oogasalad/view/core/themes/theme-dark.css";
  private static final String DUKE_THEME = "/oogasalad/view/core/themes/theme-duke.css";

  private Scene scene;

  @Override
  public void start(Stage stage) {
    scene = new Scene(new Pane(), 200, 200);
    stage.setScene(scene);
    stage.show();
  }

  @Test
  void apply_appliesTheme_setsSingleStylesheetOnScene() {
    ThemeService service = new ThemeService(() -> scene);

    runAsJFXAction(() -> service.apply(DARK_THEME));

    assertEquals(1, scene.getStylesheets().size(),
        "applying a theme should replace any prior stylesheets, leaving exactly one");
    assertTrue(scene.getStylesheets().get(0).endsWith("theme-dark.css"));
    assertNotNull(service.currentThemeUrl());
  }

  @Test
  void apply_calledTwice_replacesPriorThemeRatherThanStacking() {
    ThemeService service = new ThemeService(() -> scene);

    runAsJFXAction(() -> service.apply(DARK_THEME));
    runAsJFXAction(() -> service.apply(DUKE_THEME));

    assertEquals(1, scene.getStylesheets().size(),
        "applying a second theme must replace, not append");
    assertTrue(scene.getStylesheets().get(0).endsWith("theme-duke.css"));
  }

  @Test
  void clear_afterApply_removesAllStylesheetsFromScene() {
    ThemeService service = new ThemeService(() -> scene);
    runAsJFXAction(() -> service.apply(DARK_THEME));

    runAsJFXAction(service::clear);

    assertTrue(scene.getStylesheets().isEmpty(),
        "clear() should leave the Scene with no theme stylesheets");
    assertNull(service.currentThemeUrl());
  }

  @Test
  void apply_whenSceneSupplierReturnsNull_doesNotThrow() {
    ThemeService service = new ThemeService(() -> null);

    runAsJFXAction(() -> service.apply(DARK_THEME));

    assertNull(service.currentThemeUrl(),
        "apply() must be a no-op when no Scene is yet available");
  }

  @Test
  void clear_whenSceneSupplierReturnsNull_doesNotThrow() {
    ThemeService service = new ThemeService(() -> null);
    runAsJFXAction(service::clear);
  }

  @Test
  void constructor_nullSupplier_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> new ThemeService(null));
  }
}
