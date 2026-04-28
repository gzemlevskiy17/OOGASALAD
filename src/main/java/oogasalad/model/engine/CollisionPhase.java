package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Broad-phase collision filtering, narrow-phase dispatch, impulse caching, and position
 * correction.
 *
 * @author Paulina Vvedenskaya
 */
class CollisionPhase {

  private static final Logger LOG = LogManager.getLogger(CollisionPhase.class);
  private static final int SOLVER_ITERATIONS = 8;

  private final World world;
  private final CollisionDetector collisionDetector;
  private final CollisionDispatcher collisionDispatcher;
  private final JointSolver jointSolver;
  private final Map<List<GameObject>, CachedImpulse> impulseCache;

  CollisionPhase(World world, CollisionDetector collisionDetector,
      CollisionDispatcher collisionDispatcher, JointSolver jointSolver) {
    this.world = world;
    this.collisionDetector = collisionDetector;
    this.collisionDispatcher = collisionDispatcher;
    this.jointSolver = jointSolver;
    this.impulseCache = new HashMap<>();
  }

  private static void applyDynamicDynamicCorrection(DynamicComponent objectA,
      DynamicComponent objectB, Collision collision) {
    if (objectA == collision.getIncidentObject()) {
      collision.setPenetration(collision.getPenetration().multiply(-1));
    }
    ImpulseSolverUtilities.applyPositionalCorrection(collision, objectA, objectB);
  }

  private static void applyDynamicStaticCorrection(DynamicComponent dynamicObject,
      StaticComponent staticObject, Collision collision) {
    if (dynamicObject == collision.getReferenceObject()) {
      collision.setPenetration(collision.getPenetration().multiply(-1));
    }
    ImpulseSolverUtilities.applyPositionalCorrection(collision, staticObject, dynamicObject);
  }

  private static boolean isDynamicDynamicPair(CollisionPair pair, GameObject objectA,
      GameObject objectB) {
    return pair.objectA().isAssignableFrom(objectA.getClass())
        && pair.objectB().isAssignableFrom(objectB.getClass())
        && DynamicComponent.class.isAssignableFrom(pair.objectA())
        && DynamicComponent.class.isAssignableFrom(pair.objectB());
  }

  private static boolean isDynamicStaticPair(CollisionPair pair, GameObject objectA,
      GameObject objectB) {
    if (!pair.objectA().isAssignableFrom(objectA.getClass())
        || !pair.objectB().isAssignableFrom(objectB.getClass())) {
      return false;
    }
    if (PassThroughComponent.class.isAssignableFrom(pair.objectB())) {
      return false;
    }
    return DynamicComponent.class.isAssignableFrom(pair.objectA())
        && StaticComponent.class.isAssignableFrom(pair.objectB());
  }

  private static boolean aabbsOverlap(GameObject objectA, GameObject objectB) {
    return SweptCollisionResolver.BoundingBox.from(objectA)
        .overlaps(SweptCollisionResolver.BoundingBox.from(objectB));
  }

  private static List<GameObject> canonicalPair(GameObject a, GameObject b) {
    List<GameObject> pair = new ArrayList<>(List.of(a, b));
    pair.sort(Comparator.comparing(o -> o.getClass().getName())
        .thenComparing(o -> o.hashCode()));
    return pair;
  }

  private static List<GameObject> pairKeyFor(Collision collision) {
    return canonicalPair(collision.getReferenceObject(), collision.getIncidentObject());
  }

  void solve(double elapsedTime) {
    List<Collision> collisions = detectCollisions();
    warmStartCollisions(collisions);
    solveVelocityIterations(collisions);
    solvePositionIterations(collisions, elapsedTime);
    updateImpulseCache(collisions);
  }

  private void warmStartCollisions(List<Collision> collisions) {
    for (Collision collision : collisions) {
      List<GameObject> pairKey = pairKeyFor(collision);
      CachedImpulse cached = impulseCache.get(pairKey);
      if (cached != null) {
        collision.setCachedNormalImpulse(cached.normalImpulse());
        collision.setCachedFrictionImpulse(cached.frictionImpulse());
        collision.setCachedNormal(cached.normal());
      }
    }
  }

  private void solveVelocityIterations(List<Collision> collisions) {
    for (int i = 0; i < SOLVER_ITERATIONS; i++) {
      jointSolver.applyVelocityConstraints();
      dispatchCollisions(collisions, true, i == 0);
    }
  }

  private void solvePositionIterations(List<Collision> collisions, double elapsedTime) {
    for (int i = 0; i < SOLVER_ITERATIONS; i++) {
      jointSolver.applyPositionConstraints(elapsedTime, 1);
      dispatchCollisions(collisions, false, false);
      updateDynamicPolygons();
    }
  }

  private void updateImpulseCache(List<Collision> collisions) {
    Set<List<GameObject>> activeKeys = new HashSet<>();
    for (Collision collision : collisions) {
      List<GameObject> pairKey = pairKeyFor(collision);
      storeImpulse(pairKey, collision);
      activeKeys.add(pairKey);
    }
    impulseCache.keySet().retainAll(activeKeys);
  }

