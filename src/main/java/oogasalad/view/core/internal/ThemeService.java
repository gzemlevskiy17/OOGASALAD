package oogasalad.view.core.internal;

import java.util.Objects;
import java.util.function.Supplier;
import javafx.scene.Scene;

/**
 * Applies a theme stylesheet to the application's shared Scene so a theme change is visible
 * across every screen sharing that Scene, not just the screen that triggered the change.
 *
 * <p>The Scene is supplied lazily via a {@link Supplier} because ScreenManagerImpl creates the
 * shared Scene on first screen switch — after this service is constructed.
 *
 * @author Mason Wu
 */
public class ThemeService {

  private final Supplier<Scene> sceneSupplier;
  private String currentThemeUrl;

  /** Captures lazily-provided Scene used by apply and clear once the Stage exists. */
  public ThemeService(Supplier<Scene> sceneSupplier) {
    this.sceneSupplier = Objects.requireNonNull(sceneSupplier, "sceneSupplier must not be null");
  }

  /**
   * Replaces any prior theme stylesheet on the shared Scene with the one at the given classpath
   * resource. Silently no-ops when the Scene does not yet exist.
   */
  public void apply(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath must not be null");
    Scene scene = sceneSupplier.get();
    if (scene == null) {
      return;
    }
    String url = Objects.requireNonNull(getClass().getResource(resourcePath)).toExternalForm();
    scene.getStylesheets().setAll(url);
    currentThemeUrl = url;
  }

  /**
   * Clears any theme stylesheet from the shared Scene, restoring per-screen styling.
   * Silently no-ops when the Scene does not yet exist.
   */
  public void clear() {
    Scene scene = sceneSupplier.get();
    if (scene == null) {
      return;
    }
    scene.getStylesheets().clear();
    currentThemeUrl = null;
  }

  /** Returns the URL of the currently-applied theme, or null if none is applied. */
  public String currentThemeUrl() {
    return currentThemeUrl;
  }
}
