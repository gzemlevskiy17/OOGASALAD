package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MainCharacterDestroyedLossChecker}.
 *
 * @author Paulina Vvedenskaya
 */
class MainCharacterDestroyedLossCheckerTest {

  private static PigComponent makePig(String type, double maxHp) {
    double size = 40.0;
    return new PigComponent(300, 300, 0,
        ConvexPolygon.axisAlignedBox(280, 280, size, size),
        "", 0.3, 10, 10 * (size * size + size * size) / 12.0,
        0.5, true, maxHp, type);
  }

  @Test
  void hasLost_pigAtFullHp_returnsFalse() {
    FakeWorld world = new FakeWorld();
    world.addComponent(makePig("BigPig", 100));
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("BigPig");

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_pigAtZeroHp_returnsTrue() {
    FakeWorld world = new FakeWorld();
    PigComponent pig = makePig("BigPig", 100);
    pig.damage(100);
    world.addComponent(pig);
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("BigPig");

    assertTrue(checker.hasLost(world));
  }

  @Test
  void hasLost_pigDamagedButNotDestroyed_returnsFalse() {
    FakeWorld world = new FakeWorld();
    PigComponent pig = makePig("BigPig", 100);
    pig.damage(99);
    world.addComponent(pig);
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("BigPig");

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_noMainCharacterInWorld_returnsFalse() {
    FakeWorld world = new FakeWorld();
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("BigPig");

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_multiplePigsOneDestroyed_returnsTrue() {
    FakeWorld world = new FakeWorld();
    PigComponent alive = makePig("BigPig", 100);
    PigComponent dead = makePig("BigPig", 50);
    dead.damage(50);
    world.addComponent(alive);
    world.addComponent(dead);
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("BigPig");

    assertTrue(checker.hasLost(world));
  }

  @Test
  void hasLost_multiplePigsAllAlive_returnsFalse() {
    FakeWorld world = new FakeWorld();
    world.addComponent(makePig("BigPig", 100));
    world.addComponent(makePig("BigPig", 50));
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("BigPig");

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_nonDamageableMainCharacter_returnsFalse() {
    FakeWorld world = new FakeWorld();
    world.addComponent(new TaggedBody(DefaultTag.PIGS.value(), 100, 100));
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("");

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_pigRepairedBackFromZero_returnsFalseAfterRepair() {
    FakeWorld world = new FakeWorld();
    PigComponent pig = makePig("BigPig", 100);
    pig.damage(100);
    pig.repair(1);
    world.addComponent(pig);
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("BigPig");

    assertFalse(checker.hasLost(world));
  }

  @Test
  void hasLost_blankMainCharacterId_fallsBackToPigTag() {
    FakeWorld world = new FakeWorld();
    PigComponent pig = makePig("SmallPig", 30);
    pig.damage(30);
    world.addComponent(pig);
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker("");

    assertTrue(checker.hasLost(world));
  }

  @Test
  void hasLost_nullMainCharacterId_fallsBackToPigTag() {
    FakeWorld world = new FakeWorld();
    PigComponent pig = makePig("SmallPig", 30);
    pig.damage(30);
    world.addComponent(pig);
    MainCharacterDestroyedLossChecker checker =
        new MainCharacterDestroyedLossChecker(null);

    assertTrue(checker.hasLost(world));
  }

  private static final class TaggedBody extends StaticComponent {

    TaggedBody(String tag, double x, double y) {
      super(x, y, 0, ConvexPolygon.axisAlignedBox(x - 5, y - 5, 10, 10), tag, 0, 0);
    }
  }

  private static final class FakeWorld implements World {

    private final List<GameComponent> components = new ArrayList<>();

    @Override
    public List<GameComponent> getComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<GameComponent> getAllComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<GameComponent> getActiveComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<DynamicComponent> getActiveDynamicComponents() {
      return getActiveComponents().stream()
          .filter(DynamicComponent.class::isInstance)
          .map(DynamicComponent.class::cast)
          .toList();
    }

    @Override
    public List<Joint> getJoints() {
      return List.of();
    }

    @Override
    public void addComponent(GameComponent c) {
      components.add(c);
    }

    @Override
    public void removeComponent(GameComponent c) {
      components.remove(c);
    }

    @Override
    public void clear() {
      components.clear();
    }

    @Override
    public void addJoint(Joint j) {
    }

    @Override
    public void removeJoint(Joint j) {
    }

    @Override
    public PhysicsEnvironment getPhysicsEnvironment() {
      return PhysicsEnvironment.DEFAULT;
    }
  }
}
