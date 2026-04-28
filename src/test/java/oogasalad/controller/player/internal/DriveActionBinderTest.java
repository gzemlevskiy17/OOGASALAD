package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.RelativeDirectionDriveComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for DriveActionBinder, which binds controllable drive components to input router buttons.
 *
 * @author Paulina Vvedenskaya
 */
class DriveActionBinderTest {

  private World world;
  private InputRouterImpl router;
  private DriveActionBinder binder;

  @BeforeEach
  void setUp() {
    world = new WorldImpl();
    router = new InputRouterImpl();
    binder = new DriveActionBinder();
  }

  @Test
  void collectToolIds_emptyWorld_returnsEmpty() {
    Set<String> ids = binder.collectToolIds(world);

    assertTrue(ids.isEmpty());
  }

  @Test
  void collectToolIds_oneDriveComponent_returnsItsId() {
    world.addComponent(newDrive("Engine", true));

    Set<String> ids = binder.collectToolIds(world);

    assertEquals(Set.of("engine"), ids);
  }

  @Test
  void collectToolIds_nonPlayerControllableDrive_excluded() {
    world.addComponent(newDrive("Engine", false));

    Set<String> ids = binder.collectToolIds(world);

    assertTrue(ids.isEmpty(),
        "Non-player-controllable drives should not produce a tool ID.");
  }

  @Test
  void collectToolIds_twoDrivesWithSameName_returnsSingleId() {
    world.addComponent(newDrive("Fan", true));
    world.addComponent(newDrive("Fan", true));

    Set<String> ids = binder.collectToolIds(world);

    assertEquals(1, ids.size());
    assertTrue(ids.contains("fan"));
  }

  @Test
  void collectToolIds_twoDifferentDrives_returnsBothIds() {
    world.addComponent(newDrive("Engine", true));
    world.addComponent(newDrive("Fan", true));

    Set<String> ids = binder.collectToolIds(world);

    assertEquals(Set.of("engine", "fan"), ids);
  }

  @Test
  void collectToolIds_nonDriveableObject_excluded() {
    world.addComponent(newStructural("Frame"));

    Set<String> ids = binder.collectToolIds(world);

    assertTrue(ids.isEmpty(),
        "Non-Driveable objects should not produce tool IDs.");
  }

  @Test
  void bind_singleDrive_activatesOnPress() {
    RelativeDirectionDriveComponent engine = newDrive("Engine", true);
    world.addComponent(engine);

    binder.bind(router, world);
    router.handleButtonPress("engine");

    assertTrue(engine.isDriveEnabled(),
        "Pressing the bound button should activate the drive.");
  }

  @Test
  void bind_singleDrive_deactivatesOnRelease() {
    RelativeDirectionDriveComponent engine = newDrive("Engine", true);
    world.addComponent(engine);

    binder.bind(router, world);
    router.handleButtonPress("engine");
    router.handleButtonRelease("engine");

    assertFalse(engine.isDriveEnabled(),
        "Releasing the bound button should deactivate the drive.");
  }

  @Test
  void bind_twoFans_bothActivateOnSinglePress() {
    RelativeDirectionDriveComponent fan1 = newDrive("Fan", true);
    RelativeDirectionDriveComponent fan2 = newDrive("Fan", true);
    world.addComponent(fan1);
    world.addComponent(fan2);

    binder.bind(router, world);
    router.handleButtonPress("fan");

    assertTrue(fan1.isDriveEnabled(), "First fan should be activated.");
    assertTrue(fan2.isDriveEnabled(), "Second fan should be activated.");
  }

  @Test
  void bind_twoFans_bothDeactivateOnRelease() {
    RelativeDirectionDriveComponent fan1 = newDrive("Fan", true);
    RelativeDirectionDriveComponent fan2 = newDrive("Fan", true);
    world.addComponent(fan1);
    world.addComponent(fan2);

    binder.bind(router, world);
    router.handleButtonPress("fan");
    router.handleButtonRelease("fan");

    assertFalse(fan1.isDriveEnabled(), "First fan should be deactivated.");
    assertFalse(fan2.isDriveEnabled(), "Second fan should be deactivated.");
  }

  @Test
  void bind_nonPlayerControllable_notBound() {
    RelativeDirectionDriveComponent engine = newDrive("Engine", false);
    world.addComponent(engine);

    binder.bind(router, world);
    router.handleButtonPress("engine");

    assertFalse(engine.isDriveEnabled(),
        "Non-controllable drives should not be bound.");
  }

  @Test
  void bind_independentDrives_activateIndependently() {
    RelativeDirectionDriveComponent engine = newDrive("Engine", true);
    RelativeDirectionDriveComponent fan = newDrive("Fan", true);
    world.addComponent(engine);
    world.addComponent(fan);

    binder.bind(router, world);
    router.handleButtonPress("engine");

    assertTrue(engine.isDriveEnabled());
    assertFalse(fan.isDriveEnabled(),
        "Pressing engine should not activate fan.");
  }

  private RelativeDirectionDriveComponent newDrive(String displayName, boolean playerControllable) {
    RelativeDirectionDriveComponent drive = new RelativeDirectionDriveComponent(
        0, 0, 0, 10.0, 0.3, 100.0, displayName, 60.0, 60.0, new Vector(1, 0));
    drive.setPlayerControllable(playerControllable);
    return drive;
  }

  private StructuralComponent newStructural(String displayName) {
    return new StructuralComponent(
        0, 0, 0,
        ConvexPolygon.axisAlignedBox(-30, -30, 60, 60),
        "",
        0.3, 1.5,
        1.5 * (60.0 * 60.0 + 60.0 * 60.0) / 12.0,
        0.5, true, 100.0,
        displayName, 60.0, 60.0);
  }
}
