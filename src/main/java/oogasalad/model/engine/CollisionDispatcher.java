package oogasalad.model.engine;

import java.util.HashMap;
import java.util.Map;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 * Routes collisions to appropriate {@link CollisionSolver} implementations based on object type
 * pairs.
 *
 * <p><strong>Purpose:</strong> Maintains a mapping of object type pairs to solvers, allowing the
 * engine to dynamically select the correct collision response strategy for each detected collision.
 * This decouples collision routing from response logic.
 *
 * <p><strong>Assumptions:</strong> Each {@link CollisionPair} inserted into the solver map has a
 * corresponding non-null {@link CollisionSolver}. Object types passed to lookups match registered
 * pair patterns.
 *
 * <p><strong>Dependencies:</strong> Depends on {@link CollisionPair}, {@link CollisionSolver},
 * {@link ImpulseCollisionSolver}, and {@link DynamicPassthroughSolver}. Uses {@link HashMap} for
 * internal storage.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * CollisionDispatcher dispatcher = new CollisionDispatcher();
 * CollisionSolver solver = dispatcher.getSolverMap()
 *     .get(new CollisionPair(DynamicComponent.class, DynamicComponent.class));
 * solver.solve(objA, objB, collision);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public class CollisionDispatcher {

  private final ImpulseCollisionSolver impulseSolver = new ImpulseCollisionSolver();
  private final DynamicPassthroughSolver dynamicPassthroughSolver = new DynamicPassthroughSolver();
  private Map<CollisionPair, CollisionSolver> solverMap;

  //  Map<CollisionPair, CollisionDetector> collisionDetectors;

  public CollisionDispatcher() {
    initializeSolverMap();
  }

  private void initializeSolverMap() {
    solverMap = new HashMap<>();
    solverMap.put(new CollisionPair(DynamicComponent.class, DynamicComponent.class),
        (objectA, objectB, collision) -> {
          impulseSolver.solve((DynamicComponent) objectA, (DynamicComponent) objectB, collision);
        });
//    collisionSolvers.put(new CollisionPair(DynamicComponent.class, StaticComponent.class), new ImpulseCollisionSolver());
//    solverMap.put(new CollisionPair(DynamicComponent.class, PassThroughComponent.class),
//        new DynamicPassthroughSolver());
  }

  public Map<CollisionPair, CollisionSolver> getSolverMap() {
    return solverMap;
  }
}
