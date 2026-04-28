package oogasalad.model.engine;

import java.util.Map;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.GameEngine;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 * Main physics-step orchestrator for simulation updates in a world.
 *
 * <p>Coordinates per-frame simulation flow: apply forces, integrate state,
 * resolve swept collisions, and solve constraints. Each phase is delegated
 * to a focused collaborator.
 *
 * @author Georgiy Zemlevskiy
 * @author Iurii Beliaev
 */
public class GameEngineImpl implements GameEngine {

  private static final double DEFAULT_FIXED_TIME_STEP = 1.0 / 60.0;
  private static final int DEFAULT_SUB_STEP_COUNT = 4;
  private static final double MAX_FRAME_TIME = 0.25;

  private final World world;
  private final ForceIntegrator forceIntegrator;
  private final JointSolver jointSolver;
  private final SweptCollisionResolver sweptResolver;
  private final CollisionPhase collisionPhase;
  private final double fixedTimeStep;
  private final int subStepCount;
  private double accumulatedTime;

  public GameEngineImpl(World world, CollisionDetector collisionDetector) {
    this(world, collisionDetector, DEFAULT_FIXED_TIME_STEP, DEFAULT_SUB_STEP_COUNT);
  }

  public GameEngineImpl(World world, CollisionDetector collisionDetector,
      double fixedTimeStep, int subStepCount) {
    if (fixedTimeStep <= 0.0) {
      throw new IllegalArgumentException("fixedTimeStep must be positive");
    }
    if (subStepCount <= 0) {
      throw new IllegalArgumentException("subStepCount must be positive");
    }
    this.world = world;
    this.fixedTimeStep = fixedTimeStep;
    this.subStepCount = subStepCount;
    this.accumulatedTime = 0.0;

    this.forceIntegrator = new ForceIntegrator(world);
    this.jointSolver = new JointSolver(world);
    this.sweptResolver = new SweptCollisionResolver(world);
    CollisionDispatcher dispatcher = new CollisionDispatcher();
    this.collisionPhase = new CollisionPhase(world, collisionDetector, dispatcher, jointSolver);
  }

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
    forceIntegrator.resetForces(world.getObjects());
    updateAllPolygons();

    Map<DynamicComponent, SweptCollisionResolver.MotionSnapshot> previousMotion =
        sweptResolver.captureMotion();

    jointSolver.applyAll(elapsedTime, 1);
    forceIntegrator.applyForces(world.getObjects());
    forceIntegrator.applyForcesToAcceleration(world.getObjects());
    integrateDynamicComponents(elapsedTime);
    updateDynamicPolygons();

    sweptResolver.resolve(previousMotion);
    updateDynamicPolygons();

    collisionPhase.solve(elapsedTime);
    forceIntegrator.updateSleepStates(world.getObjects(), elapsedTime);
  }

  private void integrateDynamicComponents(double elapsedTime) {
    for (GameObject gameObject : world.getObjects()) {
      if (gameObject instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.integrateObjectState(elapsedTime);
      }
    }
  }

  private void updateAllPolygons() {
    for (GameObject gameObject : world.getObjects()) {
      gameObject.updatePolygon();
    }
  }

  private void updateDynamicPolygons() {
    for (GameObject gameObject : world.getObjects()) {
      if (gameObject instanceof DynamicComponent) {
        gameObject.updatePolygon();
      }
    }
  }
}
