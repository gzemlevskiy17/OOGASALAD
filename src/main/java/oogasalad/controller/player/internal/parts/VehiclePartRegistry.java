package oogasalad.controller.player.internal.parts;

import java.util.HashMap;
import java.util.Map;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.Vector;

/**
 * Registry of vehicle part specifications. Ships with hardcoded Bad Piggies defaults and can be
 * overlaid with entries from a {@link GameConfig} so that any custom game's components work in the
 * vehicle builder and physics engine without touching code.
 *
 * @author Paulina Vvedenskaya
 */
public final class VehiclePartRegistry {

  private static final double DEFAULT_SIZE = 60.0;
  private static final double DEFAULT_THRUST = 3000.0;

  private final Map<String, VehiclePartSpec> byName = new HashMap<>();
  private final Map<String, VehiclePartSpec> byCaseInsensitive = new HashMap<>();

  public VehiclePartRegistry() {
  }

  /**
   * Overlays registrations from a game catalog. Config entries override hardcoded defaults when
   * the component id matches. Components with buttons become drive parts; the main character
   * becomes a pig part; everything else becomes structural.
   */
  public void registerFromConfig(GameConfig config) {
    if (config == null) {
      return;
    }
    String mainCharacterId = config.mainCharacterId();
    for (ComponentInfo ci : config.components()) {
      VehiclePartSpec spec = buildSpecFromConfig(ci, mainCharacterId);
      register(spec);
    }
  }

  private VehiclePartSpec buildSpecFromConfig(ComponentInfo ci, String mainCharacterId) {
    String id = ci.id();
    double mass = ci.mass();
    double w = DEFAULT_SIZE;
    double h = DEFAULT_SIZE;

    if (id.equals(mainCharacterId)) {
      return new PigPartSpec(id, mass, w, h);
    }
    VehiclePartSpec existing = getIfPresent(id);
    if (existing != null) {
      return existing;
    }
    if (!ci.buttons().isEmpty()) {
      ButtonInfo first = ci.buttons().get(0);
      Vector direction = new Vector(first.directionX(), first.directionY()).normalize();
      double force = Math.max(
          Math.sqrt(first.directionX() * first.directionX()
              + first.directionY() * first.directionY()),
          DEFAULT_THRUST);
      return new DrivePartSpec(id, direction, force, mass, w, h);
    }
    return new StructuralPartSpec(id, JointStyle.STRUCTURAL, mass, w, h);
  }

  private VehiclePartSpec getIfPresent(String paletteName) {
    if (paletteName == null) {
      return null;
    }
    VehiclePartSpec spec = byName.get(paletteName);
    return spec != null ? spec : byCaseInsensitive.get(paletteName.toLowerCase());
  }

  public VehiclePartSpec get(String paletteName) {
    VehiclePartSpec spec = getIfPresent(paletteName);
    if (spec == null) {
      throw new IllegalArgumentException("Unknown vehicle part: " + paletteName);
    }
    return spec;
  }

  private void register(VehiclePartSpec spec) {
    byName.put(spec.paletteName(), spec);
    byCaseInsensitive.put(spec.paletteName().toLowerCase(), spec);
  }
}
