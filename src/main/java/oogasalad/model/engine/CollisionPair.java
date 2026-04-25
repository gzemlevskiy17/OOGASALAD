package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionSolver;

/**
 * Immutable key for mapping object type pairs to {@link CollisionSolver} implementations.
 *
 * <p><strong>Purpose:</strong> Represents a unique pairing of two object types for use in
 * collision solver routing. This record canonicalizes the ordering of type pairs to enable
 * consistent lookups regardless of collision reference direction.
 *
 * <p><strong>Assumptions:</strong> Both class parameters are non-null and represent concrete
 * component types. The {@code of} method is used to normalize pair ordering before storing in
 * maps.
 *
 * <p><strong>Dependencies:</strong> Used by {@link CollisionDispatcher} as a {@link HashMap} key.
 * Relies on {@link Class} equality and naming for ordering normalization.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * CollisionPair pair = new CollisionPair(DynamicComponent.class, DynamicComponent.class);
 * // or normalize first:
 * CollisionPair normalized = pair.of(TypeA.class, TypeB.class);
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public record CollisionPair(Class<?> objectA, Class<?> objectB) {

  public static CollisionPair of(Class<?> objectA, Class<?> objectB) {
    if (objectA.getName().compareTo(objectB.getName()) < 0) {
      return new CollisionPair(objectA, objectB);
    } else {
      return new CollisionPair(objectB, objectA);
    }
  }
}
