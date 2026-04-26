package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.EngineError;

/**
 * Shared base type for collidable components with friction and restitution.
 *
 * <p><strong>Purpose:</strong> Adds material response properties used by impulse resolution and
 * defines inverse-mass/inverse-inertia contracts for dynamic and static subclasses.
 *
 * <p><strong>Assumptions:</strong> Friction is non-negative and restitution is in [0, 1].
 *
 * <p><strong>Dependencies:</strong> Extends {@link GameComponent} geometry/transform data and is
 * consumed by collision solvers through polymorphic mass-property APIs.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * double invMass = body.getInverseMass();
 * double e = body.getRestitution();
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 */
public abstract class RigidBody extends GameComponent {

  private double friction;
  private double restitution;

  /**
   * Creates a rigid body with transform, geometry, and material properties.
   *
   * @param x           initial x position
   * @param y           initial y position
   * @param rotation    initial angular position in radians
   * @param polygon     collision polygon in world coordinates
   * @param tag         lookup tag for gameplay queries
   * @param friction    friction coefficient (must be non-negative)
   * @param restitution restitution coefficient in [0, 1]
   * @throws IllegalArgumentException when friction or restitution is out of range
   */
  public RigidBody(double x, double y, double rotation, ConvexPolygon polygon,
      String tag, double friction, double restitution) {
    super(x, y, rotation, polygon, tag);
    setFriction(friction);
    setRestitution(restitution);
  }


  public double getFriction() {
    return friction;
  }

  /**
   * Sets friction coefficient.
   *
   * @param friction non-negative friction coefficient
   * @throws IllegalArgumentException when {@code friction < 0}
   */
  public void setFriction(double friction) {
    if (friction < 0) {
      throw new IllegalArgumentException(EngineError.FRICTION_MUST_BE_NON_NEGATIVE.message());
    }
    this.friction = friction;
  }

  public double getRestitution() {
    return restitution;
  }

  /**
   * Sets restitution (bounciness).
   *
   * @param restitution restitution coefficient in [0, 1]
   * @throws IllegalArgumentException when restitution is outside [0, 1]
   */
  public void setRestitution(double restitution) {
    if (restitution < 0 || restitution > 1) {
      throw new IllegalArgumentException(EngineError.RESTITUTION_OUT_OF_RANGE.message());
    }
    this.restitution = restitution;
  }

  /**
   * Returns inverse mass used by impulse and integration calculations.
   *
   * @return inverse mass, or 0 for immovable bodies
   */
  public abstract double getInverseMass();

  /**
   * Returns inverse moment of inertia used for angular impulse response.
   *
   * @return inverse inertia, or 0 for bodies that do not rotate dynamically
   */
  public abstract double getInverseMomentOfInertia();
}
