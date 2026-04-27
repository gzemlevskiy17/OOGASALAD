package oogasalad.controller.player.internal.parts;

import java.util.HashMap;
import java.util.Map;
import oogasalad.model.engine.api.Vector;

/**
 * @author Paulina Vvedenskaya
 */
public final class VehiclePartRegistry {

  private final Map<String, VehiclePartSpec> byName = new HashMap<>();

  public VehiclePartRegistry() {
    register(new StructuralPartSpec("Frame", JointStyle.STRUCTURAL));
    register(new StructuralPartSpec("Sled", JointStyle.STRUCTURAL));
    register(new StructuralPartSpec("Wood Wheel", JointStyle.WHEEL));
    register(new StructuralPartSpec("Metal Wheel", JointStyle.WHEEL));
    register(new PigPartSpec("Pig"));
    register(new PigPartSpec("Small Pig"));
    register(new DrivePartSpec("Engine", new Vector(1, 0), 3000.0)); //TODO: make vectors point in direction of motion
    register(new DrivePartSpec("Fan", new Vector(1, 0), 10000.0));
    register(new DrivePartSpec("Balloon", new Vector(0, -1), 8000.0));
  }

  public VehiclePartSpec get(String paletteName) {
    VehiclePartSpec spec = byName.get(paletteName);
    if (spec == null) {
      throw new IllegalArgumentException("Unknown vehicle part: " + paletteName);
    }
    return spec;
  }

  private void register(VehiclePartSpec spec) {
    byName.put(spec.paletteName(), spec);
  }
}
