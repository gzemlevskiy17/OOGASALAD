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
    applyJointConstraints(elapsedTime);

    handleCollisions();

    //TODO: do this only for dynamic objects
    for (GameObject gameObject : world.getObjects()) { //update to include only dynamic rigid bodies
//      gameObject.getStaticState().integrateObjectState(elapsedTime);
    }

//    for (Constraint constraint : constraings) {
//      enforceConstraint(constraint);
//    }

  }

  //TODO: implement collision detection for all world objects
  //TODO: implement application of forces

  //TODO: implement solving of collisions in multiple small steps
  private void handleCollisions() {
    List<CollisionPair> collisions = getCollisions();

    solveCollisions(collisions);
  }

  private List<CollisionPair> getCollisions() {
    List<CollisionPair> collisions = new ArrayList<>();
    for (GameObject objectA : world.getObjects()) {
      for (GameObject objectB : world.getObjects()) {
        if (collisionDetector.detectCollision(objectA, objectB)) {
          collisions.add(new CollisionPair(objectA, objectB));
        }

//        collision.ifPresent(collisions::add);
      }
    }
    return collisions;
  }

  private void applyJointConstraints(double elapsedTime) {
    for (Joint joint : world.getJoints()) {
      joint.apply(elapsedTime);
    }
  }

  class CollisionPair {

    GameObject objectA;
    GameObject objectB;

    public CollisionPair(GameObject objectA, GameObject objectB) {
      this.objectA = objectA;
      this.objectB = objectB;
    }
  }

  private void solveCollisions(List<Collision> collisions) {
    for (Collision collision : collisions) {
      collisionSolver.solve(collision);
    }
  }
}
