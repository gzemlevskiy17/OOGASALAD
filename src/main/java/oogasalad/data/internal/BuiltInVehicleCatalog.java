package oogasalad.data.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;

/**
 * Canonical Bad Piggies vehicle parts for UI palettes and empty game catalogs. Ids match
 * hardcoded build rules and VehiclePartRegistry (Wood Wheel, Metal Wheel)
 * so wheel revolute joints and round colliders keep working.
 */
public final class BuiltInVehicleCatalog {

  private BuiltInVehicleCatalog() {}

  public static List<ComponentInfo> defaultComponents() {
    List<ComponentInfo> list = new ArrayList<>();
    list.add(new ComponentInfo("BigPig", "data/assets/vehicle/bp_bigPig.png", 10.0, List.of()));
    list.add(new ComponentInfo("SmallPig", "data/assets/vehicle/bp_smallPig.png", 5.0, List.of()));
    list.add(new ComponentInfo("Balloon", "data/assets/vehicle/bp_balloon.png", 1.0,
        List.of(new ButtonInfo("Inflate", 0.0, 500.0))));
    list.add(new ComponentInfo("Fan", "data/assets/vehicle/bp_fan.png", 3.0,
        List.of(
            new ButtonInfo("Thrust Left", -400.0, 0.0),
            new ButtonInfo("Thrust Right", 400.0, 0.0))));
    list.add(new ComponentInfo("Engine", "data/assets/vehicle/bp_engine.png", 8.0,
        List.of(
            new ButtonInfo("Drive Left", -500.0, 0.0),
            new ButtonInfo("Drive Right", 500.0, 0.0))));
    list.add(new ComponentInfo("Frame", "data/assets/vehicle/bp_frame.png", 6.0, List.of()));
    list.add(new ComponentInfo("Sled", "data/assets/vehicle/bp_sled.png", 4.0, List.of()));
    list.add(new ComponentInfo("Wood Wheel", "", 2.0, List.of()));
    list.add(new ComponentInfo("Metal Wheel", "", 3.0, List.of()));
    return List.copyOf(list);
  }

  /**
   * Returns an immutable snapshot of a preset ComponentInfo if id matches a
   * builtin entry (object fields defined in defaultComponents() map).
   */
  public static Optional<ComponentInfo> templateById(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    String key = id.trim();
    for (ComponentInfo c : defaultComponents()) {
      if (key.equals(c.id())) {
        return Optional.of(replicate(c));
      }
    }
    return Optional.empty();
  }

  /** Preset ids not already present. */
  public static List<String> idsNotYetInCatalog(List<ComponentInfo> current) {
    Set<String> have = new HashSet<>();
    for (ComponentInfo c : current) {
      if (c.id() != null && !c.id().isBlank()) {
        have.add(c.id().trim());
      }
    }
    List<String> out = new ArrayList<>();
    for (ComponentInfo c : defaultComponents()) {
      if (!have.contains(c.id())) {
        out.add(c.id());
      }
    }
    return out;
  }

  /** New instance so later edits to one list entry do not touch the template. */
  private static ComponentInfo replicate(ComponentInfo c) {
    return new ComponentInfo(c.id(), c.spriteUrl(), c.mass(),
        c.buttons().stream()
            .map(b -> new ButtonInfo(b.name(), b.directionX(), b.directionY()))
            .toList());
  }


  public static List<String> defaultComponentIds() {
    return defaultComponents().stream().map(ComponentInfo::id).toList();
  }

  public static List<String> componentIdsFromConfig(GameConfig gameConfig) {
    if (gameConfig == null) {
      return List.of();
    }
    return gameConfig.components().stream()
        .map(ComponentInfo::id)
        .filter(id -> id != null && !id.isBlank())
        .map(String::trim)
        .distinct()
        .toList();
  }

}
