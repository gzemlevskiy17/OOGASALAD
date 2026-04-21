package oogasalad.model.engine;

import java.util.HashMap;
import java.util.Map;
import oogasalad.model.engine.api.CollisionSolver;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.PassThroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;

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
 * {@link DynamicDynamicSolver}, and {@link DynamicPassthroughSolver}. Uses {@link HashMap} for
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
 * @author Iurii Beliaev
 */
public class CollisionDispatcher {

  private final DynamicDynamicSolver impulseSolver = new DynamicDynamicSolver();
  private final DynamicPassthroughSolver dynamicPassthroughSolver = new DynamicPassthroughSolver();
  private final DynamicStaticSolver dynamicStaticSolver = new DynamicStaticSolver();

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
    solverMap.put(new CollisionPair(DynamicComponent.class, StaticComponent.class),
        (objectA, objectB, collision) -> {
          dynamicStaticSolver.solve((DynamicComponent) objectA, (StaticComponent) objectB, collision);
        });
    solverMap.put(new CollisionPair(DynamicComponent.class, PassThroughComponent.class),
        (objectA, objectB, collision) -> {
          dynamicPassthroughSolver.solve((DynamicComponent) objectA, (PassThroughComponent) objectB, collision);
          dynamicPassthroughSolver.solve((DynamicComponent) objectA, (PassThroughComponent) objectB,
              collision);
    });
  }

  public Map<CollisionPair, CollisionSolver> getSolverMap() {
    return solverMap;
  }
}
