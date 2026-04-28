package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.controller.player.internal.parts.VehiclePartRegistry;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import oogasalad.model.engine.internal.joints.RigidJoint;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Main character (pig) rigid attachment to the vehicle — one assertion per test method.
 */
class MainCharacterVehicleAttachmentTest {

  private static final double START_X = 100.0;
  private static final double START_Y = 200.0;
  private static final double CELL = VehicleFactory.CELL_SIZE;

  private static boolean rigidConnects(Joint j, GameComponent a, GameComponent b) {
    if (!(j instanceof RigidJoint)) {
      return false;
    }
    return (j.getComponentA() == a && j.getComponentB() == b)
        || (j.getComponentA() == b && j.getComponentB() == a);
  }

  private static World build(VehicleLayout layout) {
    World world = new WorldImpl();
    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);
    return world;
  }

  private static PigComponent pigAt(World world, double worldX, double worldY) {
    return world.getComponents().stream()
        .filter(PigComponent.class::isInstance)
        .map(PigComponent.class::cast)
        .filter(p -> Math.abs(p.x() - worldX) < 1e-9 && Math.abs(p.y() - worldY) < 1e-9)
        .findFirst()
        .orElseThrow();
  }

  private static StructuralComponent frameAt(World world, double worldX, double worldY) {
    return world.getComponents().stream()
        .filter(StructuralComponent.class::isInstance)
        .map(StructuralComponent.class::cast)
        .filter(s -> "Frame".equals(s.getDisplayName()))
        .filter(f -> Math.abs(f.x() - worldX) < 1e-9 && Math.abs(f.y() - worldY) < 1e-9)
        .findFirst()
        .orElseThrow();
  }

  @Test
  void pigNorthOfFrame_hasRigidJointToFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(1, 3, "Pig")
    )));
    PigComponent pig = pigAt(world, START_X + 3 * CELL, START_Y + 1 * CELL);
    StructuralComponent frame = frameAt(world, START_X + 3 * CELL, START_Y + 2 * CELL);
    assertTrue(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, frame)));
  }

  @Test
  void pigSouthOfFrame_hasRigidJointToFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(3, 3, "Pig")
    )));
    PigComponent pig = pigAt(world, START_X + 3 * CELL, START_Y + 3 * CELL);
    StructuralComponent frame = frameAt(world, START_X + 3 * CELL, START_Y + 2 * CELL);
    assertTrue(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, frame)));
  }

  @Test
  void pigWestOfFrame_hasRigidJointToFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 2, "Pig")
    )));
    PigComponent pig = pigAt(world, START_X + 2 * CELL, START_Y + 2 * CELL);
    StructuralComponent frame = frameAt(world, START_X + 3 * CELL, START_Y + 2 * CELL);
    assertTrue(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, frame)));
  }

  @Test
  void pigEastOfFrame_hasRigidJointToFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 4, "Pig")
    )));
    PigComponent pig = pigAt(world, START_X + 4 * CELL, START_Y + 2 * CELL);
    StructuralComponent frame = frameAt(world, START_X + 3 * CELL, START_Y + 2 * CELL);
    assertTrue(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, frame)));
  }

  @Test
  void pigAdjacentWestOfFrame_hasAtLeastTwoRigidJointsToThatFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 2, "Pig")
    )));
    PigComponent pig = pigAt(world, START_X + 2 * CELL, START_Y + 2 * CELL);
    StructuralComponent frame = frameAt(world, START_X + 3 * CELL, START_Y + 2 * CELL);
    long count = world.getJoints().stream()
        .filter(RigidJoint.class::isInstance)
        .filter(j -> rigidConnects(j, pig, frame))
        .count();
    assertTrue(count >= 2);
  }

  @Test
  void pigWestOfWoodWheel_hasRigidJointToWheel() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(2, 2, "Pig"),
        new PlacedComponent(2, 3, "Wood Wheel")
    )));
    PigComponent pig = pigAt(world, START_X + 2 * CELL, START_Y + 2 * CELL);
    StructuralComponent wheel = world.getComponents().stream()
        .filter(StructuralComponent.class::isInstance)
        .map(StructuralComponent.class::cast)
        .filter(s -> "Wood Wheel".equals(s.getDisplayName()))
        .filter(w -> Math.abs(w.x() - (START_X + 3 * CELL)) < 1e-9
            && Math.abs(w.y() - (START_Y + 2 * CELL)) < 1e-9)
        .findFirst()
        .orElseThrow();
    assertTrue(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, wheel)));
  }

  @Test
  void pigWestOfWoodWheel_isNeverOnRevoluteJoint() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(2, 2, "Pig"),
        new PlacedComponent(2, 3, "Wood Wheel")
    )));
    PigComponent pig = pigAt(world, START_X + 2 * CELL, START_Y + 2 * CELL);
    assertFalse(world.getJoints().stream()
        .filter(RevoluteJoint.class::isInstance)
        .anyMatch(j -> j.getComponentA() == pig || j.getComponentB() == pig));
  }

  @Test
  void pigAlone_throwsIllegalStateException() {
    assertThrows(IllegalStateException.class,
        () -> build(new VehicleLayout(List.of(new PlacedComponent(2, 2, "Pig")))));
  }

  @Test
  void bigPigSameCellAsFrame_sharesWorldXWithFrame() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());
    World world = new WorldImpl();
    new VehicleFactory(registry).buildVehicle(new VehicleLayout(List.of(
        new PlacedComponent(1, 1, "Frame"),
        new PlacedComponent(1, 1, "BigPig")
    )), world, START_X, START_Y);
    PigComponent pig = pigAt(world, START_X + CELL, START_Y + CELL);
    StructuralComponent frame = frameAt(world, START_X + CELL, START_Y + CELL);
    assertEquals(frame.x(), pig.x(), 1e-9);
  }

  @Test
  void bigPigSameCellAsFrame_sharesWorldYWithFrame() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());
    World world = new WorldImpl();
    new VehicleFactory(registry).buildVehicle(new VehicleLayout(List.of(
        new PlacedComponent(1, 1, "Frame"),
        new PlacedComponent(1, 1, "BigPig")
    )), world, START_X, START_Y);
    PigComponent pig = pigAt(world, START_X + CELL, START_Y + CELL);
    StructuralComponent frame = frameAt(world, START_X + CELL, START_Y + CELL);
    assertEquals(frame.y(), pig.y(), 1e-9);
  }

  @Test
  void bigPigSameCellAsFrame_hasRigidJointToFrame() {
    VehiclePartRegistry registry = new VehiclePartRegistry();
    registry.registerFromConfig(TestGameConfig.badPiggies());
    World world = new WorldImpl();
    new VehicleFactory(registry).buildVehicle(new VehicleLayout(List.of(
        new PlacedComponent(1, 1, "Frame"),
        new PlacedComponent(1, 1, "BigPig")
    )), world, START_X, START_Y);
    PigComponent pig = pigAt(world, START_X + CELL, START_Y + CELL);
    StructuralComponent frame = frameAt(world, START_X + CELL, START_Y + CELL);
    assertTrue(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, frame)));
  }

  @Test
  void twoPigsOnSeparateFrames_bothHaveRigidJointToSomeFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(0, 0, "Frame"),
        new PlacedComponent(0, 0, "Pig"),
        new PlacedComponent(0, 2, "Frame"),
        new PlacedComponent(0, 2, "Pig")
    )));
    List<PigComponent> pigs = world.getComponents().stream()
        .filter(PigComponent.class::isInstance)
        .map(PigComponent.class::cast)
        .toList();
    List<StructuralComponent> frames = world.getComponents().stream()
        .filter(StructuralComponent.class::isInstance)
        .map(StructuralComponent.class::cast)
        .filter(s -> "Frame".equals(s.getDisplayName()))
        .toList();
    long linkedPigs = pigs.stream()
        .filter(pig -> frames.stream().anyMatch(f -> world.getJoints().stream()
            .filter(RigidJoint.class::isInstance)
            .anyMatch(j -> rigidConnects(j, pig, f))))
        .count();
    assertEquals(2L, linkedPigs);
  }

  @Test
  void pigWithNorthAndEastFrames_linksToNorthFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(1, 3, "Frame"),
        new PlacedComponent(2, 4, "Frame"),
        new PlacedComponent(2, 3, "Pig")
    )));
    PigComponent pig = pigAt(world, START_X + 3 * CELL, START_Y + 2 * CELL);
    StructuralComponent northFrame = frameAt(world, START_X + 3 * CELL, START_Y + 1 * CELL);
    assertTrue(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, northFrame)));
  }

  @Test
  void pigWithNorthAndEastFrames_doesNotLinkToEastFrame() {
    World world = build(new VehicleLayout(List.of(
        new PlacedComponent(1, 3, "Frame"),
        new PlacedComponent(2, 4, "Frame"),
        new PlacedComponent(2, 3, "Pig")
    )));
    PigComponent pig = pigAt(world, START_X + 3 * CELL, START_Y + 2 * CELL);
    StructuralComponent eastFrame = frameAt(world, START_X + 4 * CELL, START_Y + 2 * CELL);
    assertFalse(world.getJoints().stream().anyMatch(j -> rigidConnects(j, pig, eastFrame)));
  }
}