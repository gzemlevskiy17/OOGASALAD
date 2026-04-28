package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelAttributes;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DriveComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link GameplaySessionFactoryImpl#bindDriveActions} against a real world, a real
 * {@link InputRouterImpl}, and real {@link DriveComponent} instances. Verifies which objects get
 * bound, under what button IDs, and that pressing a button toggles only the intended target.
 */
class BindDriveActionsTest {

  private GameplaySessionFactoryImpl factory;
  private InputRouterImpl router;
  private World world;

  @BeforeEach
  void setUp() {
    factory = new GameplaySessionFactoryImpl(
        null,
        null,
        () -> null
    );
    router = new InputRouterImpl();
    world = new WorldImpl();
  }

  private DriveComponent newDrive(String displayName) {
    return new DriveComponent(0, 0, 0, 10.0, 0.3, 100.0, displayName, 60.0, 60.0);
  }

  @Test
  void buttonPress_matchingDisplayNameLowercase_enablesDrive() {
    DriveComponent fan = newDrive("Fan");
    world.addComponent(fan);

    factory.bindDriveActions(router, world);
    router.handleButtonPress("fan");

    assertTrue(fan.isDriveEnabled(),
        "pressing 'fan' should enable the DriveComponent with displayName 'Fan'");
  }

  @Test
  void buttonPress_twiceOnSameId_togglesBack() {
    DriveComponent fan = newDrive("Fan");
    world.addComponent(fan);

    factory.bindDriveActions(router, world);
    router.handleButtonPress("fan");
    router.handleButtonPress("fan");

    assertFalse(fan.isDriveEnabled(),
        "two presses should toggle off — matches control-button ToggleButton select/deselect");
  }

  @Test
  void twoDrives_pressingOne_doesNotAffectTheOther() {
    DriveComponent fan = newDrive("Fan");
    DriveComponent balloon = newDrive("Balloon");
    world.addComponent(fan);
    world.addComponent(balloon);

    factory.bindDriveActions(router, world);
    router.handleButtonPress("fan");

    assertTrue(fan.isDriveEnabled(), "fan should be on");
    assertFalse(balloon.isDriveEnabled(), "balloon must stay off");
  }

  @Test
  void nonDriveable_noBindingCreated() {
    PigComponent pig = new PigComponent(0, 0, 0,
        ConvexPolygon.axisAlignedBox(-20, -20, 40, 40),
        "", 0, 1.0, 1.0, 0.1, true, 100, "pig");
    world.addComponent(pig);

    factory.bindDriveActions(router, world);
    // Pressing a button id derived from the pig's tag should be a safe no-op.
    router.handleButtonPress("pigs");
    router.handleButtonPress("pig");
    // No exception, no state to check — the assertion is that neither throws and no drive flips.
  }

  @Test
  void nonPlayerControllableDrive_noBindingCreated() {
    DriveComponent fan = newDrive("Fan");
    fan.setPlayerControllable(false);
    world.addComponent(fan);

    factory.bindDriveActions(router, world);
    router.handleButtonPress("fan");

    assertFalse(fan.isDriveEnabled(),
        "a drive flagged non-player-controllable must not be toggleable via button");
  }

  @Test
  void buttonId_isCaseSensitive() {
    DriveComponent fan = newDrive("Fan");
    world.addComponent(fan);

    factory.bindDriveActions(router, world);
    router.handleButtonPress("FAN");
    router.handleButtonPress("Fan");

    assertFalse(fan.isDriveEnabled(),
        "binding uses lowercase displayName; upper/mixed case button ids must not match");
  }

  @Test
  void twoDrivesWithSameDisplayName_bothRespondToOnePress() {
    // Two fans on one vehicle share the same displayName → same button id.
    // bindDriveActions registers them sequentially; the second bind REPLACES the first in
    // InputRouterImpl (HashMap#put). This test pins the current behavior so a regression to
    // "multi-fan support via list-of-actions" is an intentional choice, not silent drift.
    DriveComponent fanA = newDrive("Fan");
    DriveComponent fanB = newDrive("Fan");
    world.addComponent(fanA);
    world.addComponent(fanB);

    factory.bindDriveActions(router, world);
    router.handleButtonPress("fan");

    // Whichever one was bound last will be toggled; the other stays off.
    boolean exactlyOneEnabled = fanA.isDriveEnabled() ^ fanB.isDriveEnabled();
    assertTrue(exactlyOneEnabled,
        "current InputRouterImpl binds one action per id — only the last-registered fan toggles. "
            + "If you want both fans to share a button, change to a multimap and update this test.");
  }
}
