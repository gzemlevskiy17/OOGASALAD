package oogasalad.model.engine.internal.joints;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import oogasalad.model.engine.EngineError;
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

  /**
   * Creates a rigid joint that locks the initial anchor separation.
   */
  public RigidJoint(GameComponent objectA, GameComponent objectB) {
    super(objectA, objectB, 0.0, 0.0);
  }

  /**
   * Creates a rigid joint with an allowed distance band.
   */
  public RigidJoint(GameComponent objectA, GameComponent objectB, double minDistance,
      double maxDistance) {
    super(objectA, objectB, minDistance, maxDistance, 0.0, 0.0);
  }

  /**
   * Creates a rigid joint with explicit local anchors and a distance band.
   */
  public RigidJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB,
      double minDistance, double maxDistance) {
    super(objectA, objectB, localAnchorA, localAnchorB, minDistance, maxDistance, 0.0, 0.0);
  }

  /**
   * Registers three rigid bars on the three pairs of bodies: AB, BC, and AC.
   *
   * <p>Together they enforce the triangle side lengths at construction time (each
   * {@link RigidJoint}
   * captures the current anchor separation). That is the usual “three lengths lock the triangle” /
   * SSS rigidification used to brace an {@code A–B–C} linkage when all three edges should be rigid
   * bars.
   *
   * <p>Do not use this if two of the edges are already in the {@link World}, unless you intend
   * duplicate constraints — prefer adding only the missing edges in that case.
   *
   * @param world simulation world receiving the joints
   * @param a     first vertex of the triangle
   * @param b     second vertex
   * @param c     third vertex
   */
  public static void registerRigidTriangle(World world, GameComponent a, GameComponent b,
      GameComponent c) {
    Objects.requireNonNull(world, EngineError.NULL_PARAMETER.format("world"));
    Objects.requireNonNull(a, EngineError.NULL_PARAMETER.format("a"));
    Objects.requireNonNull(b, EngineError.NULL_PARAMETER.format("b"));
    Objects.requireNonNull(c, EngineError.NULL_PARAMETER.format("c"));
    world.addJoint(new RigidJoint(a, b));
    world.addJoint(new RigidJoint(b, c));
    world.addJoint(new RigidJoint(a, c));
  }

  /**
   * Same as {@link #registerRigidTriangle(World, GameComponent, GameComponent, GameComponent)} with
   * vertices taken in list order ({@code get(0)}, {@code get(1)}, {@code get(2)}).
   *
   * @throws IllegalArgumentException if the list size is not exactly 3
   */
  public static void registerRigidTriangle(World world, List<GameComponent> abc) {
    Objects.requireNonNull(abc, EngineError.NULL_PARAMETER.format("abc"));
    if (abc.size() != 3) {
      throw new IllegalArgumentException(
          EngineError.RIGID_TRIANGLE_LIST_SIZE.format(abc.size()));
    }
    registerRigidTriangle(world, abc.get(0), abc.get(1), abc.get(2));
  }

  /**
   * Registers the three rigid bars AB, BC, and AC, skipping any pair whose canonical undirected key
   * is already in {@code existingCanonicalPairKeys}. Intended when some edges were registered
   * earlier (e.g. cardinal grid bars) so only missing triangle sides are added—typically the chord.
   *
   * @param existingCanonicalPairKeys mutable set; updated when a new pair is registered (same format
   *                                  as elsewhere, e.g. lexicographic {@code "r,c|r2,c2"})
   * @param keyA                    stable key for vertex {@code a} (caller-defined)
   * @param keyB                    stable key for vertex {@code b}
   * @param keyC                    stable key for vertex {@code c}
   */
  public static void registerRigidTriangle(World world, GameComponent a, GameComponent b,
      GameComponent c, Set<String> existingCanonicalPairKeys, String keyA, String keyB,
      String keyC) {
    Objects.requireNonNull(world, EngineError.NULL_PARAMETER.format("world"));
    Objects.requireNonNull(a, EngineError.NULL_PARAMETER.format("a"));
    Objects.requireNonNull(b, EngineError.NULL_PARAMETER.format("b"));
    Objects.requireNonNull(c, EngineError.NULL_PARAMETER.format("c"));
    Objects.requireNonNull(existingCanonicalPairKeys,
        EngineError.NULL_PARAMETER.format("existingCanonicalPairKeys"));
    Objects.requireNonNull(keyA, EngineError.NULL_PARAMETER.format("keyA"));
    Objects.requireNonNull(keyB, EngineError.NULL_PARAMETER.format("keyB"));
    Objects.requireNonNull(keyC, EngineError.NULL_PARAMETER.format("keyC"));
    tryRegisterRigidBar(world, a, b, existingCanonicalPairKeys, keyA, keyB);
    tryRegisterRigidBar(world, b, c, existingCanonicalPairKeys, keyB, keyC);
    tryRegisterRigidBar(world, a, c, existingCanonicalPairKeys, keyA, keyC);
  }

  private static void tryRegisterRigidBar(World world, GameComponent x, GameComponent y,
      Set<String> keys, String keyX, String keyY) {
    String pk = canonicalPairKey(keyX, keyY);
    if (!keys.add(pk)) {return;}
    world.addJoint(new RigidJoint(x, y));
  }

  private static String canonicalPairKey(String keyA, String keyB) {
    return keyA.compareTo(keyB) < 0 ? keyA + "|" + keyB : keyB + "|" + keyA;
  }
}
