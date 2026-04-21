package oogasalad.model.engine.internal.joints;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Rigid distance link ({@link DistanceJoint} with zero stiffness).
 *
 * <p><strong>Triangle rigidity (SSS):</strong> In 2D, three pairwise distance constraints between the
 * same three bodies fix all three edge lengths of a triangle, which pins the shape (up to
 * congruence) and removes the extra rotational freedom that a single bar between two particles
 * leaves. Use {@link #registerRigidTriangle(World, GameComponent, GameComponent, GameComponent)}
 * when you want that behavior in one call; when edges are added incrementally (e.g. grid neighbors
 * first, then a closing chord), add each {@link RigidJoint} separately so joints are not
 * duplicated, or use {@link #registerRigidTriangle(World, GameComponent, GameComponent,
 * GameComponent, java.util.Set, String, String, String)} with stable vertex keys.
 */
public class RigidJoint extends DistanceJoint {

  public RigidJoint(GameComponent objectA, GameComponent objectB) {
    super(objectA, objectB, 0.0, 0.0);
  }

  public RigidJoint(GameComponent objectA, GameComponent objectB, double minDistance,
      double maxDistance) {
    super(objectA, objectB, minDistance, maxDistance, 0.0, 0.0);
  }

  public RigidJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB, double minDistance, double maxDistance) {
    super(objectA, objectB, localAnchorA, localAnchorB, minDistance, maxDistance, 0.0, 0.0);
  }

  public static void registerRigidTriangle(World world, GameComponent a, GameComponent b,
      GameComponent c) {
    Objects.requireNonNull(world, "world");
    Objects.requireNonNull(a, "a");
    Objects.requireNonNull(b, "b");
    Objects.requireNonNull(c, "c");
    world.addJoint(new RigidJoint(a, b));
    world.addJoint(new RigidJoint(b, c));
    world.addJoint(new RigidJoint(a, c));
  }

  public static void registerRigidTriangle(World world, List<GameComponent> abc) {
    Objects.requireNonNull(abc, "abc");
    if (abc.size() != 3) {
      throw new IllegalArgumentException(
          "registerRigidTriangle requires exactly 3 components, got " + abc.size());
    }
    registerRigidTriangle(world, abc.get(0), abc.get(1), abc.get(2));
  }

  public static void registerRigidTriangle(World world, GameComponent a, GameComponent b,
      GameComponent c, Set<String> existingCanonicalPairKeys, String keyA, String keyB,
      String keyC) {
    Objects.requireNonNull(world, "world");
    Objects.requireNonNull(a, "a");
    Objects.requireNonNull(b, "b");
    Objects.requireNonNull(c, "c");
    Objects.requireNonNull(existingCanonicalPairKeys, "existingCanonicalPairKeys");
    Objects.requireNonNull(keyA, "keyA");
    Objects.requireNonNull(keyB, "keyB");
    Objects.requireNonNull(keyC, "keyC");
    tryRegisterRigidBar(world, a, b, existingCanonicalPairKeys, keyA, keyB);
    tryRegisterRigidBar(world, b, c, existingCanonicalPairKeys, keyB, keyC);
    tryRegisterRigidBar(world, a, c, existingCanonicalPairKeys, keyA, keyC);
  }

  private static void tryRegisterRigidBar(World world, GameComponent x, GameComponent y,
      Set<String> keys, String keyX, String keyY) {
    String pk = canonicalPairKey(keyX, keyY);
    if (!keys.add(pk)) {
      return;
    }
    world.addJoint(new RigidJoint(x, y));
  }

  private static String canonicalPairKey(String keyA, String keyB) {
    return keyA.compareTo(keyB) < 0 ? keyA + "|" + keyB : keyB + "|" + keyA;
  }
}
