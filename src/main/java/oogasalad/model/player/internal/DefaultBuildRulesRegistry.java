package oogasalad.model.player.internal;

import static oogasalad.model.player.api.Side.BOTTOM;
import static oogasalad.model.player.api.Side.LEFT;
import static oogasalad.model.player.api.Side.RIGHT;
import static oogasalad.model.player.api.Side.TOP;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.model.player.api.BuildRules;
import oogasalad.model.player.api.BuildRulesRegistry;
import oogasalad.model.player.api.Containment;
import oogasalad.model.player.api.PositionConstraint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default in-code rules registry. One entry per component type in the current palette.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 */
public class DefaultBuildRulesRegistry implements BuildRulesRegistry {

  private static final Logger LOG = LogManager.getLogger(DefaultBuildRulesRegistry.class);
  private static final BuildRules FRAME_RULES = new BuildRules(
      Set.of(TOP, RIGHT, BOTTOM, LEFT), Set.of(), Set.of(),
      Containment.NONE, PositionConstraint.NONE);
  private static final BuildRules WHEEL_RULES = new BuildRules(
      Set.of(TOP), Set.of(TOP), Set.of(),
      Containment.NONE, PositionConstraint.BOTTOM_ROW_OF_GRID);
  private static final BuildRules PIG_RULES = new BuildRules(
      Set.of(), Set.of(), Set.of(),
      Containment.INSIDE_FRAME, PositionConstraint.NONE);

  private static final Map<String, BuildRules> DEFAULT_RULES = Map.ofEntries(
      Map.entry("Frame", FRAME_RULES),
      Map.entry("Wood Wheel", WHEEL_RULES),
      Map.entry("Metal Wheel", WHEEL_RULES),
      Map.entry("Sled", WHEEL_RULES),
      Map.entry("Pig", PIG_RULES),
      Map.entry("BigPig", PIG_RULES),
      Map.entry("Small Pig", PIG_RULES),
      Map.entry("SmallPig", PIG_RULES),
      Map.entry("Fan", new BuildRules(
          Set.of(RIGHT), Set.of(RIGHT), Set.of(LEFT),
          Containment.NONE, PositionConstraint.NONE)),
      Map.entry("Balloon", new BuildRules(
          Set.of(BOTTOM), Set.of(BOTTOM), Set.of(TOP),
          Containment.NONE, PositionConstraint.NONE)),
      Map.entry("Engine", new BuildRules(
          Set.of(RIGHT, BOTTOM), Set.of(), Set.of(),
          Containment.NONE, PositionConstraint.NONE))
  );

  private final Map<String, BuildRules> rules = new HashMap<>(DEFAULT_RULES);

  @Override
  public void registerFromConfig(GameConfig config) {
    Optional.ofNullable(config).ifPresent(this::registerCatalogRules);
  }

  @Override
  public BuildRules lookup(String componentType) {
    return Optional.ofNullable(rules.get(componentType))
        .orElseThrow(() -> new IllegalArgumentException(
            "No build rules registered for: " + componentType));
  }

  private void registerCatalogRules(GameConfig config) {
    config.components().stream()
        .map(ComponentInfo::id)
        .map(id -> id == null ? "" : id.trim())
        .filter(id -> !id.isBlank())
        .forEach(id -> rules.putIfAbsent(id, inferredRules(id, config.primaryComponentId())));
    LOG.debug("Registered build rules for {} catalog components", config.components().size());
  }

  private BuildRules inferredRules(String componentId, String primaryComponentId) {
    return isPigComponent(componentId, primaryComponentId) ? PIG_RULES : FRAME_RULES;
  }

  private boolean isPigComponent(String componentId, String primaryComponentId) {
    return componentId.equals(primaryComponentId)
        || componentId.toLowerCase(Locale.ROOT).contains("pig");
  }

}