  private List<Collision> detectCollisions() {
    List<Collision> collisions = new ArrayList<>();
    Set<List<GameObject>> seenPairs = new HashSet<>();
    Set<List<GameObject>> jointedPairs = buildJointedPairs();

    for (DynamicComponent objectA : world.getActiveDynamicComponents()) {
      for (GameObject objectB : world.getObjects()) {
        if (objectA == objectB) {
          continue;
        }
        List<GameObject> pairKey = canonicalPair(objectA, objectB);
        if (!seenPairs.add(pairKey)) {
          continue;
        }
        if (jointedPairs.contains(pairKey)) {
          continue;
        }
        if (aabbsOverlap(objectA, objectB)) {
          collisionDetector.detectCollision(objectA, objectB).ifPresent(collisions::add);
        }
      }
    }
    return collisions;
  }

  private void dispatchCollisions(List<Collision> collisions, boolean applyImpulse,
      boolean isFirstIteration) {
    for (Collision collision : collisions) {
      dispatchSingleCollision(collision, applyImpulse, isFirstIteration);
    }
  }

  private void dispatchSingleCollision(Collision collision, boolean applyImpulse,
      boolean isFirstIteration) {
    GameObject objectA = collision.getReferenceObject();
    GameObject objectB = collision.getIncidentObject();
    CollisionPair collisionPair = CollisionPair.of(objectA.getClass(), objectB.getClass());

    if (!collisionDispatcher.getSolverMap().containsKey(collisionPair)) {
      collisionPair = findSuperClassPair(collisionPair);
    }
    if (collisionPair == null) {
      LOG.warn("No collision solver registered for pair: {} vs {}",
          objectA.getClass(), objectB.getClass());
      return;
    }

    if (!collisionPair.objectA().isAssignableFrom(objectA.getClass())) {
      GameObject temp = objectA;
      objectA = objectB;
      objectB = temp;
    }

    if (applyImpulse) {
      CollisionSolver solver = collisionDispatcher.getSolverMap().get(collisionPair);
      solver.solve(objectA, objectB, collision, isFirstIteration);
    } else {
      solvePositionOnly(collisionPair, objectA, objectB, collision);
    }
  }

  private void solvePositionOnly(CollisionPair pair, GameObject objectA, GameObject objectB,
      Collision collision) {
    if (isDynamicDynamicPair(pair, objectA, objectB)) {
      applyDynamicDynamicCorrection((DynamicComponent) objectA, (DynamicComponent) objectB,
          collision);
      return;
    }
    if (isDynamicStaticPair(pair, objectA, objectB)) {
      applyDynamicStaticCorrection((DynamicComponent) objectA, (StaticComponent) objectB,
          collision);
    }
  }

  private CollisionPair findSuperClassPair(CollisionPair pair) {
    Map<CollisionPair, CollisionSolver> solverMap = collisionDispatcher.getSolverMap();
    CollisionPair sorted = CollisionPair.of(pair.objectA(), pair.objectB());
    if (solverMap.containsKey(sorted)) {
      return sorted;
    }

    CollisionPair left = null;
    if (pair.objectA().getSuperclass() != null) {
      left = findSuperClassPair(
          new CollisionPair(pair.objectA().getSuperclass(), pair.objectB()));
    }
    if (left != null) {
      return left;
    }

    if (pair.objectB().getSuperclass() != null) {
      return findSuperClassPair(
          new CollisionPair(pair.objectA(), pair.objectB().getSuperclass()));
    }
    return null;
  }

  private void storeImpulse(List<GameObject> pairKey, Collision collision) {
    double normalImpulse = collision.getAppliedNormalImpulse();
    double frictionImpulse = collision.getAppliedFrictionImpulse();
    if (normalImpulse <= 0.0 && Math.abs(frictionImpulse) <= 1e-9) {
      impulseCache.remove(pairKey);
      return;
    }
    impulseCache.put(pairKey, new CachedImpulse(
        normalImpulse, frictionImpulse, collision.getPenetration().normalize()));
  }

  private Set<List<GameObject>> buildJointedPairs() {
    Set<List<GameObject>> pairs = new HashSet<>();
    for (Joint joint : world.getJoints()) {
      if (joint.isCollideConnected()) {
        continue;
      }
      GameObject a = joint.getComponentA();
      GameObject b = joint.getComponentB();
      if (a == null || b == null || a == b) {
        continue;
      }
      pairs.add(canonicalPair(a, b));
    }
    return pairs;
  }

  private void updateDynamicPolygons() {
    for (GameObject go : world.getObjects()) {
      if (go instanceof DynamicComponent) {
        go.updatePolygon();
      }
    }
  }

  record CachedImpulse(double normalImpulse, double frictionImpulse,
                       oogasalad.model.engine.api.Vector normal) {

  }
}
