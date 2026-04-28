package oogasalad.model.engine;

import oogasalad.model.engine.api.CollisionSolver;

/**
 * Immutable key for mapping object type pairs to {@link CollisionSolver} implementations.
 *
 * <p><strong>Purpose:</strong> Represents a unique pairing of two object types for use in
 * collision solver routing. This record canonicalizes the ordering of type pairs so lookups stay
 * consistent with {@link CollisionPhase#canonicalPair(oogasalad.model.engine.api.GameObject,
 * oogasalad.model.engine.api.GameObject)}.
 *
 * <p><strong>Assumptions:</strong> Both class parameters are non-null and represent concrete
 * component types. The {@code of} method is used to normalize pair ordering before storing in
 * maps.
 *
 * <p><strong>Dependencies:</strong> Used by {@link CollisionDispatcher} as a canonical map key.
 * Relies on {@link Class} names, with {@link Class#hashCode()} as a tie-breaker when names match.
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

  /**
   * Creates a canonicalized collision pair whose class order is deterministic by class name, then
   * class hash code.
   *
   * <p>Use this factory when building map keys so lookups do not depend on collision direction.
   *
   * @param objectA first object type in arbitrary order
   * @param objectB second object type in arbitrary order
   * @return normalized pair with lexicographically ordered class names and hash-code tie-breaking
   */
  public static CollisionPair of(Class<?> objectA, Class<?> objectB) {
    int compareByName = objectA.getName().compareTo(objectB.getName());
    if (compareByName < 0) {
      return new CollisionPair(objectA, objectB);
    }
    if (compareByName > 0) {
      return new CollisionPair(objectB, objectA);
    }
    if (objectA.hashCode() <= objectB.hashCode()) {
      return new CollisionPair(objectA, objectB);
    } else {
      return new CollisionPair(objectB, objectA);
    }
  }

  /**
   * Returns a readable representation of the normalized type pair.
   *
   * @return debug string containing both paired classes
   */
  @Override
  public String toString() {
    return "CollisionPair{" +
        "objectA=" + objectA +
        ", objectB=" + objectB +
        '}';
  }
}
