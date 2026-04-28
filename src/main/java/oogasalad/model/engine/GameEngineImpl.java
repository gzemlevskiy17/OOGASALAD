package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.joints.DistanceJoint;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main physics-step orchestrator for simulation updates in a world.
 *
 * <p>
 * Use this class to coordinate per-frame simulation flow: detect collisions,
 * resolve them via
 * an injected solver, and then integrate object states.
 *
 * <p>
 * <strong>Assumptions:</strong> Constructor dependencies are non-null, world
 * objects expose
 * valid physical state, and injected detector/solver implementations obey their
 * interface
 * contracts. Invalid or partial state data may cause no-op updates or runtime
 * exceptions.
 *
 * <p>
 * <strong>Dependencies:</strong> Depends on {@link World},
 * {@link CollisionDetector},
 * {@link CollisionSolver}, {@link GameObject}, {@link StaticState},
 * {@link DynamicState},
 * {@link Collision}, and {@link Vector}.
 *
 * <p>
 * <strong>Example:</strong>
 *
 * <pre>{@code
 * GameEngine engine = new GameEngineImpl(world, detector, solver);
 * engine.step(1.0 / 60.0);
 * }</pre>
 *
 * <p>
 * <strong>Details:</strong> Collision resolution architecture is inspired by
 * Tim Wheeler's
 * write-up on 2D collision detection/resolution: <a
 * href=
 * "https://timallanwheeler.com/blog/2024/08/01/2d-collision-detection-and-resolution/">...</a>
 * </p>
 *
 * @author Georgiy Zemlevskiy
 * @author Iurii Beliaev
 */
public class GameEngineImpl implements GameEngine {

  private static final double DEFAULT_FIXED_TIME_STEP = 1.0 / 60.0;
  private static final int DEFAULT_SUB_STEP_COUNT = 4;
  private static final double MAX_FRAME_TIME = 0.25;
  private static final Logger LOG = LogManager.getLogger(GameEngineImpl.class);
  private static final double AABB_MARGIN = 1e-6;
  private static final double CCD_SKIN = 1e-4;

  /**
   * Total order for building a stable key for the unordered pair {A, B}. Same two
   * instances always
   * map to the same list regardless of which was passed as "first" in the double
   * loop.
   */
  private static final Comparator<GameObject> UNORDERED_PAIR_KEY_ORDER = Comparator
      .<GameObject>comparingInt(o -> System.identityHashCode(o))
      .thenComparingInt(Object::hashCode)
      .thenComparing(o -> o.getClass().getName())
      .thenComparing(GameObject::getTag, Comparator.nullsFirst(Comparator.naturalOrder()));
  private static final int JOINT_SOLVER_ITERATIONS = 8;
  private final World world;
  private final CollisionDetector collisionDetector;
  private final CollisionDispatcher collisionDispatcher;
  // private final CollisionSolver overrideCollisionSolver;
  private final Map<List<GameObject>, CachedCollisionImpulse> collisionImpulseCache;
  private final double fixedTimeStep;
  private final int subStepCount;
  private double accumulatedTime;

  /**
   * Creates an engine with explicit simulation dependencies.
   *
   * <p>Per-level physics constants (gravity, drag) are read from
   * {@code world.getPhysicsEnvironment()} on each step rather than passed here.
   *
   * @param world             world that stores active simulation objects and the physics environment
   * @param collisionDetector pairwise detector used to build collision records
   */
  public GameEngineImpl(World world, CollisionDetector collisionDetector) {
    this(world, collisionDetector, null, DEFAULT_FIXED_TIME_STEP, DEFAULT_SUB_STEP_COUNT);
  }

  /**
   * Compatibility constructor for tests and callers that inject a single response strategy.
   */
  public GameEngineImpl(World world, CollisionDetector collisionDetector,
      CollisionSolver collisionSolver) {
    this(world, collisionDetector, collisionSolver, DEFAULT_FIXED_TIME_STEP, DEFAULT_SUB_STEP_COUNT);
  }

  private GameEngineImpl(World world, CollisionDetector collisionDetector,
      CollisionSolver collisionSolver, double fixedTimeStep, int subStepCount) {
    if (fixedTimeStep <= 0.0) {
      throw new IllegalArgumentException("fixedTimeStep must be positive");
    }
    if (subStepCount <= 0) {
      throw new IllegalArgumentException("subStepCount must be positive");
    }
    this.world = world;
    this.collisionDetector = collisionDetector;
    this.collisionDispatcher = new CollisionDispatcher();
    // this.overrideCollisionSolver = collisionSolver;
    this.collisionImpulseCache = new HashMap<>();
    this.fixedTimeStep = fixedTimeStep;
    this.subStepCount = subStepCount;
    this.accumulatedTime = 0.0;
  }

