package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.joints.DistanceJoint;
import oogasalad.model.engine.internal.joints.RevoluteJoint;

/**
 * Main physics-step orchestrator for simulation updates in a world.
 *
 * <p>Use this class to coordinate per-frame simulation flow: detect collisions, resolve them via
 * an injected solver, and then integrate object states.
 *
 * <p><strong>Assumptions:</strong> Constructor dependencies are non-null, world objects expose
 * valid physical state, and injected detector/solver implementations obey their interface
 * contracts. Invalid or partial state data may cause no-op updates or runtime exceptions.
 *
 * <p><strong>Dependencies:</strong> Depends on {@link World}, {@link CollisionDetector},
 * {@link CollisionSolver}, {@link GameObject}, {@link StaticState}, {@link DynamicState}, {@link Collision}, and
 * {@link Vector}.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * GameEngine engine = new GameEngineImpl(world, detector, solver);
 * engine.step(1.0 / 60.0);
 * }</pre>
 *
 * <p><strong>Details:</strong> Collision resolution architecture is inspired by Tim Wheeler's
 * write-up on 2D collision detection/resolution: <a
 * href="https://timallanwheeler.com/blog/2024/08/01/2d-collision-detection-and-resolution/">...</a></p>
 *
 * @author Georgiy Zemlevskiy
 */
public class GameEngineImpl implements GameEngine {

  private World world;
  private CollisionDetector collisionDetector;
  private CollisionDispatcher collisionDispatcher;

  private final Vector gravityVector = new Vector(0, -9.8); // Change in the future (inject levelAttributes)
  private static final double GRAVITY = 400.0;
  private static final int JOINT_SOLVER_ITERATIONS = 8;

  /**
   * Creates an engine with explicit simulation dependencies.
   *
   * <p><strong>Purpose:</strong> Initializes the physics engine with a world instance and
   * collision detector. The engine internally creates a {@link CollisionDispatcher} to route
   * collisions to appropriate solvers based on object type pairs.
   *
   * <p><strong>Assumptions:</strong> All arguments are non-null and remain valid for the engine
   * lifetime. The provided world and detector follow their interface contracts.
   *
   * <p><strong>Dependencies:</strong> Requires {@link World} and {@link CollisionDetector}
   * implementations. Uses {@link CollisionDispatcher} and {@link ImpulseCollisionSolver}
   * internally.
   *
   * @param world             world that stores active simulation objects
   * @param collisionDetector pairwise detector used to build collision records
   */
  public GameEngineImpl(World world, CollisionDetector collisionDetector) {
    this.world = world;
    this.collisionDetector = collisionDetector;
    this.collisionDispatcher = new CollisionDispatcher();
  }

  /**
   * Advances the simulation by one time step.
   *
   * <p>Use this method once per frame/tick to run collision handling and state integration in a
   * consistent order.
   *
   * <p><strong>Assumptions:</strong> {@code elapsedTime} is non-negative and world object states
   * are valid for integration.
   *
   * @param elapsedTime simulation delta time in seconds
   */
  @Override
  public void step(double elapsedTime) {
    resetForces(world.getObjects());

    applyAllJointConstraints(elapsedTime, 1);

    for (GameObject gameObject : world.getObjects()) {
      gameObject.updatePolygon();
    }

    applyForces(world.getObjects());

    applyForcesToAcceleration(world.getObjects());

    //TODO: store dynamic/static components in world and only loop over dynamics to integrate
    for (GameObject gameObject : world.getObjects()) {
      if (gameObject instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.integrateObjectState(elapsedTime);
      }
    }

    handleCollisions();

    // Positional constraints (rigid distance/revolute) need iterative post-integration stabilization.
    applyPositionJointConstraints(elapsedTime, JOINT_SOLVER_ITERATIONS);

//    for (Constraint constraint : constraings) {
//      enforceConstraint(constraint);
//    }

  }

  private void applyForcesToAcceleration(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.applyForceToAcceleration();
      }
    }
  }

  private void applyForces(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        if (dynamicComponent.isAffectedByGravity()) {
          dynamicComponent.addForce(gravityVector.multiply(dynamicComponent.getMass()));
        }

        if (object instanceof Driveable driveable) {
          dynamicComponent.addForce(driveable.getDriveForce());
        }
      }
    }

  }

  private void resetForces(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.resetForce();
      }
    }
  }

  //TODO: implement collision detection for all world objects

  //TODO: implement solving of collisions in multiple small steps
  private void handleCollisions() {
    List<Collision> collisions = getCollisions();
    solveCollisions(collisions);
  }

  private void applyAllJointConstraints(double elapsedTime, int iterations) {
    for (int i = 0; i < iterations; i++) {
      for (Joint joint : world.getJoints()) {
        joint.apply(elapsedTime);
      }
    }
  }

  private void applyPositionJointConstraints(double elapsedTime, int iterations) {
    for (int i = 0; i < iterations; i++) {
      for (Joint joint : world.getJoints()) {
        if (isPositionConstraintJoint(joint)) {
          joint.apply(elapsedTime);
        }
      }
    }
  }

  private boolean isPositionConstraintJoint(Joint joint) {
    if (joint instanceof RevoluteJoint) {
      return true;
    }
    return joint instanceof DistanceJoint distanceJoint && distanceJoint.isRigid();
  }

  private static final String VEHICLE_TAG = "vehicle";
  private static final String PIGS_TAG = "pigs";

  private static boolean isPigVehiclePair(String tagA, String tagB) {
    return (PIGS_TAG.equals(tagA) && VEHICLE_TAG.equals(tagB))
        || (VEHICLE_TAG.equals(tagA) && PIGS_TAG.equals(tagB));
  }

  private List<Collision> getCollisions() {
    List<Collision> collisions = new ArrayList<>();
    for (GameObject objectA : world.getObjects()) {
      for (GameObject objectB : world.getObjects()) {
        if (objectA == objectB) {
          continue;
        }
        collisionDetector.detectCollision(objectA, objectB).ifPresent(collisions::add);
      }
    }
    return collisions;
  }

  private void solveCollisions(List<Collision> collisions) {
    for (Collision collision : collisions) {
      CollisionPair collisionPair = CollisionPair.of(collision.getReferenceObject().getClass(),
          collision.getIncidentObject()
              .getClass()); //TODO: add recursive map lookup handler (looks up classes up the hierarchy)
      if (!collisionDispatcher.getSolverMap().containsKey(collisionPair)) {
        collisionPair = findSuperClass(collisionPair);
      }
      collisionDispatcher.getSolverMap().get(collisionPair)
          .solve(collision.getReferenceObject(), collision.getIncidentObject(), collision);
    }
  }

  private CollisionPair findSuperClass(CollisionPair collisionPair) {
    Map<CollisionPair, CollisionSolver> solverMap = collisionDispatcher.getSolverMap();
    CollisionPair sortedPair = CollisionPair.of(collisionPair.objectA(), collisionPair.objectB());
    if (solverMap.containsKey(sortedPair)) {
      return sortedPair;
    }

    //TODO: make more efficient: currently recursive calls are unnecessarily duplicated (DP?)
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

  private void applyJointConstraints(double elapsedTime) {
    for (Joint joint : world.getJoints()) {
      joint.apply(elapsedTime);
    }
  }
}
