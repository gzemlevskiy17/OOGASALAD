package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.RelativeDirectionDriveComponent;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests per-ButtonInfo toolbar creation and force binding in ButtonForceController.
 *
 * @author Paulina Vvedenskaya
 */
class ButtonForceControllerTest {

  private static final double EPS = 1e-9;

  private World world;
  private InputRouterImpl router;

  @BeforeEach
  void setUp() {
    world = new WorldImpl();
    router = new InputRouterImpl();
  }

  @Test
  void collectButtonIds_fanWithTwoButtons_returnsBothButtonIds() {
    GameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("Fan", "", 3.0,
            List.of(
                new ButtonInfo("Thrust Left", -400, 0),
                new ButtonInfo("Thrust Right", 400, 0)))),
        List.of(), List.of());

    RelativeDirectionDriveComponent fan = newDrive("Fan");
    world.addComponent(fan);

    ButtonForceController controller = new ButtonForceController();
    Set<String> ids = controller.collectButtonIds(config, world);

    assertTrue(ids.contains("thrust left"), "Should include Thrust Left button ID");
    assertTrue(ids.contains("thrust right"), "Should include Thrust Right button ID");
  }

  @Test
  void collectButtonIds_componentNotInWorld_excludesItsButtons() {
    GameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Rocket", "", 5.0,
            List.of(new ButtonInfo("Boost", 0, 1000)))),
        List.of(), List.of());

    ButtonForceController controller = new ButtonForceController();
    Set<String> ids = controller.collectButtonIds(config, world);

    assertTrue(ids.isEmpty(),
        "Buttons for components not placed in the world should be excluded.");
  }

  @Test
  void bind_fanThrustLeft_appliesForceOnPress() {
    GameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("Fan", "", 3.0,
            List.of(new ButtonInfo("Thrust Left", -400, 0)))),
        List.of(), List.of());

    RelativeDirectionDriveComponent fan = newDrive("Fan");
    world.addComponent(fan);

    ButtonForceController controller = new ButtonForceController();
    controller.bind(router, config, world);
    router.handleButtonPress("thrust left");

    Vector externalForce = fan.getExternalForceTotal();
    assertEquals(-400, externalForce.x(), EPS,
        "Pressing 'Thrust Left' should apply a leftward force to the Fan.");
  }

  @Test
  void bind_fanThrustLeft_removesForceOnRelease() {
    GameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("Fan", "", 3.0,
            List.of(new ButtonInfo("Thrust Left", -400, 0)))),
        List.of(), List.of());

    RelativeDirectionDriveComponent fan = newDrive("Fan");
    world.addComponent(fan);

    ButtonForceController controller = new ButtonForceController();
    controller.bind(router, config, world);
    router.handleButtonPress("thrust left");
    router.handleButtonRelease("thrust left");

    Vector externalForce = fan.getExternalForceTotal();
    assertEquals(0, externalForce.x(), EPS,
        "Releasing 'Thrust Left' should remove the force from the Fan.");
  }

  @Test
  void bind_bigPigWithButtons_appliesForceToNonDriveableComponent() {
    GameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("BigPig", "", 10.0,
            List.of(
                new ButtonInfo("Move Left", -300, 0),
                new ButtonInfo("Move Right", 300, 0),
                new ButtonInfo("Jump", 0, 400)))),
        List.of(), List.of());

    PigComponent pig = newPig("BigPig");
    world.addComponent(pig);

    ButtonForceController controller = new ButtonForceController();
    controller.bind(router, config, world);

    router.handleButtonPress("jump");

    Vector externalForce = pig.getExternalForceTotal();
    assertEquals(0, externalForce.x(), EPS);
    assertEquals(400, externalForce.y(), EPS,
        "Pressing 'Jump' should apply an upward force to BigPig.");
  }

  @Test
  void bind_twoButtonsOnSameComponent_bothForcesApplySimultaneously() {
    GameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("BigPig", "", 10.0,
            List.of(
                new ButtonInfo("Move Right", 300, 0),
                new ButtonInfo("Jump", 0, 400)))),
        List.of(), List.of());

    PigComponent pig = newPig("BigPig");
    world.addComponent(pig);

    ButtonForceController controller = new ButtonForceController();
    controller.bind(router, config, world);
    router.handleButtonPress("move right");
    router.handleButtonPress("jump");

    Vector externalForce = pig.getExternalForceTotal();
    assertEquals(300, externalForce.x(), EPS);
    assertEquals(400, externalForce.y(), EPS,
        "Both button forces should be active simultaneously.");
  }

  @Test
  void collectButtonIds_noConfig_returnsEmpty() {
    ButtonForceController controller = new ButtonForceController();
    Set<String> ids = controller.collectButtonIds(null, world);

    assertTrue(ids.isEmpty());
  }

  @Test
  void bind_threeButtonsOnSameComponent_allForcesSum() {
    GameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("BigPig", "", 10.0,
            List.of(
                new ButtonInfo("Move Right", 300, 0),
                new ButtonInfo("Jump", 0, 400),
                new ButtonInfo("Diagonal Boost", 100, 100)))),
        List.of(), List.of());

    PigComponent pig = newPig("BigPig");
    world.addComponent(pig);

    ButtonForceController controller = new ButtonForceController();
    controller.bind(router, config, world);
    router.handleButtonPress("move right");
    router.handleButtonPress("jump");
    router.handleButtonPress("diagonal boost");

    Vector externalForce = pig.getExternalForceTotal();
    assertEquals(400, externalForce.x(), EPS,
        "x-forces should sum: 300 + 0 + 100 = 400");
    assertEquals(500, externalForce.y(), EPS,
        "y-forces should sum: 0 + 400 + 100 = 500");
  }

  @Test
  void bind_releasingOneButtonKeepsOthersActive() {
    GameConfig config = new TestGameConfig("test", "BigPig",
        List.of(new ComponentInfo("BigPig", "", 10.0,
            List.of(
                new ButtonInfo("Move Right", 300, 0),
                new ButtonInfo("Jump", 0, 400)))),
        List.of(), List.of());

    PigComponent pig = newPig("BigPig");
    world.addComponent(pig);

    ButtonForceController controller = new ButtonForceController();
    controller.bind(router, config, world);
    router.handleButtonPress("move right");
    router.handleButtonPress("jump");
    router.handleButtonRelease("move right");

    Vector externalForce = pig.getExternalForceTotal();
    assertEquals(0, externalForce.x(), EPS,
        "Released force should be removed.");
    assertEquals(400, externalForce.y(), EPS,
        "Un-released force should still be active.");
  }

  @Test
  void bind_componentWithNoButtons_noBindingsCreated() {
    GameConfig config = new TestGameConfig("test", "",
        List.of(new ComponentInfo("Frame", "", 2.0, List.of())),
        List.of(), List.of());

    ButtonForceController controller = new ButtonForceController();
    controller.bind(router, config, world);

    // No assertions needed beyond no exception — no buttons to press.
  }

  private RelativeDirectionDriveComponent newDrive(String displayName) {
    return new RelativeDirectionDriveComponent(
        0, 0, 0, 10.0, 0.3, 100.0, displayName, 60.0, 60.0, new Vector(1, 0));
  }

  private PigComponent newPig(String componentType) {
    return new PigComponent(0, 0, 0,
        ConvexPolygon.axisAlignedBox(-30, -30, 60, 60),
        "", 0.3, 10.0, 1.0, 0.5, true, 100, componentType);
  }
}