  private static List<GameObject> canonicalUnorderedPair(GameObject a, GameObject b) {
    if (UNORDERED_PAIR_KEY_ORDER.compare(a, b) <= 0) {
      return List.of(a, b);
    }
    return List.of(b, a);
  }

  private static boolean aabbsOverlap(GameObject objectA, GameObject objectB) {
    return BoundingBox.from(objectA).overlaps(BoundingBox.from(objectB));
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

  private static void applyDynamicDynamicPositionCorrection(DynamicComponent objectA,
      DynamicComponent objectB, Collision collision) {
    if (objectA == collision.getIncidentObject()) {
      collision.setPenetration(collision.getPenetration().multiply(-1));
    }
    ImpulseSolverUtilities.applyPositionalCorrection(collision, objectA, objectB);
  }

  private static void applyDynamicStaticPositionCorrection(DynamicComponent dynamicObject,
      StaticComponent staticObject, Collision collision) {
    if (dynamicObject == collision.getReferenceObject()) {
      collision.setPenetration(collision.getPenetration().multiply(-1));
    }
    ImpulseSolverUtilities.applyPositionalCorrection(collision, staticObject, dynamicObject);
  }

  /**
   * Returns jointed unordered pairs whose joint does not opt into connected-body
   * collision.
   */
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
      pairs.add(canonicalUnorderedPair(a, b));
    }
    return pairs;
  }

  private void applyAllJointConstraints(double elapsedTime, int iterations) {
    for (int i = 0; i < iterations; i++) {
      List<Joint> joints = new ArrayList<>(world.getJoints());
      Collections.shuffle(joints);
      for (Joint joint : joints) {
        joint.apply(elapsedTime);
      }
    }
  }

  private void applyPositionJointConstraints(double elapsedTime, int iterations) {
    for (int i = 0; i < iterations; i++) {
      List<Joint> joints = new ArrayList<>(world.getJoints());
      Collections.shuffle(joints);
      for (Joint joint : joints) {
        if (isPositionConstraintJoint(joint)) {
          joint.apply(elapsedTime);
        }
      }
    }
  }

  private void applyForcesToAcceleration(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.applyForceToAcceleration();
      }
    }
  }

  private boolean isPositionConstraintJoint(Joint joint) {
    if (joint instanceof RevoluteJoint) {
      return true;
    }
    return joint instanceof DistanceJoint distanceJoint && distanceJoint.isRigid();
  }

  private void applyVelocityJointConstraints() {
    List<Joint> joints = new ArrayList<>(world.getJoints());
    Collections.shuffle(joints);
    for (Joint joint : joints) {
      joint.applyVelocity();
    }
  }

  private void applyForces(Collection<GameObject> objects) {
    PhysicsEnvironment physics = world.getPhysicsEnvironment();
    Vector gravityVector = physics.gravity();
    double drag = physics.drag();
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        if (dynamicComponent.isAffectedByGravity()) {
          dynamicComponent.addForce(gravityVector.multiply(dynamicComponent.getMass()));
          dynamicComponent.addForce(dynamicComponent.getDynamicState().getLinearVelocity()
              .multiply(-drag));
          dynamicComponent.addTorque(dynamicComponent.getDynamicState().getAngularVelocity()
              * -drag);
        }

        if (object instanceof Driveable driveable) { // TODO: refactor
          dynamicComponent.addForce(driveable.getDriveForce());
        }
      }
    }
  }

  private void resetForces(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.resetForces();
      }
    }
  }

  private void updateSleepStates(Collection<GameObject> objects, double elapsedTime) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.updateSleepState(elapsedTime);
      }
    }
  }

  /**
   * Advances the simulation by one time step.
   *
   * <p>
   * Use this method once per frame/tick to run collision handling and state
   * integration in a
   * consistent order.
   *
   * <p>
   * <strong>Assumptions:</strong> {@code elapsedTime} is non-negative and world
   * object states
   * are valid for integration.
   *
   * @param elapsedTime simulation delta time in seconds
   */
  @Override
  public void step(double elapsedTime) {
    if (elapsedTime <= 0.0 || Double.isNaN(elapsedTime)) {
      return;
    }

    accumulatedTime += Math.min(elapsedTime, MAX_FRAME_TIME);
    while (accumulatedTime + 1e-12 >= fixedTimeStep) {
      stepFixedTimeSlice();
      accumulatedTime -= fixedTimeStep;
    }
  }

  private void stepFixedTimeSlice() {
    double subStepTime = fixedTimeStep / subStepCount;
    for (int i = 0; i < subStepCount; i++) {
      simulateSubStep(subStepTime);
    }
  }

  private void simulateSubStep(double elapsedTime) {
    resetForces(world.getObjects());

    for (GameObject gameObject : world.getObjects()) {
      gameObject.updatePolygon();
    }
    Map<DynamicComponent, MotionSnapshot> previousMotion = captureDynamicMotion();

    applyAllJointConstraints(elapsedTime, 1);

    applyForces(world.getObjects());

    applyForcesToAcceleration(world.getObjects());

    // TODO: store dynamic/static components in world and only loop over dynamics to
    // integrate
    for (GameObject gameObject : world.getObjects()) {
      if (gameObject instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.integrateObjectState(elapsedTime);
      }
    }

    resolveSweptStaticCollisions(previousMotion);

    // update the polygon for the second time after the computation
    for (GameObject gameObject : world.getObjects()) {
      if (gameObject instanceof DynamicComponent) {
        gameObject.updatePolygon();
      }
    }

    solveConstraints(elapsedTime);
    updateSleepStates(world.getObjects(), elapsedTime);
  }

  private void solveConstraints(double elapsedTime) {
    List<Collision> collisions = getCollisions();

    for (Collision collision : collisions) {
      GameObject objectA = collision.getReferenceObject();
      GameObject objectB = collision.getIncidentObject();
      List<GameObject> pairKey = canonicalUnorderedPair(objectA, objectB);
      applyCachedImpulse(pairKey, collision);
    }

    for (int i = 0; i < JOINT_SOLVER_ITERATIONS; i++) {
      applyVelocityJointConstraints();
      solveCollisions(collisions, true, i == 0);
    }

    for (int i = 0; i < JOINT_SOLVER_ITERATIONS; i++) {
      applyPositionJointConstraints(elapsedTime, 1);
      solveCollisions(collisions, false, false);
      for (GameObject go : world.getObjects()) {
        if (go instanceof DynamicComponent) {
          go.updatePolygon();
        }
      }
    }

    Set<List<GameObject>> activeImpulseKeys = new HashSet<>();
    for (Collision collision : collisions) {
      GameObject objectA = collision.getReferenceObject();
      GameObject objectB = collision.getIncidentObject();
      List<GameObject> pairKey = canonicalUnorderedPair(objectA, objectB);
      storeCachedImpulse(pairKey, collision);
      activeImpulseKeys.add(pairKey);
    }
    collisionImpulseCache.keySet().retainAll(activeImpulseKeys);
  }

  private List<Collision> getCollisions() {
    List<Collision> collisions = new ArrayList<>();
    Set<List<GameObject>> seenUnorderedPairs = new HashSet<>();
    Set<List<GameObject>> jointedPairs = buildJointedPairs();
    for (DynamicComponent objectA : world.getActiveDynamicComponents()) {
      for (GameObject objectB : world.getObjects()) {
        if (objectA == objectB) {
          continue;
        }

        // TODO: refactor to use CollisionPairs?
        List<GameObject> pairKey = canonicalUnorderedPair(objectA, objectB);
        if (!seenUnorderedPairs.add(pairKey)) {
          continue;
        }

        if (jointedPairs.contains(pairKey)) { // Joints will handle the collision of jointed pairs
          continue;
        }

        // This broad-phase check is less expensive than the collisionDetector's narrow
        // phase check
        if (aabbsOverlap(objectA, objectB)) {
          collisionDetector.detectCollision(objectA, objectB).ifPresent(collisions::add);
        }
      }
    }
    return collisions;
  }

  private void solveCollisions(List<Collision> collisions, boolean applyImpulse, boolean isFirstIteration) {
    for (Collision collision : collisions) {
      GameObject objectA = collision.getReferenceObject();
      GameObject objectB = collision.getIncidentObject();
      List<GameObject> pairKey = canonicalUnorderedPair(objectA, objectB);
      CollisionPair collisionPair = CollisionPair.of(objectA.getClass(),
          objectB.getClass());
      if (!collisionDispatcher.getSolverMap().containsKey(collisionPair)) {
        collisionPair = findSuperClass(collisionPair);
      }

      if (collisionPair == null) {
        LOG.warn("No collision solver registered for pair: {} vs {}",
            objectA.getClass(), objectB.getClass());
        continue;
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
        solveCollisionPositionOnly(collisionPair, objectA, objectB, collision);
      }
    }
  }

  private void applyCachedImpulse(List<GameObject> pairKey, Collision collision) {
    CachedCollisionImpulse cachedImpulse = collisionImpulseCache.get(pairKey);
    if (cachedImpulse == null) {
      return;
    }
    collision.setCachedNormalImpulse(cachedImpulse.normalImpulse());
    collision.setCachedFrictionImpulse(cachedImpulse.frictionImpulse());
    collision.setCachedNormal(cachedImpulse.normal());
  }

  private void storeCachedImpulse(List<GameObject> pairKey, Collision collision) {
    if (collision.getAppliedNormalImpulse() <= 0.0
        && Math.abs(collision.getAppliedFrictionImpulse()) <= 1e-9) {
      collisionImpulseCache.remove(pairKey);
      return;
    }
    collisionImpulseCache.put(pairKey, new CachedCollisionImpulse(
        collision.getAppliedNormalImpulse(),
        collision.getAppliedFrictionImpulse(),
        collision.getPenetration().normalize()));
  }

  private void solveCollisionPositionOnly(CollisionPair collisionPair, GameObject objectA,
      GameObject objectB, Collision collision) {
    if (isDynamicDynamicPair(collisionPair, objectA, objectB)) {
      applyDynamicDynamicPositionCorrection((DynamicComponent) objectA, (DynamicComponent) objectB,
          collision);
      return;
    }
    if (isDynamicStaticPair(collisionPair, objectA, objectB)) {
      applyDynamicStaticPositionCorrection((DynamicComponent) objectA, (StaticComponent) objectB,
          collision);
    }
  }

  private CollisionPair findSuperClass(CollisionPair collisionPair) {
    Map<CollisionPair, CollisionSolver> solverMap = collisionDispatcher.getSolverMap();
    CollisionPair sortedPair = CollisionPair.of(collisionPair.objectA(), collisionPair.objectB());
    if (solverMap.containsKey(sortedPair)) {
      return sortedPair;
    }

    // TODO: make more efficient: currently recursive calls are unnecessarily
    // duplicated (DP?)
    CollisionPair left = null;
    if (collisionPair.objectA().getSuperclass() != null) {
      left = findSuperClass(
          new CollisionPair(collisionPair.objectA().getSuperclass(), collisionPair.objectB()));
    }

    if (left != null) {
      return left;
    }

    if (collisionPair.objectB().getSuperclass() != null) {
      return findSuperClass(
          new CollisionPair(collisionPair.objectA(), collisionPair.objectB().getSuperclass()));
    }
    return null;
  }

  private Map<DynamicComponent, MotionSnapshot> captureDynamicMotion() {
    Map<DynamicComponent, MotionSnapshot> motion = new HashMap<>();
    for (GameObject object : world.getObjects()) {
      if (object instanceof DynamicComponent dynamicComponent) {
        motion.put(dynamicComponent, new MotionSnapshot(BoundingBox.from(dynamicComponent),
            dynamicComponent.getStaticState().getLinearPosition()));
      }
    }
    return motion;
  }

  private void resolveSweptStaticCollisions(Map<DynamicComponent, MotionSnapshot> previousMotion) {
    for (GameObject dynamicObject : world.getObjects()) {
      if (!(dynamicObject instanceof DynamicComponent dynamicComponent)) {
        continue;
      }
      MotionSnapshot start = previousMotion.get(dynamicComponent);
      if (start == null) {
        continue;
      }
      BoundingBox end = BoundingBox.from(dynamicComponent);
      Vector displacement = end.center().subtract(start.bounds().center());
      SweptHit earliestHit = null;
      for (GameObject staticObject : world.getObjects()) {
        if (staticObject == dynamicObject || !(staticObject instanceof StaticComponent)) {
          continue;
        }
        BoundingBox staticBounds = BoundingBox.from(staticObject);
        if (end.overlaps(staticBounds)) {
          continue;
        }
        SweptHit hit = sweepAgainstStatic(start.bounds(), staticBounds, displacement);
        if (hit != null && (earliestHit == null || hit.time() < earliestHit.time())) {
          earliestHit = hit;
        }
      }
      if (earliestHit != null) {
        applySweptHit(dynamicComponent, start.position(), displacement, earliestHit);
      }
    }
  }

  private SweptHit sweepAgainstStatic(BoundingBox movingStart, BoundingBox staticBounds,
      Vector displacement) {
    AxisSweep xSweep = sweepAxis(movingStart.minX(), movingStart.maxX(), staticBounds.minX(),
        staticBounds.maxX(), displacement.x());
    AxisSweep ySweep = sweepAxis(movingStart.minY(), movingStart.maxY(), staticBounds.minY(),
        staticBounds.maxY(), displacement.y());
    if (!xSweep.canHit() || !ySweep.canHit()) {
      return null;
    }

    double entryTime = Math.max(xSweep.entryTime(), ySweep.entryTime());
    double exitTime = Math.min(xSweep.exitTime(), ySweep.exitTime());
    if (entryTime < 0.0 || entryTime > 1.0 || entryTime > exitTime) {
      return null;
    }

    Vector normal = xSweep.entryTime() > ySweep.entryTime()
        ? new Vector(-Math.signum(displacement.x()), 0)
        : new Vector(0, -Math.signum(displacement.y()));
    return new SweptHit(entryTime, normal);
  }

  private AxisSweep sweepAxis(double movingMin, double movingMax, double staticMin,
      double staticMax, double delta) {
    if (Math.abs(delta) <= AABB_MARGIN) {
      boolean alreadyOverlapping = movingMax >= staticMin && staticMax >= movingMin;
      return alreadyOverlapping
          ? new AxisSweep(true, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
          : new AxisSweep(false, 0.0, 0.0);
    }
    double entryDistance = delta > 0.0 ? staticMin - movingMax : staticMax - movingMin;
    double exitDistance = delta > 0.0 ? staticMax - movingMin : staticMin - movingMax;
    return new AxisSweep(true, entryDistance / delta, exitDistance / delta);
  }

  private void applySweptHit(DynamicComponent component, Vector startCenter, Vector displacement,
      SweptHit hit) {
    double safeTime = sweptContactTime(hit);
    component.getStaticState().setLinearPosition(startCenter.add(displacement.multiply(safeTime)));
    Vector velocity = component.getDynamicState().getLinearVelocity();
    double normalVelocity = velocity.dot(hit.normal());
    if (normalVelocity < 0.0 && isHorizontalHit(hit)) {
      component.getDynamicState().setLinearVelocity(
          velocity.subtract(hit.normal().multiply(normalVelocity)));
    }
    component.updatePolygon();
  }

  private double sweptContactTime(SweptHit hit) {
    double skinDirection = isHorizontalHit(hit) ? -CCD_SKIN : CCD_SKIN;
    return Math.max(0.0, Math.min(1.0, hit.time() + skinDirection));
  }

  private boolean isHorizontalHit(SweptHit hit) {
    return Math.abs(hit.normal().x()) > Math.abs(hit.normal().y());
  }

  private record BoundingBox(double minX, double minY, double maxX, double maxY) {

    private static BoundingBox from(GameObject object) {
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      for (ConvexPolygon polygon : object.getCollisionPolygons()) {
        for (Vector point : polygon.points()) {
          minX = Math.min(minX, point.x());
          minY = Math.min(minY, point.y());
          maxX = Math.max(maxX, point.x());
          maxY = Math.max(maxY, point.y());
        }
      }
      return new BoundingBox(minX, minY, maxX, maxY);
    }

    private boolean overlaps(BoundingBox other) {
      return maxX + AABB_MARGIN >= other.minX
          && other.maxX + AABB_MARGIN >= minX
          && maxY + AABB_MARGIN >= other.minY
          && other.maxY + AABB_MARGIN >= minY;
    }

    private Vector center() {
      return new Vector((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }
  }

  private record AxisSweep(boolean canHit, double entryTime, double exitTime) {

  }

  private record SweptHit(double time, Vector normal) {

  }

  private record MotionSnapshot(BoundingBox bounds, Vector position) {

  }

  private record CachedCollisionImpulse(double normalImpulse, double frictionImpulse,
      Vector normal) {

  }
}
