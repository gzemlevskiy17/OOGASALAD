package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import oogasalad.model.engine.internal.joints.RigidJoint;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.model.player.api.VehicleLayout;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the bug where a Pig placed in the same grid cell as a Frame is moved to an
 * adjacent cell during vehicle assembly, so it renders above the frame instead of inside it.
 */
class VehicleFactoryPigPlacementTest {

  private static final double START_X = 380.0;
  private static final double START_Y = 330.0;
  private static final double CELL_SIZE = 60.0;

  private PigComponent findPig(World world) {
    return world.getComponents().stream()
        .filter(PigComponent.class::isInstance)
        .map(PigComponent.class::cast)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No PigComponent in world"));
  }

  private StructuralComponent findFrame(World world) {
    return world.getComponents().stream()
        .filter(StructuralComponent.class::isInstance)
        .map(StructuralComponent.class::cast)
        .filter(s -> "Frame".equals(s.getDisplayName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No Frame StructuralComponent in world"));
  }

  @Test
  void pigAndFrameInSameCell_resultInSameWorldPosition() {
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 3, "Pig")
    ));
    World world = new WorldImpl();

    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    PigComponent pig = findPig(world);
    StructuralComponent frame = findFrame(world);

    assertEquals(frame.getX(), pig.getX(), 1e-9,
        "Pig X should match Frame X when placed in same cell");
    assertEquals(frame.getY(), pig.getY(), 1e-9,
        "Pig Y should match Frame Y when placed in same cell");
  }

  @Test
  void pigInSameCellAsFrame_lands_atThatCellsWorldCoordinates() {
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 3, "Pig")
    ));
    World world = new WorldImpl();

    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    PigComponent pig = findPig(world);

    double expectedX = START_X + 3 * CELL_SIZE;
    double expectedY = START_Y + 2 * CELL_SIZE;
    assertEquals(expectedX, pig.getX(), 1e-9,
        "Pig should be at cell (2,3) world X");
    assertEquals(expectedY, pig.getY(), 1e-9,
        "Pig should be at cell (2,3) world Y");
  }

  @Test
  void pigSpritePolygon_overlapsFrameSpritePolygon() {
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 3, "Pig")
    ));
    World world = new WorldImpl();

    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    PigComponent pig = findPig(world);
    StructuralComponent frame = findFrame(world);
    pig.updatePolygon();
    frame.updatePolygon();

    double[] pigBounds = polygonBounds(pig);
    double[] frameBounds = polygonBounds(frame);

    boolean overlap = pigBounds[0] < frameBounds[2]
        && pigBounds[2] > frameBounds[0]
        && pigBounds[1] < frameBounds[3]
        && pigBounds[3] > frameBounds[1];

    assertTrue(overlap,
        "Pig polygon should overlap Frame polygon — pig=%s frame=%s".formatted(
            boundsToString(pigBounds), boundsToString(frameBounds)));
  }

  @Test
  void multipleFramesWithPig_pigStaysWithItsFrame() {
    // Column 3 has three stacked components: two frames and a pig in the middle frame's cell.
    // After assembly, the pig should still share a cell with a frame (any frame is fine).
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(1, 3, "Frame"),
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 3, "Pig")
    ));
    World world = new WorldImpl();

    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    PigComponent pig = findPig(world);

    boolean sharesCellWithFrame = world.getComponents().stream()
        .filter(StructuralComponent.class::isInstance)
        .map(StructuralComponent.class::cast)
        .filter(s -> "Frame".equals(s.getDisplayName()))
        .anyMatch(f -> Math.abs(f.getX() - pig.getX()) < 1e-9
            && Math.abs(f.getY() - pig.getY()) < 1e-9);

    assertTrue(sharesCellWithFrame,
        "Pig must share world coordinates with some Frame it was placed inside");
  }

  @Test
  void pigInSameCellAsFrame_getsRigidJointToFrame() {
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 3, "Frame"),
        new PlacedComponent(2, 3, "Pig")
    ));
    World world = new WorldImpl();

    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    PigComponent pig = findPig(world);
    StructuralComponent frame = findFrame(world);

    boolean hasPigFrameJoint = world.getJoints().stream()
        .filter(RigidJoint.class::isInstance)
        .anyMatch(j -> jointConnects(j, pig, frame));

    assertTrue(hasPigFrameJoint,
        "Pig and Frame in the same cell should be connected by a RigidJoint");
  }

  @Test
  void smallPigInSameCellAsFrame_isBuiltAsPigAndRigidlyAttached() {
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 2, "Frame"),
        new PlacedComponent(2, 2, "Small Pig")
    ));
    World world = new WorldImpl();

    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    long pigCount = world.getObjectsByTag(DefaultTag.PIGS.value()).size();
    assertEquals(1, pigCount, "Small Pig should be instantiated as a pig-tagged component");

    PigComponent pig = findPig(world);
    GameComponent frame = findComponentAt(world, START_X + 2 * CELL_SIZE, START_Y + 2 * CELL_SIZE,
        "Frame");
    assertNotNull(frame, "Frame should exist at the small pig host cell");

    boolean hasPigFrameJoint = world.getJoints().stream()
        .filter(RigidJoint.class::isInstance)
        .anyMatch(j -> jointConnects(j, pig, frame));
    assertTrue(hasPigFrameJoint,
        "Small Pig and Frame in the same cell should be connected by a RigidJoint");
  }

  private boolean jointConnects(Joint j, GameComponent a, GameComponent b) {
    return (j.getComponentA() == a && j.getComponentB() == b)
        || (j.getComponentA() == b && j.getComponentB() == a);
  }

  private GameComponent findComponentAt(World world, double x, double y, String displayName) {
    return world.getComponents().stream()
        .filter(c -> Math.abs(c.getX() - x) < 1e-9 && Math.abs(c.getY() - y) < 1e-9)
        .filter(c -> !(c instanceof PigComponent))
        .filter(c -> {
          if (c instanceof StructuralComponent sc) {
            return displayName.equals(sc.getDisplayName());
          }
          if (c instanceof oogasalad.model.engine.internal.components.DriveComponent dc) {
            return displayName.equals(dc.getDisplayName());
          }
          return false;
        })
        .findFirst()
        .orElse(null);
  }

  @Test
  void fanAttachedToFrame_getsWeldStyleDualRigidConstraints() {
    VehicleLayout layout = new VehicleLayout(List.of(
        new PlacedComponent(2, 0, "Fan"),
        new PlacedComponent(2, 1, "Frame"),
        new PlacedComponent(2, 2, "Frame"),
        new PlacedComponent(3, 1, "Wheel"),
        new PlacedComponent(3, 2, "Wheel")
    ));
    World world = new WorldImpl();
    new VehicleFactory().buildVehicle(layout, world, START_X, START_Y);

    GameComponent fan = findComponentAt(world, START_X, START_Y + 2 * CELL_SIZE, "Fan");
    GameComponent attachedFrame = findComponentAt(world, START_X + CELL_SIZE,
        START_Y + 2 * CELL_SIZE, "Frame");
    assertNotNull(fan, "Fan should be instantiated at expected cell");
    assertNotNull(attachedFrame, "Attached frame should be instantiated at expected cell");

    long rigidJointCount = world.getJoints().stream()
        .filter(RigidJoint.class::isInstance)
        .filter(j -> jointConnects(j, fan, attachedFrame))
        .count();
    assertTrue(rigidJointCount >= 2,
        "Fan and attached frame should have two rigid constraints to resist swing");
  }

  private double[] polygonBounds(GameComponent c) {
    var pts = c.getPolygon().getPoints();
    double minX = pts.stream().mapToDouble(p -> p.x()).min().orElseThrow();
    double maxX = pts.stream().mapToDouble(p -> p.x()).max().orElseThrow();
    double minY = pts.stream().mapToDouble(p -> p.y()).min().orElseThrow();
    double maxY = pts.stream().mapToDouble(p -> p.y()).max().orElseThrow();
    return new double[]{minX, minY, maxX, maxY};
  }

  private String boundsToString(double[] b) {
    return "(%.1f,%.1f)-(%.1f,%.1f)".formatted(b[0], b[1], b[2], b[3]);
  }
}
