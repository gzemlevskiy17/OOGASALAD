package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.components.VehicleComponent;

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
  private static final int PHYSICS_SUBSTEPS = 2;
  private static final int COLLISION_RESOLUTION_ITERATIONS = 1;

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
    double subStep = elapsedTime / PHYSICS_SUBSTEPS;
    for (int s = 0; s < PHYSICS_SUBSTEPS; s++) {
      applyJointConstraints(subStep);

      for (GameObject gameObject : world.getObjects()) {
        gameObject.updatePolygon();
      }

      handleCollisions();
      clampWheelsToTerrain();

      for (GameObject gameObject : world.getObjects()) {
        accumulateForces(gameObject);
        applyForcesToAcceleration(gameObject);
        gameObject.getState().integrateObjectState(subStep);
        gameObject.setForce(new Vector(0, 0));
      }

      clampWheelsToTerrain();
    }
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
    for (int i = 0; i < COLLISION_RESOLUTION_ITERATIONS; i++) {
      for (GameObject gameObject : world.getObjects()) {
        gameObject.updatePolygon();
      }
      solveCollisions(getCollisions());
    }
  }

  private void applyJointConstraints(double elapsedTime) {
    for (Joint joint : world.getJoints()) {
      joint.apply(elapsedTime);
    }
  }

  private List<Collision> getCollisions() {
    JointConnectivity jointConnectivity = JointConnectivity.fromJoints(world.getJoints());
    List<Collision> collisions = new ArrayList<>();
    List<GameObject> objects = world.getObjects();
    for (int i = 0; i < objects.size(); i++) {
      GameObject objectA = objects.get(i);
      if (!objectA.isActive()) {
        continue;
      }
      for (int j = i + 1; j < objects.size(); j++) {
        GameObject objectB = objects.get(j);
        if (!objectB.isActive()) {
          continue;
        }
        if (objectA == objectB) {
          continue;
        }
        if (jointConnectivity.inSameAssembly(objectA, objectB)) {
          continue;
        }
        if (shouldSkipCollisionSolverForWheelTerrain(objectA, objectB)) {
          continue;
        }
        Optional<Collision> collision = collisionDetector.detectCollision(objectA, objectB);
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

  /**
   * Wheel–terrain contact is handled by {@link #clampWheelsToTerrain()} (AABB floor clamp) instead
   * of SAT + impulse to avoid lag; same rectangle overlap test as JavaFX {@code Bounds.intersects}
   * for axis-aligned nodes.
   */
  private boolean shouldSkipCollisionSolverForWheelTerrain(GameObject a, GameObject b) {
    return (isWheel(a) && b instanceof TerrainBlock) || (isWheel(b) && a instanceof TerrainBlock);
  }

  private static boolean isWheel(GameObject o) {
    return o instanceof VehicleComponent vc && vc.isWheel();
  }

  /**
   * Keeps wheel hitboxes from intersecting terrain from above: if rectangles overlap and the wheel
   * extends past the terrain's top edge, move the wheel up and remove downward velocity (y grows
   * downward, matching JavaFX).
   */
  private void clampWheelsToTerrain() {
    List<GameObject> objects = world.getObjects();
    List<TerrainBlock> terrains = new ArrayList<>();
    for (GameObject o : objects) {
      if (o instanceof TerrainBlock tb && o.isActive()) {
        terrains.add(tb);
      }
    }
    if (terrains.isEmpty()) {
      return;
    }
    for (GameObject o : objects) {
      if (!(o instanceof VehicleComponent wheel) || !wheel.isWheel() || !o.isActive()) {
        continue;
      }
      double wx = wheel.getX();
      double wy = wheel.getY();
      double ww = wheel.getSpriteWidth();
      double wh = wheel.getSpriteHeight();
      double wheelBottom = wy + wh;

      for (TerrainBlock t : terrains) {
        double tx = t.getX();
        double ty = t.getY();
        double tw = t.getWidth();
        double th = t.getHeight();

        if (!axisAlignedRectsIntersect(wx, wy, ww, wh, tx, ty, tw, th)) {
          continue;
        }
        double terrainBottom = ty + th;
        if (wheelBottom <= ty || wy >= terrainBottom) {
          continue;
        }
        double penetration = wheelBottom - ty;
        if (penetration <= 0) {
          continue;
        }
        wy -= penetration;
        wheelBottom = wy + wh;
        wheel.getState().setLinearPosition(new Vector(wx, wy));
        Vector v = wheel.getState().getLinearVelocity();
        if (v.y() > 0) {
          wheel.getState().setLinearVelocity(new Vector(v.x(), 0));
        }
      }
      wheel.updatePolygon();
    }
  }

  /** Same predicate as two JavaFX {@code Bounds} for min/max layout rects. */
  private static boolean axisAlignedRectsIntersect(
      double ax, double ay, double aw, double ah,
      double bx, double by, double bw, double bh) {
    return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
  }

  /**
   * Union-find over joint endpoints: every body linked by the world's joints is one assembly.
   * Skipping collision for pairs in the same assembly avoids impulses fighting {@link Joint}s.
   */
  private static final class JointConnectivity {

    private final Map<GameObject, GameObject> parent = new IdentityHashMap<>();

    static JointConnectivity fromJoints(List<Joint> joints) {
      JointConnectivity jc = new JointConnectivity();
      for (Joint joint : joints) {
        jc.union(joint.getComponentA(), joint.getComponentB());
      }
      return jc;
    }

    private GameObject find(GameObject o) {
      GameObject p = parent.get(o);
      if (p == null) {
        parent.put(o, o);
        return o;
      }
      if (p != o) {
        GameObject root = find(p);
        parent.put(o, root);
        return root;
      }
      return o;
    }

    private void union(GameObject a, GameObject b) {
      GameObject ra = find(a);
      GameObject rb = find(b);
      if (ra != rb) {
        parent.put(ra, rb);
      }
    }

    boolean inSameAssembly(GameObject a, GameObject b) {
      return find(a) == find(b);
    }
  }
}
