package oogasalad.model.player.internal;

import static oogasalad.model.player.api.Side.BOTTOM;
import static oogasalad.model.player.api.Side.LEFT;
import static oogasalad.model.player.api.Side.RIGHT;
import static oogasalad.model.player.api.Side.TOP;

import java.util.Map;
import java.util.Set;
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

  private static final Map<String, BuildRules> RULES = Map.ofEntries(
      Map.entry("Frame", new BuildRules(
          Set.of(TOP, RIGHT, BOTTOM, LEFT), Set.of(), Set.of(),
          Containment.NONE, PositionConstraint.NONE)),
      Map.entry("Wheel", new BuildRules(
          Set.of(TOP), Set.of(TOP), Set.of(),
          Containment.NONE, PositionConstraint.BOTTOM_ROW_OF_GRID)),
      Map.entry("Small Wheel", new BuildRules(
          Set.of(TOP), Set.of(TOP), Set.of(),
          Containment.NONE, PositionConstraint.BOTTOM_ROW_OF_GRID)),
      Map.entry("Large Wheel", new BuildRules(
          Set.of(TOP), Set.of(TOP), Set.of(),
          Containment.NONE, PositionConstraint.BOTTOM_ROW_OF_GRID)),
      Map.entry("Sled", new BuildRules(
          Set.of(TOP), Set.of(TOP), Set.of(),
          Containment.NONE, PositionConstraint.BOTTOM_ROW_OF_GRID)),
      Map.entry("Pig", new BuildRules(
          Set.of(), Set.of(), Set.of(),
          Containment.INSIDE_FRAME, PositionConstraint.NONE)),
      Map.entry("Small Pig", new BuildRules(
          Set.of(), Set.of(), Set.of(),
          Containment.INSIDE_FRAME, PositionConstraint.NONE)),
      Map.entry("Fan", new BuildRules(
          Set.of(RIGHT), Set.of(RIGHT), Set.of(LEFT),
          Containment.NONE, PositionConstraint.NONE)),
      Map.entry("Balloon", new BuildRules(
          Set.of(BOTTOM), Set.of(BOTTOM), Set.of(TOP),
          Containment.NONE, PositionConstraint.NONE))
  );

  @Override
  public BuildRules lookup(String componentType) {
    BuildRules r = RULES.get(componentType);
    if (r == null) {
      throw new IllegalArgumentException("No build rules registered for: " + componentType);
    }
    return r;
  }
}
