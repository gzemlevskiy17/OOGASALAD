package oogasalad.view.core.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.model.engine.api.TypedComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.view.core.api.LayeredSprite;
import oogasalad.view.core.api.Renderable;
import oogasalad.view.core.api.SingleSprite;
import oogasalad.view.core.api.Sprite;
import oogasalad.view.core.api.SpriteRegistry;

/**
 * {@link SpriteRegistry} backed by the VehicleComponentPalette resource bundle, with an overlay
 * from {@link GameConfig} when available. Config-sourced entries take precedence over the bundle
 * so that custom games define their own sprites without touching properties files.
 *
 * @author Paulina Vvedenskaya
 */
public class PaletteSpriteRegistry implements SpriteRegistry {

  private static final String DEFAULT_BUNDLE = "oogasalad.view.authoring.VehicleComponentPalette";
  private static final double DEFAULT_SIZE = 60.0;

  private final ResourceBundle bundle;
  private final Map<String, Sprite> configSprites = new HashMap<>();

  public PaletteSpriteRegistry() {
    this(ResourceBundle.getBundle(DEFAULT_BUNDLE));
  }

  public PaletteSpriteRegistry(ResourceBundle bundle) {
    this.bundle = bundle;
  }

  /**
   * Populates the sprite cache from a game catalog. Entries here override the properties bundle.
   */
  public void registerFromConfig(GameConfig config) {
    configSprites.clear();
    if (config == null) {
      return;
    }
    for (ComponentInfo ci : config.components()) {
      if (ci.spriteUrl() != null && !ci.spriteUrl().isBlank()) {
        configSprites.put(ci.id(), new SingleSprite(ci.spriteUrl(), DEFAULT_SIZE, DEFAULT_SIZE));
      }
    }
    for (StaticComponentInfo si : config.staticComponents()) {
      if (si.spriteUrl() != null && !si.spriteUrl().isBlank()) {
        configSprites.put(si.id(), new SingleSprite(si.spriteUrl(), DEFAULT_SIZE, DEFAULT_SIZE));
      }
    }
  }

  @Override
  public Sprite resolve(Renderable renderable) {
    if (renderable instanceof GoalLineComponent gl) {
      Sprite fromConfig = configSprites.get("GoalLine");
      if (fromConfig != null) {
        return fromConfig;
      }
      return new SingleSprite(require("GoalLine.sprite"), gl.getWidth(), gl.getHeight());
    }
    if (renderable instanceof TypedComponent tc) {
      return resolveByType(tc.getComponentType());
    }
    throw new IllegalArgumentException("No sprite registered for renderable: " + renderable);
  }

  /**
   * Looks up a sprite by the component's display name. Checks the config-sourced cache first,
   * then falls back to the properties bundle with case normalization.
   */
  public Sprite resolveByType(String componentType) {
    Sprite fromConfig = configSprites.get(componentType);
    if (fromConfig != null) {
      return fromConfig;
    }
    String key = normalizeKey(componentType);
    double width = requireDouble(key + ".width");
    double height = requireDouble(key + ".height");

    String hub = readOrNull(key + ".hubSprite");
    String wheel = readOrNull(key + ".wheelSprite");
    if (hub != null && wheel != null) {
      return new LayeredSprite(hub, wheel, width, height);
    }
    return new SingleSprite(require(key + ".sprite"), width, height);
  }

  private String normalizeKey(String componentType) {
    String raw = componentType.replace(" ", "");
    if (bundle.containsKey(raw + ".width")) {
      return raw;
    }
    String titleCased = raw.isEmpty() ? raw
        : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    if (bundle.containsKey(titleCased + ".width")) {
      return titleCased;
    }
    return raw;
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
