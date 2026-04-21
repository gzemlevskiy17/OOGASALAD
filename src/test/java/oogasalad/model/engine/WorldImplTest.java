package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorldImplTest {

  private WorldImpl world;
  private GameComponent componentA;
  private GameComponent componentB;

  @BeforeEach
  void setUp() {
    world = new WorldImpl();
    componentA = new TerrainBlock(0, 0, 1, 1, "a.png", true, 0.5);
    componentB = new TerrainBlock(1, 0, 1, 1, "b.png", false, 0.4);
  }

  @Test
  void addComponent_registersInAllAndActiveCollections() {
    world.addComponent(componentA);

    assertEquals(1, world.getAllComponents().size());
    assertEquals(1, world.getActiveComponents().size());
    assertEquals(1, world.getComponents().size());
    assertSame(componentA, world.getAllComponents().iterator().next());
    assertSame(componentA, world.getActiveComponents().iterator().next());
    assertSame(componentA, world.getComponents().iterator().next());
  }

  @Test
  void deactivateAndActivateAll_keepRegisteredComponentsButUpdateActiveView() {
    world.addComponent(componentA);
    world.addComponent(componentB);

    world.deactivateAll();

    assertEquals(2, world.getAllComponents().size());
    assertTrue(world.getActiveComponents().isEmpty());
    assertTrue(world.getComponents().isEmpty());
    assertFalse(componentA.isActive());
    assertFalse(componentB.isActive());

    world.activateAll();

    assertEquals(2, world.getActiveComponents().size());
    assertEquals(2, world.getComponents().size());
    assertTrue(componentA.isActive());
    assertTrue(componentB.isActive());
  }

  @Test
  void destroyComponent_unregistersAndDeactivatesComponent() {
    world.addComponent(componentA);
    world.destroyComponent(componentA);
    assertFalse(componentA.isActive());
    assertTrue(world.getAllComponents().isEmpty());
    assertTrue(world.getActiveComponents().isEmpty());
    assertTrue(world.getComponents().isEmpty());
  }

  @Test
  void clear_removesComponentsAndJoints() {
    world.addComponent(componentA);
    world.addComponent(componentB);
    world.addJoint(new FakeJoint(componentA, componentB));
    world.clear();
    assertTrue(world.getAllComponents().isEmpty());
    assertTrue(world.getActiveComponents().isEmpty());
    assertTrue(world.getComponents().isEmpty());
    assertTrue(world.getJoints().isEmpty());
  }

  @Test
  void getComponentsByTag_returnsRegisteredComponentsWithMatchingTagInOrder() {
    world.addComponent(componentA);
    world.addComponent(componentB);
    assertIterableEquals(List.of(componentA, componentB), world.getComponentsByTag(DefaultTag.TERRAIN.value()));
    assertTrue(world.getComponentsByTag(DefaultTag.VEHICLE.value()).isEmpty());
  }

  @Test
  void getObjectsByTag_filtersMixedWorldUsingDefaultAndAuthoringTags() {
    GameComponent terrain = new TerrainBlock(0, 0, 2, 1, "ground.png", true, 0.5);
    GameComponent pig = new FakeComponent(1, 0, DefaultTag.DYNAMIC.value());
    GameComponent checkpointA = new FakeComponent(2, 0, "checkpoint");
    GameComponent checkpointB = new FakeComponent(3, 0, "checkpoint");
    GameComponent hazard = new FakeComponent(4, 0, "hazard");

    world.addComponent(terrain);
    world.addComponent(pig);
    world.addComponent(checkpointA);
    world.addComponent(checkpointB);
    world.addComponent(hazard);

    assertIterableEquals(List.of(checkpointA, checkpointB), world.getObjectsByTag("checkpoint"));
    assertIterableEquals(List.of(terrain), world.getObjectsByTag(DefaultTag.TERRAIN.value()));
    assertIterableEquals(List.of(pig), world.getObjectsByTag(DefaultTag.DYNAMIC.value()));
    assertTrue(world.getObjectsByTag("missing-tag").isEmpty());
  }

  private record FakeJoint(GameComponent objectA, GameComponent objectB) implements Joint {
    @Override
    public GameComponent getComponentA() {return objectA;}
    @Override
    public GameComponent getComponentB() {return objectB;}
  }

  private static class FakeComponent extends DynamicComponent {
    FakeComponent(double x, double y, String tag) {
      super(x, y, 0, 1.0, 0.5);
      setState(new ObjectState(
          new Vector(x, y),
          new Vector(0, 0),
          new Vector(0, 0),
          0,
          0.0,
          0.0
      ));
      setTag(tag);
    }
  }
}
