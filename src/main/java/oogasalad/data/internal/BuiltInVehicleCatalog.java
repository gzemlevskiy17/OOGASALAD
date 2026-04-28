package oogasalad.data.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;

/**
 * Canonical Bad Piggies vehicle parts for UI palettes and empty game catalogs. Ids match
 * hardcoded build rules and VehiclePartRegistry (Wood Wheel, Metal Wheel)
 * so wheel revolute joints and round colliders keep working.
 *
 * @author Iurii Beliaev
 */
public final class BuiltInVehicleCatalog {

  private static final ResourceBundle CONFIG =
      ResourceBundle.getBundle("oogasalad.data.internal.BuiltInVehicleCatalog");

  private static final List<ComponentInfo> DEFAULT_COMPONENTS = loadDefaultComponents();

  private BuiltInVehicleCatalog() {}

  private static List<ComponentInfo> loadDefaultComponents() {
    List<ComponentInfo> list = new ArrayList<>();
    for (String rawId : CONFIG.getString("componentIds").split(",")) {
      String id = rawId.trim();
      if (id.isEmpty()) {
        continue;
      }
      String spriteKey = id + ".sprite";
      String sprite = CONFIG.containsKey(spriteKey) ? CONFIG.getString(spriteKey) : "";
      double mass = Double.parseDouble(CONFIG.getString(id + ".mass").trim());
      list.add(new ComponentInfo(id, sprite, mass, parseButtons(id)));
    }
    return List.copyOf(list);
  }

  private static List<ButtonInfo> parseButtons(String componentId) {
    String key = componentId + ".buttons";
    if (!CONFIG.containsKey(key)) {
      return List.of();
    }
    String raw = CONFIG.getString(key).trim();
    if (raw.isEmpty()) {
      return List.of();
    }
    List<ButtonInfo> buttons = new ArrayList<>();
    for (String segment : raw.split(";")) {
      String part = segment.trim();
      if (part.isEmpty()) {
        continue;
      }
      String[] triple = part.split("\\|", 3);
      if (triple.length != 3) {
        throw new IllegalArgumentException(
            "BuiltInVehicleCatalog: bad button for '" + componentId + "': " + part);
      }
      buttons.add(new ButtonInfo(
          triple[0].trim(),
          Double.parseDouble(triple[1].trim()),
          Double.parseDouble(triple[2].trim())));
    }
    return List.copyOf(buttons);
  }

  public static List<ComponentInfo> defaultComponents() {
    return DEFAULT_COMPONENTS;
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
    if (current == null) {
      current = List.of(); // TODO: Refactor this simple fallback in the future
    }

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
