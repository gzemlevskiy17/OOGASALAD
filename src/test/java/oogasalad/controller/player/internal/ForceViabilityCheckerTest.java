package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.model.engine.CollisionDetectorImpl;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.GameEngineImpl;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests for ForceViabilityChecker, which warns when authored button forces are too weak relative to
 * gravity.
 *
 * @author Paulina Vvedenskaya
 */
class ForceViabilityCheckerTest {

  private static final Vector GRAVITY = new Vector(0, 980);

  @Test
  void check_nullConfig_returnsEmptyWarnings() {
    List<String> warnings = ForceViabilityChecker.check(null, GRAVITY);

    assertTrue(warnings.isEmpty());
  }

  @Test
  void check_componentWithNoButtons_noWarning() {
    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Frame", "", 5.0, List.of())),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, GRAVITY);

    assertTrue(warnings.isEmpty());
  }

  @Test
  void check_buttonForceExceedsThreshold_noWarning() {
    double mass = 2.0;
    double requiredForce = mass * GRAVITY.magnitude() * 0.3;
    double strongForce = requiredForce + 100;

    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Fan", "", mass,
            List.of(new ButtonInfo("Thrust", strongForce, 0)))),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, GRAVITY);

    assertTrue(warnings.isEmpty(),
        "A button force above the viability threshold should not produce a warning.");
  }

  @Test
  void check_buttonForceBelowThreshold_producesWarning() {
    double mass = 10.0;
    double tinyForce = 1.0;

    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Engine", "", mass,
            List.of(new ButtonInfo("Go", tinyForce, 0)))),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, GRAVITY);

    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).contains("Engine"),
        "Warning should mention the component id.");
  }

  @Test
  void check_multipleButtonsOnComponent_usesStrongestForce() {
    double mass = 2.0;
    double requiredForce = mass * GRAVITY.magnitude() * 0.3;
    double weakForce = 1.0;
    double strongForce = requiredForce + 500;

    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Fan", "", mass,
            List.of(
                new ButtonInfo("Weak Push", weakForce, 0),
                new ButtonInfo("Strong Push", strongForce, 0)))),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, GRAVITY);

    assertTrue(warnings.isEmpty(),
        "If any one button force exceeds the threshold, no warning should be emitted.");
  }

  @Test
  void check_diagonalButtonForce_usesMagnitude() {
    double mass = 2.0;
    double requiredForce = mass * GRAVITY.magnitude() * 0.3;
    double component = (requiredForce + 100) / Math.sqrt(2);

    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Rocket", "", mass,
            List.of(new ButtonInfo("Diagonal", component, component)))),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, GRAVITY);

    assertTrue(warnings.isEmpty(),
        "Diagonal force magnitude should be computed, not just individual x/y components.");
  }

  @Test
  void check_twoWeakComponents_producesWarningForEach() {
    TestGameConfig config = new TestGameConfig("test", "",
        List.of(
            new ComponentInfo("Fan", "", 10.0,
                List.of(new ButtonInfo("Blow", 1, 0))),
            new ComponentInfo("Engine", "", 10.0,
                List.of(new ButtonInfo("Go", 1, 0)))),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, GRAVITY);

    assertEquals(2, warnings.size());
  }

  @Test
  void check_zeroGravity_noWarnings() {
    TestGameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Fan", "", 10.0,
            List.of(new ButtonInfo("Blow", 1, 0)))),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, new Vector(0, 0));

    assertTrue(warnings.isEmpty(),
        "With zero gravity, any force should be viable.");
  }

  @Test
  void suggestedMinForce_returnsCorrectValue() {
    double mass = 5.0;
    double gravMag = 980.0;

    double suggested = ForceViabilityChecker.suggestedMinForce(mass, gravMag);

    assertEquals(mass * gravMag * 0.3, suggested, 1e-9);
  }

  @Test
  void check_belowThreshold_objectDoesNotMoveSignificantlyInPhysics() {
    double mass = 10.0;
    double tinyForce = 1.0;

    TestGameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("BigPig", "", mass,
            List.of(new ButtonInfo("Nudge", tinyForce, 0)))),
        List.of(), List.of());

    List<String> warnings = ForceViabilityChecker.check(config, GRAVITY);
    assertEquals(1, warnings.size(), "Checker should flag this force as too weak.");

    PhysicsEnvironment env = new PhysicsEnvironment(GRAVITY, 0.1);
    WorldImpl world = new WorldImpl(env);
    PigComponent pig = new PigComponent(300, 300, 0,
        ConvexPolygon.axisAlignedBox(270, 270, 60, 60),
        "", 0.3, mass, mass * (60.0 * 60.0 + 60.0 * 60.0) / 12.0,
        0.5, true, 100, "BigPig");
    world.addComponent(pig);

    pig.addExternalForce("button:nudge", new Vector(tinyForce, 0));

    GameEngineImpl engine = new GameEngineImpl(world, new CollisionDetectorImpl());

    double startX = pig.getStaticState().getLinearPosition().x();
    for (int i = 0; i < 60; i++) {
      engine.step(1.0 / 60.0);
    }
    double endX = pig.getStaticState().getLinearPosition().x();

    double horizontalDisplacement = Math.abs(endX - startX);
    assertTrue(horizontalDisplacement < 5.0,
        "A force flagged as too weak should not move the object significantly horizontally. "
            + "Moved " + horizontalDisplacement + " pixels in 1 second.");
  }
}
