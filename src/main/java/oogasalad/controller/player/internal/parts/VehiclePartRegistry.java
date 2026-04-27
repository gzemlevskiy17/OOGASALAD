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
    register(new StructuralPartSpec("Frame", JointStyle.STRUCTURAL));
    register(new StructuralPartSpec("Sled", JointStyle.STRUCTURAL));
    register(new StructuralPartSpec("Wood Wheel", JointStyle.WHEEL));
    register(new StructuralPartSpec("Metal Wheel", JointStyle.WHEEL));
    register(new PigPartSpec("Pig"));
    register(new PigPartSpec("Small Pig"));
    register(new DrivePartSpec("Engine", new Vector(1, 0), 3000.0));
    register(new DrivePartSpec("Fan", new Vector(1, 0), 1500.0));
    register(new DrivePartSpec("Balloon", new Vector(0, -1), 8000.0));
  }

  /**
   * Overlays registrations from a game catalog. Config entries override hardcoded defaults when
   * the component id matches. Components with buttons become drive parts; the primary component
   * becomes a pig part; everything else becomes structural.
   */
  public void registerFromConfig(GameConfig config) {
    if (config == null) {
      return;
    }
    String primaryId = config.primaryComponentId();
    for (ComponentInfo ci : config.components()) {
      VehiclePartSpec spec = buildSpecFromConfig(ci, primaryId);
      register(spec);
    }
  }

  private VehiclePartSpec buildSpecFromConfig(ComponentInfo ci, String primaryId) {
    String id = ci.id();
    double mass = ci.mass();
    double w = DEFAULT_SIZE;
    double h = DEFAULT_SIZE;

    if (!ci.buttons().isEmpty()) {
      ButtonInfo first = ci.buttons().get(0);
      Vector direction = new Vector(first.directionX(), first.directionY()).normalize();
      double force = Math.max(
          Math.sqrt(first.directionX() * first.directionX()
              + first.directionY() * first.directionY()),
          DEFAULT_THRUST);
      return new DrivePartSpec(id, direction, force, mass, w, h);
    }
    if (id.equals(primaryId)) {
      return new PigPartSpec(id, mass, w, h);
    }
    return new StructuralPartSpec(id, JointStyle.STRUCTURAL, mass, w, h);
  }

  public VehiclePartSpec get(String paletteName) {
    VehiclePartSpec spec = byName.get(paletteName);
    if (spec == null) {
      spec = byCaseInsensitive.get(paletteName.toLowerCase());
    }
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
