package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.List;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;

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

  private World world;
  private CollisionDetector collisionDetector;
  private CollisionSolver collisionSolver;

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
    applyJointConstraints(elapsedTime);

    handleCollisions();

    for (GameObject gameObject : world.getObjects()) { //update to include only dynamic rigid bodies
      ObjectState newState = integrateObjectState(gameObject, elapsedTime);
      gameObject.setState(newState);
      gameObject.setForce(new Vector(0, 0));
    }

//    for (Constraint constraint : constraings) {
//      enforceConstraint(constraint);
//    }

  }


  private void handleCollisions() {
    List<Collision> collisions = getCollisions();
    solveCollisions(collisions);
  }

  private void applyJointConstraints(double elapsedTime) {
    for (Joint joint : world.getJoints()) {
      joint.apply(elapsedTime);
    }
  }

  private List<Collision> getCollisions() {
    List<Collision> collisions = new ArrayList<>();
    for (GameObject objectA : world.getObjects()) {
      for (GameObject objectB : world.getObjects()) {
        collisions.add(collisionDetector.detectCollision(objectA, objectB));
      }
    }
    return collisions;
  }

  private void solveCollisions(List<Collision> collisions) {
    for (Collision collision : collisions) {
      if (collision != null) {
        collisionSolver.solve(collision);
      }
    }
  }

  private ObjectState integrateObjectState(GameObject gameObject, double elapsedTime) {
    ObjectState state = gameObject.getState();
    if (gameObject.getMass() <= 0.0) {
      return new ObjectState(
          state.getLinearPosition(),
          new Vector(0, 0),
          new Vector(0, 0),
          state.getAngularPosition(),
          new Vector(0, 0),
          state.getAngularAcceleration()
      );
    }

    Vector newLinearAcceleration = gameObject.getForce().multiply(1.0 / gameObject.getMass());
    Vector newLinearVelocity = integrateVector(state.getLinearVelocity(),
        newLinearAcceleration, elapsedTime);
    Vector newLinearPosition = integrateVector(state.getLinearPosition(), newLinearVelocity,
        elapsedTime);

    Vector newAngularVelocity = integrateVector(state.getAngularVelocity(),
        state.getAngularAcceleration(), elapsedTime);
    double newAngularPosition = integrateScalar(state.getAngularPosition(),
        newAngularVelocity.magnitude(), elapsedTime);

    return new ObjectState(newLinearPosition, newLinearVelocity,
        newLinearAcceleration, newAngularPosition, newAngularVelocity,
        state.getAngularAcceleration());
  }

  private Vector integrateVector(Vector previousVector, Vector integrand, double elapsedTime) {
    double newX = integrateScalar(previousVector.x(), integrand.x(), elapsedTime);
    double newY = integrateScalar(previousVector.y(), integrand.y(), elapsedTime);

    return new Vector(newX, newY);
  }

  private double integrateScalar(double previousValue, double integrandValue, double elapsedTime) {
    return previousValue + integrandValue * elapsedTime;
  }
}
