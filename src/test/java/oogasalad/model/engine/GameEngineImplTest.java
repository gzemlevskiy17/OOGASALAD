package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.FinishLineComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.RockComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import org.junit.jupiter.api.Test;

/**
 * Focused tests for force → acceleration → integration in {@link GameEngineImpl}.
 */
class GameEngineImplTest {

  private static final double EPS = 1e-9;
  /**
   * Must match {@link GameEngineImpl} default gravity (screen-space positive y).
   */
  private static final double G = 980.0;
  private static final double FIXED_DT = 1.0 / 60.0;
  private static final int SUB_STEPS = 4;

  private static final CollisionDetector NO_HITS = (a, b) -> Optional.empty();
  private static final CollisionSolver NO_SOLVE = (a, b, c, d) -> {
  };

  private static DynamicComponent box(double mass, double x, double y, boolean gravity) {
    ConvexPolygon poly =
        new ConvexPolygon(
            List.of(
                new Vector(0, 0),
                new Vector(1, 0),
                new Vector(1, 1),
                new Vector(0, 1)));
    DynamicComponent b =
        new DynamicComponent(x, y, 0, poly, "b", 0, mass, 1, 0.35, gravity) {
        };
    b.activate();
    return b;
  }

  private static double semiImplicitSubstepDisplacement(double acceleration, int fixedSteps) {
    double h = FIXED_DT / SUB_STEPS;
    int totalSubSteps = fixedSteps * SUB_STEPS;
    return acceleration * h * h * totalSubSteps * (totalSubSteps + 1) / 2.0;
  }

  @Test
  void step_gravityProducesExpectedVerticalAccelerationAndDisplacement() {
    World world = new WorldImpl();
    DynamicComponent body = box(2.0, 0, 100, true);
    world.addComponent(body);

    GameEngineImpl engine = new GameEngineImpl(world, NO_HITS, NO_SOLVE);
    engine.step(FIXED_DT);

    assertEquals(G, body.getDynamicState().getLinearAcceleration().y(), EPS);
    assertEquals(G * FIXED_DT, body.getDynamicState().getLinearVelocity().y(), EPS);
    assertEquals(semiImplicitSubstepDisplacement(G, 1),
        body.getStaticState().getLinearPosition().y() - 100, EPS);
  }

  @Test
  void SetGravity_CustomGravity_ChangesFutureVerticalAcceleration() {
    World world = new WorldImpl();
    DynamicComponent body = box(2.0, 0, 100, true);
    world.addComponent(body);
    GameEngineImpl engine = new GameEngineImpl(world, NO_HITS, NO_SOLVE);

    engine.setGravity(new Vector(0, 120));
    engine.step(FIXED_DT);

    assertEquals(120, body.getDynamicState().getLinearAcceleration().y(), EPS,
        "setGravity should affect the acceleration used by future physics steps.");
    assertEquals(120 * FIXED_DT, body.getDynamicState().getLinearVelocity().y(), EPS,
        "setGravity should affect velocity integration on the next physics step.");
  }

  @Test
  void step_accumulatesPartialFramesUntilFixedStepIsAvailable() {
    World world = new WorldImpl();
    DynamicComponent body = box(2.0, 0, 100, true);
    world.addComponent(body);

    GameEngineImpl engine = new GameEngineImpl(world, NO_HITS, NO_SOLVE);
    engine.step(FIXED_DT / 2.0);

    assertEquals(100, body.getStaticState().getLinearPosition().y(), EPS);
    assertEquals(0, body.getDynamicState().getLinearVelocity().y(), EPS);

    engine.step(FIXED_DT / 2.0);

    assertEquals(G * FIXED_DT, body.getDynamicState().getLinearVelocity().y(), EPS);
    assertEquals(100 + semiImplicitSubstepDisplacement(G, 1),
        body.getStaticState().getLinearPosition().y(), EPS);
  }

  @Test
  void step_noGravity_noLinearMotion() {
    World world = new WorldImpl();
    DynamicComponent body = box(1.0, 3, 4, false);
    world.addComponent(body);

    GameEngineImpl engine = new GameEngineImpl(world, NO_HITS, NO_SOLVE);
    engine.step(0.02);

    assertEquals(3, body.getStaticState().getLinearPosition().x(), EPS);
    assertEquals(4, body.getStaticState().getLinearPosition().y(), EPS);
    assertEquals(0, body.getDynamicState().getLinearVelocity().x(), EPS);
    assertEquals(0, body.getDynamicState().getLinearVelocity().y(), EPS);
  }

