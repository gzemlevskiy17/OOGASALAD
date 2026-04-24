package oogasalad.view.core.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.view.core.api.LayeredSprite;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.SingleSprite;
import oogasalad.view.core.api.Sprite;
import oogasalad.view.core.api.SpriteRegistry;
import oogasalad.view.core.api.SpriteRenderable;

/**
 * {@link SpriteRegistry} backed by the VehicleComponentPalette resource bundle. Handles
 * palette-driven components (via {@link SpriteRenderable#getComponentType()}) and
 * {@link GoalLineComponent}, whose dimensions come from the component instance rather than the
 * palette.
 *
 * @author Paulina Vvedenskaya
 */
public class PaletteSpriteRegistry implements SpriteRegistry {

  private static final String DEFAULT_BUNDLE = "oogasalad.view.authoring.VehicleComponentPalette";

  private final ResourceBundle bundle;

  public PaletteSpriteRegistry() {
    this(ResourceBundle.getBundle(DEFAULT_BUNDLE));
  }

  public PaletteSpriteRegistry(ResourceBundle bundle) {
    this.bundle = bundle;
  }

  @Override
  public Sprite resolve(Renderable renderable) {
    if (renderable instanceof GoalLineComponent gl) {
      return new SingleSprite(require("GoalLine.sprite"), gl.getWidth(), gl.getHeight());
    }
    if (renderable instanceof SpriteRenderable sr) {
      return resolveByType(sr.getComponentType());
    }
    throw new IllegalArgumentException("No sprite registered for renderable: " + renderable);
  }

  /**
   * Looks up a sprite by the component's display name — used by the builder preview UI.
   */
  public Sprite resolveByType(String componentType) {
    String key = componentType.replace(" ", "");
    double width = requireDouble(key + ".width");
    double height = requireDouble(key + ".height");

    String hub = readOrNull(key + ".hubSprite");
    String wheel = readOrNull(key + ".wheelSprite");
    if (hub != null && wheel != null) {
      return new LayeredSprite(hub, wheel, width, height);
    }
    return new SingleSprite(require(key + ".sprite"), width, height);
  }

  private String readOrNull(String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      return null;
    }
  }

  private String require(String key) {
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      throw new IllegalArgumentException("Missing palette key: " + key, e);
    }
  }

  private double requireDouble(String key) {
    return Double.parseDouble(require(key));
  }
}
