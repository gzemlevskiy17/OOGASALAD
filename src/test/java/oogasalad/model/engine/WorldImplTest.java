package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.api.Joint;
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

  private record FakeJoint(GameComponent objectA, GameComponent objectB) implements Joint {
    @Override
    public GameComponent getComponentA() {
      return objectA;
    }

    @Override
    public GameComponent getComponentB() {
      return objectB;
    }
  }
}
