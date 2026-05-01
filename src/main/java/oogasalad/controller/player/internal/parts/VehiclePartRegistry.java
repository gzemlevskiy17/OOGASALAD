package oogasalad.controller.player.internal.parts;

import java.util.HashMap;
import java.util.Locale;
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

  private static final double DEFAULT_SIZE = 60.0; //TODO: properties
  private static final double DEFAULT_THRUST = 3000.0;

  private final Map<String, VehiclePartSpec> byName = new HashMap<>();
  private final Map<String, VehiclePartSpec> byCaseInsensitive = new HashMap<>();
  private final Map<String, VehiclePartSpec> byNormalized = new HashMap<>();

  public VehiclePartRegistry() {
    register(new StructuralPartSpec("Frame", JointStyle.STRUCTURAL));
    register(new StructuralPartSpec("Sled", JointStyle.STRUCTURAL));
    register(new StructuralPartSpec("Wood Wheel", JointStyle.WHEEL));
    register(new StructuralPartSpec("Metal Wheel", JointStyle.WHEEL));
    register(new PigPartSpec("Pig"));
    register(new PigPartSpec("Small Pig"));
    register(new RelativeDirectionDrivePartSpec("Engine", new Vector(1, 0), DEFAULT_THRUST));
    register(
        new RelativeDirectionDrivePartSpec("Fan", new Vector(1, 0), 8000.0)); //TODO: properties
    register(new FixedDirectionDrivePartSpec("Balloon", new Vector(0, -1), DEFAULT_THRUST));
  }

  /**
   * Overlays registrations from a game catalog. Pig-named components always become pig parts.
   * Components that already have a hardcoded registration (e.g., wheels with WHEEL joint style)
   * are preserved unless the config entry is a pig. New components become structural parts;
   * authored button forces are handled separately by ButtonForceController.
   */
  public void registerFromConfig(GameConfig config) {
    if (config == null) {
      return;
    }
    String mainCharacterId = config.mainCharacterId();
    for (ComponentInfo ci : config.components()) {
      VehiclePartSpec spec = buildSpecFromConfig(ci, mainCharacterId);
      if (spec != null) {
        register(spec);
      }
    }
  }

  private VehiclePartSpec buildSpecFromConfig(ComponentInfo ci, String mainCharacterId) {
    String id = ci.id();
    double mass = ci.mass();
    double w = DEFAULT_SIZE;
    double h = DEFAULT_SIZE;

    VehiclePartSpec existing = getIfPresent(id);
    if (existing != null) {
      return existing;
    }
    if (isWheelPart(id)) {
      return new StructuralPartSpec(id, JointStyle.WHEEL, mass, w, h);
    }
    if (isPigPart(id, mainCharacterId)) {
      return new PigPartSpec(id, mass, w, h);
    }
    if (!ci.buttons().isEmpty()) {
      double maxMagnitude = ci.buttons().stream()
          .mapToDouble(b -> Math.sqrt(b.directionX() * b.directionX() + b.directionY() * b.directionY()))
          .max().orElse(DEFAULT_THRUST);
      double force = Math.max(maxMagnitude, DEFAULT_THRUST);
      boolean bidirectional = ci.buttons().size() >= 2 && ci.buttons().stream().anyMatch(a ->
          ci.buttons().stream().anyMatch(b ->
              a != b && Math.signum(a.directionX()) == -Math.signum(b.directionX())
                  && a.directionY() == b.directionY()
                  || Math.signum(a.directionY()) == -Math.signum(b.directionY()) && a.directionX() == b.directionX()));
      ButtonInfo first = ci.buttons().get(0);
      Vector direction = new Vector(first.directionX(), first.directionY()).normalize();
      if (bidirectional) {
        ButtonInfo pos = ci.buttons().stream()
            .filter(b -> b.directionX() > 0 || b.directionY() < 0)
            .findFirst().orElse(first);
        direction = new Vector(pos.directionX(), pos.directionY()).normalize();
        return new RelativeDirectionDrivePartSpec(id, direction, force, mass, w, h);
      }
      return new FixedDirectionDrivePartSpec(id, direction, force, mass, w, h);
    }
    return new StructuralPartSpec(id, JointStyle.STRUCTURAL, mass, w, h);
  }

  private boolean isPigPart(String id, String mainCharacterId) {
    return id != null
        && (id.equals(mainCharacterId) || id.toLowerCase(Locale.ROOT).contains("pig"));
  }

  private boolean isWheelPart(String id) {
    return id != null && id.toLowerCase(Locale.ROOT).contains("wheel");
  }

  private VehiclePartSpec getIfPresent(String paletteName) {
    if (paletteName == null) {
      return null;
    }
    VehiclePartSpec spec = byName.get(paletteName);
    if (spec != null) {
      return spec;
    }
    spec = byCaseInsensitive.get(paletteName.toLowerCase());
    if (spec != null) {
      return spec;
    }
    return byNormalized.get(paletteName.toLowerCase().replace(" ", ""));
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
    byNormalized.put(spec.paletteName().toLowerCase().replace(" ", ""), spec);
  }
}
