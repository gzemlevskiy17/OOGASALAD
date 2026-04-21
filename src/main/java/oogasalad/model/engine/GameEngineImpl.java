package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
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
 * {@link CollisionSolver}, {@link GameObject}, {@link ObjectState}, {@link Collision}, and
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

  private static final double GRAVITY = 400.0;
  private static final int JOINT_SOLVER_ITERATIONS = 8;

  private final World world;
  private final CollisionDetector collisionDetector;
  private final CollisionSolver collisionSolver;

  /**
   * Creates an engine with explicit simulation dependencies.
   *
   * <p><strong>Assumptions:</strong> All arguments are non-null and remain valid for the engine
   * lifetime.
   *
   * @param world             world that stores active simulation objects
   * @param collisionDetector pairwise detector used to build collision records
   * @param collisionSolver   solver used to resolve detected collisions
   */
  public GameEngineImpl(World world, CollisionDetector collisionDetector,
      CollisionSolver collisionSolver) {
    this.world = world;
    this.collisionDetector = collisionDetector;
    this.collisionSolver = collisionSolver;
  }

  //TODO: implement collision detection for all world objects
  //TODO: implement application of forces

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
   * @throws NullPointerException if world/object state references are null
   */
  @Override
  public void step(double elapsedTime) {
    applyAllJointConstraints(elapsedTime, 1);

    for (GameObject gameObject : world.getObjects()) {
      gameObject.updatePolygon();
    }

    handleCollisions();

    //TODO: do this only for dynamic objects
    for (GameObject gameObject : world.getObjects()) { //update to include only dynamic rigid bodies
      accumulateForces(gameObject);
      applyForcesToAcceleration(gameObject);
      gameObject.getState().integrateObjectState(elapsedTime);
      gameObject.setForce(new Vector(0, 0));
    }

    // Positional constraints (rigid distance/revolute) need iterative post-integration stabilization.
    applyPositionJointConstraints(elapsedTime, JOINT_SOLVER_ITERATIONS);

//    for (Constraint constraint : constraings) {
//      enforceConstraint(constraint);
//    }

  }

  private void accumulateForces(GameObject obj) {
    if (obj.isAffectedByGravity()) {
      obj.addForce(new Vector(0, GRAVITY * obj.getMass()));
    }
    if (obj instanceof Driveable driveable) {
      obj.addForce(driveable.getDriveForce());
    }
  }

  private void applyForcesToAcceleration(GameObject obj) {
    double mass = obj.getMass();
    if (mass > 0) {
      Vector acc = obj.getForce().multiply(1.0 / mass);
      obj.getState().setLinearAcceleration(acc);
    }
  }

  //TODO: implement solving of collisions in multiple small steps
  private void handleCollisions() {
    List<Collision> collisions = getCollisions();

//    Map<String, String> solverMap = {
//        {}
//    }

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
    List<GameObject> objects = world.getObjects();
    for (int i = 0; i < objects.size(); i++) {
      GameObject a = objects.get(i);
      if (a.isTrigger()) {
        continue;
      }
      for (int j = i + 1; j < objects.size(); j++) {
        GameObject b = objects.get(j);
        if (b.isTrigger()) {
          continue;
        }
        if (a.getMass() == 0 && b.getMass() == 0) {
          continue;
        }
        if (VEHICLE_TAG.equals(a.getTag()) && VEHICLE_TAG.equals(b.getTag())) {
          continue;
        }
        if (isPigVehiclePair(a.getTag(), b.getTag())) {
          continue;
        }
        Optional<Collision> collision = collisionDetector.detectCollision(a, b);
        collision.ifPresent(collisions::add);
      }
    }
    return collisions;
  }

  private void solveCollisions(List<Collision> collisions) {
    for (Collision collision : collisions) {
      collisionSolver.solve(collision);
    }
  }
}
