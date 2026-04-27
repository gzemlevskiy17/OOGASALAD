package oogasalad.model.player.internal;

import static oogasalad.model.player.api.Side.BOTTOM;
import static oogasalad.model.player.api.Side.LEFT;
import static oogasalad.model.player.api.Side.RIGHT;
import static oogasalad.model.player.api.Side.TOP;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.model.player.api.BuildRules;
import oogasalad.model.player.api.BuildRulesRegistry;
import oogasalad.model.player.api.Containment;
import oogasalad.model.player.api.PositionConstraint;

/**
 * Default in-code rules registry. One entry per component type in the current palette.
 *
 * @author Paulina Vvedenskaya
 */
public class DefaultBuildRulesRegistry implements BuildRulesRegistry {

  private static final BuildRules STRUCTURAL_RULE = new BuildRules(
      Set.of(TOP, RIGHT, BOTTOM, LEFT), Set.of(), Set.of(),
      Containment.NONE, PositionConstraint.NONE);
  private static final BuildRules WHEEL_RULE = new BuildRules(
      Set.of(TOP), Set.of(TOP), Set.of(),
      Containment.NONE, PositionConstraint.BOTTOM_ROW_OF_GRID);
  private static final BuildRules PIG_RULE = new BuildRules(
      Set.of(), Set.of(), Set.of(),
      Containment.INSIDE_FRAME, PositionConstraint.NONE);
  private static final BuildRules DRIVE_RULE = new BuildRules(
      Set.of(RIGHT), Set.of(RIGHT), Set.of(LEFT),
      Containment.NONE, PositionConstraint.NONE);
  private static final BuildRules BALLOON_RULE = new BuildRules(
      Set.of(BOTTOM), Set.of(BOTTOM), Set.of(TOP),
      Containment.NONE, PositionConstraint.NONE);
  private static final BuildRules ENGINE_RULE = new BuildRules(
      Set.of(RIGHT, BOTTOM), Set.of(), Set.of(),
      Containment.NONE, PositionConstraint.NONE);

  private static final Map<String, BuildRules> DEFAULT_RULES = Map.ofEntries(
      Map.entry("Frame", STRUCTURAL_RULE),
      Map.entry("Wood Wheel", WHEEL_RULE),
      Map.entry("Metal Wheel", WHEEL_RULE),
      Map.entry("Sled", WHEEL_RULE),
      Map.entry("Pig", PIG_RULE),
      Map.entry("Small Pig", PIG_RULE),
      Map.entry("Fan", DRIVE_RULE),
      Map.entry("Balloon", BALLOON_RULE),
      Map.entry("Engine", ENGINE_RULE)
  );

  private final Map<String, BuildRules> rules = new LinkedHashMap<>(DEFAULT_RULES);

  @Override
  public BuildRules lookup(String componentType) {
    BuildRules r = rules.get(componentType);
    if (r == null && componentType != null) {
      r = rules.entrySet().stream()
          .filter(entry -> entry.getKey().equalsIgnoreCase(componentType))
          .map(Map.Entry::getValue)
          .findFirst()
          .orElse(null);
    }
    if (r == null) {
      throw new IllegalArgumentException("No build rules registered for: " + componentType);
    }
    return r;
  }

  @Override
  public void registerFromConfig(GameConfig config) {
    if (config == null) {
      return;
    }
    String mainCharacterId = config.mainCharacterId();
    for (ComponentInfo component : config.components()) {
      String id = component.id();
      if (id == null || id.isBlank()) {
        continue;
      }
      rules.put(id, rulesFor(component, mainCharacterId));
    }
  }

  private BuildRules rulesFor(ComponentInfo component, String mainCharacterId) {
    BuildRules existing = lookupIfPresent(component.id());
    if (existing != null) {
      return existing;
    }
    String id = component.id();
    if (id.equals(mainCharacterId) || id.toLowerCase().contains("pig")) {
      return PIG_RULE;
    }
    if (!component.buttons().isEmpty()) {
      return DRIVE_RULE;
    }
    return STRUCTURAL_RULE;
  }

  private BuildRules lookupIfPresent(String componentType) {
    try {
      return lookup(componentType);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }
}
