package oogasalad.data.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

  public static Optional<ComponentInfo> templateById(String id) { /* lookup in defaultComponents */ }

  public static List<String> idsNotYetInCatalog(List<ComponentInfo> current) { /* omit already-present ids */ }

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