  @Test
  void step_driveableThrustAddsForceAndAcceleration() {
    World world = new WorldImpl();
    DriveBody body = new DriveBody(0, 0, 1.0, new Vector(0, 1), 5.0);
    world.addComponent(body);

    GameEngineImpl engine = new GameEngineImpl(world, NO_HITS, NO_SOLVE);
    double dt = 0.05;
    engine.step(dt);

    assertEquals(5.0, body.getDynamicState().getLinearAcceleration().y(), EPS);
    assertEquals(semiImplicitSubstepDisplacement(5.0, 3),
        body.getStaticState().getLinearPosition().y(), EPS);
  }

  @Test
  void step_inactiveBodySkippedByEngineLoop() {
    World world = new WorldImpl();
    DynamicComponent body = box(1.0, 0, 0, true);
    body.deactivate();
    world.addComponent(body);

    GameEngineImpl engine = new GameEngineImpl(world, NO_HITS, NO_SOLVE);
    engine.step(0.1);

    assertEquals(0, body.getStaticState().getLinearPosition().x(), EPS);
    assertEquals(0, body.getStaticState().getLinearPosition().y(), EPS);
  }

  @Test
  void findSuperClass_resolvesDynamicStatic()
      throws ReflectiveOperationException {
    GameEngineImpl engine = new GameEngineImpl(new WorldImpl(), NO_HITS, NO_SOLVE);
    Method method = GameEngineImpl.class.getDeclaredMethod("findSuperClass", CollisionPair.class);
    method.setAccessible(true);

    CollisionPair result = (CollisionPair) method.invoke(engine,
        new CollisionPair(RockComponent.class, DriveBody.class));

    assertEquals(CollisionPair.of(StaticComponent.class, DynamicComponent.class), result);
  }

  @Test
  void findSuperClass_resolvesStaticDynamic() throws ReflectiveOperationException {
    GameEngineImpl engine = new GameEngineImpl(new WorldImpl(), NO_HITS, NO_SOLVE);
    Method method = GameEngineImpl.class.getDeclaredMethod("findSuperClass", CollisionPair.class);
    method.setAccessible(true);

    CollisionPair result = (CollisionPair) method.invoke(engine,
        new CollisionPair(StructuralComponent.class, FinishLineComponent.class));

    assertEquals(CollisionPair.of(DynamicComponent.class, PassThroughComponent.class), result);
  }

  @Test
  void findSuperClass_returnsNullOnStaticStaticPair()
      throws ReflectiveOperationException {
    GameEngineImpl engine = new GameEngineImpl(new WorldImpl(), NO_HITS, NO_SOLVE);
    Method method = GameEngineImpl.class.getDeclaredMethod("findSuperClass", CollisionPair.class);
    method.setAccessible(true);

    CollisionPair result = (CollisionPair) method.invoke(engine,
        new CollisionPair(StaticComponent.class, StaticComponent.class));

    assertNull(result);
  }

  private static final class DriveBody extends DynamicComponent implements Driveable {

    private Vector dir = new Vector(1, 0);
    private double thrust;
    private boolean driveOn = true;
    private boolean player = true;

    DriveBody(double x, double y, double mass, Vector dir, double thrust) {
      super(
          x,
          y,
          0,
          new ConvexPolygon(
              List.of(
                  new Vector(0, 0),
                  new Vector(1, 0),
                  new Vector(1, 1),
                  new Vector(0, 1))),
          "d",
          0,
          mass,
          1,
          0.35,
          false);
      this.dir = dir;
      this.thrust = thrust;
      activate();
    }

    @Override
    public Vector getThrustDirection() {
      return dir;
    }

    @Override
    public void setThrustDirection(Vector thrustDirection) {
      this.dir = thrustDirection;
    }

    @Override
    public double getThrustForce() {
      return thrust;
    }

    @Override
    public void setThrustForce(double thrustForce) {
      this.thrust = thrustForce;
    }

    @Override
    public boolean isDriveEnabled() {
      return driveOn;
    }

    @Override
    public void setDriveEnabled(boolean driveEnabled) {
      this.driveOn = driveEnabled;
    }

    @Override
    public boolean isPlayerControllable() {
      return player;
    }

    @Override
    public void setPlayerControllable(boolean playerControllable) {
      this.player = playerControllable;
    }
  }
}
