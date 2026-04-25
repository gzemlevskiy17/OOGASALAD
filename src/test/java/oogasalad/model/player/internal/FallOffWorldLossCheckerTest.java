package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FallOffWorldLossChecker}.
 *
 * <p>Focus: the post-spline behaviour where the checker falls back to tracking the
 * {@link DefaultTag#VEHICLE} tag if no {@link DefaultTag#PIGS} are present. This was the fix for
 * the "black viewport" issue on authored levels: a vehicle falling into open air in a pig-less
 * level used to leave the checker permanently {@code false}, so the game never ended.
 */
class FallOffWorldLossCheckerTest {

  private static final double WORLD_HEIGHT = 1000.0;

  @Test
  void hasLost_noTrackedObjects_returnsFalse() {
    FakeWorld world = new FakeWorld();
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_pigAboveWorldFloor_returnsFalse() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.PIGS.value(), 0, 500));
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_allPigsBelowWorldFloor_returnsTrue() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.PIGS.value(), 0, WORLD_HEIGHT + 10));
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertTrue(checker.hasLost(world));
  }

  @Test
  void hasLost_somePigsAboveWorldFloor_returnsFalse() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.PIGS.value(), 0, WORLD_HEIGHT + 10));
    world.addComponent(new TaggedBody(DefaultTag.PIGS.value(), 100, 500));
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_noPigs_vehicleAboveFloor_returnsFalse() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.VEHICLE.value(), 0, 500));
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_noPigs_vehicleBelowFloor_returnsTrue() {
    // Regression for the "black viewport / never loses" bug: pig-less levels must still lose
    // when the vehicle falls past the world floor.
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.VEHICLE.value(), 0, WORLD_HEIGHT + 50));
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertTrue(checker.hasLost(world));
  }

  @Test
  void hasLost_pigsTakePrecedenceOverVehicle_whenBothPresent() {
    // When pigs exist the checker should ignore the vehicle — a winning run with the vehicle
    // fallen off (e.g. a suicide launch) shouldn't flip the result if the pigs are still alive.
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.PIGS.value(), 0, 500));
    world.addComponent(new TaggedBody(DefaultTag.VEHICLE.value(), 100, WORLD_HEIGHT + 500));
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_pigExactlyAtWorldFloor_returnsFalse() {
    // Boundary: y == worldHeight is still considered "in-world" per the <= comparison.
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.PIGS.value(), 0, WORLD_HEIGHT));
    FallOffWorldLossChecker checker = new FallOffWorldLossChecker(WORLD_HEIGHT);

    assertFalse(checker.hasLost(world));
  }

  // --- Test doubles ---

  private static final class TaggedBody extends GameComponent {
    TaggedBody(String tag, double x, double y) {
      super(x, y, 0, 1, 0);
      setTag(tag);
      setPolygon(ConvexPolygon.axisAlignedBox(x - 5, y - 5, 10, 10));
    }
  }

  private static final class FakeWorld implements World {
    private final List<GameComponent> components = new ArrayList<>();
    @Override public List<GameComponent> getComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getAllComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getActiveComponents() { return List.copyOf(components); }
    @Override public List<Joint> getJoints() { return List.of(); }
    @Override public void addComponent(GameComponent c) { components.add(c); }
    @Override public void removeComponent(GameComponent c) { components.remove(c); }
    @Override public void clear() { components.clear(); }
    @Override public void addJoint(Joint j) {}
    @Override public void removeJoint(Joint j) {}
  }
}
