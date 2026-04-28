package oogasalad.model.engine;

import java.util.HashMap;
import java.util.Map;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Central registry that maps canonicalized type-pairs to {@link CollisionSolver} implementations.
 *
 * <p><strong>Purpose:</strong> Provide a deterministic lookup for collision response strategies
 * so the physics phase can delegate collision handling without embedding type-specific logic. The
 * dispatcher produces the canonical key using {@link CollisionPair#of(Class, Class)} and invokes
 * the corresponding solver with iteration flags that indicate warm-start and whether the current
 * pass should apply positional correction.
 *
 * <p><strong>Assumptions:</strong> Registered {@link CollisionPair} keys cover the pairs actually
 * encountered at runtime; callers normalize types via {@link CollisionPair#of(Class, Class)} when
 * building their own keys. Solvers must tolerate being called in velocity-only or position-only
 * passes (they receive flags to distinguish these cases).
 *
 * <p><strong>Dependencies:</strong> Relies on {@link CollisionPair} canonicalization and the
 * {@link CollisionSolver} contract. The dispatcher registers concrete solvers such as
 * {@link DynamicDynamicSolver}, {@link DynamicStaticSolver}, and {@link DynamicPassthroughSolver}.
 *
 * <p><strong>Example:</strong>
 * <pre>{@code
 * CollisionDispatcher dispatcher = new CollisionDispatcher();
 * CollisionPair key = CollisionPair.of(DynamicComponent.class, DynamicComponent.class);
 * CollisionSolver solver = dispatcher.getSolverMap().get(key);
 * // solvers expect: (objectA, objectB, collision, isFirstIteration, applyImpulse)
 * solver.solve(objA, objB, collision, true, true);
 * }</pre>
 *
 * <p><strong>Notes:</strong> This class is intentionally small; the dispatcher only routes to
 * registered solvers. If a direct key is not found the dispatcher searches super-classes via
 * {@link #findSuperClassPair(CollisionPair)} to allow generic solver registrations.
 *
 * @author Georgiy Zemlevskiy
 * @author Iurii Beliaev
 */
public class CollisionDispatcher {

  private static final Logger LOG = LogManager.getLogger(CollisionDispatcher.class);

  private final DynamicDynamicSolver impulseSolver = new DynamicDynamicSolver();
  private final DynamicPassthroughSolver dynamicPassthroughSolver = new DynamicPassthroughSolver();
  private final DynamicStaticSolver dynamicStaticSolver = new DynamicStaticSolver();

  private Map<CollisionPair, CollisionSolver> solverMap;

  public CollisionDispatcher() {
    initializeSolverMap();
  }

  /**
   * Dispatches a single {@link Collision} to the registered {@link CollisionSolver}.
   *
   * <p>Purpose: find the canonical solver for the runtime types, optionally fall back to a
   * superclass-registered solver, ensure the objects are passed in the solver's expected order, and
   * invoke the solver with the provided iteration flags.
   *
   * <p>Assumptions: {@code collision} contains non-null reference and incident objects and the
   * solver map has been initialized. Solvers must handle being called during velocity-only or
   * position-only passes according to the flags.
   *
   * @param collision        collision record to route
   * @param isFirstIteration true when this is the first solver iteration for this collision set
   * @param applyImpulse     true when the caller requests positional correction in addition to
   *                         velocity/impulse handling
   */
  public void dispatch(Collision collision, boolean isFirstIteration, boolean applyImpulse) {
    GameObject objectA = collision.getReferenceObject();
    GameObject objectB = collision.getIncidentObject();
    CollisionPair collisionPair = CollisionPair.of(objectA.getClass(), objectB.getClass());
    if (!solverMap.containsKey(collisionPair)) {
      collisionPair = findSuperClassPair(collisionPair);
    }
    if (collisionPair == null) {
      LOG.error("No valid pair found: {} vs {}",
          objectA.getClass(), objectB.getClass());
      return;
    }

    if (!collisionPair.objectA().isAssignableFrom(objectA.getClass())) {
      GameObject temp = objectA;
      objectA = objectB;
      objectB = temp;
    }

    CollisionSolver solver = solverMap.get(collisionPair);
    if (solver == null) {
      LOG.error("No solver found for pair: {} vs {}",
          collisionPair.objectA(), collisionPair.objectB());
    }
    solver.solve(objectA, objectB, collision, isFirstIteration, applyImpulse);
  }


  private CollisionPair findSuperClassPair(CollisionPair pair) {
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

  private void initializeSolverMap() {
    solverMap = new HashMap<>();
    solverMap.put(new CollisionPair(DynamicComponent.class, DynamicComponent.class),
        (objectA, objectB, collision, isFirstIteration, solveImpulse) -> {
          impulseSolver.solve((DynamicComponent) objectA, (DynamicComponent) objectB, collision,
              isFirstIteration, solveImpulse);
        });
    solverMap.put(new CollisionPair(DynamicComponent.class, StaticComponent.class),
        (objectA, objectB, collision, isFirstIteration, solveImpulse) -> {
          dynamicStaticSolver.solve((DynamicComponent) objectA, (StaticComponent) objectB,
              collision, isFirstIteration, solveImpulse);
        });
    solverMap.put(new CollisionPair(DynamicComponent.class, PassThroughComponent.class),
        (objectA, objectB, collision, isFirstIteration, solveImpulse) -> {
          dynamicPassthroughSolver.solve((DynamicComponent) objectA, (PassThroughComponent) objectB,
              collision, isFirstIteration, solveImpulse);
        });
  }
}
