package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.Vector;

/**
 * Base rigid-body component for immovable simulation objects.
 *
 * <p><strong>Purpose:</strong> Represents terrain/obstacles that participate in collision response
 * with material properties but do not translate or rotate due to forces.
 *
 * <p><strong>Assumptions:</strong> Static objects are externally positioned and should ignore
 * runtime force application.
 *
 * <p><strong>Dependencies:</strong> Extends {@link RigidBody} and is used by impulse solvers where
 * zero inverse mass/inertia implies no dynamic motion.
 *
 * <p><strong>Example:</strong>
 *
 * <pre>{@code
 * StaticComponent wall = ...;
 * double invMass = wall.getInverseMass(); // 0.0
 * }</pre>
 *
 * @author Georgiy Zemlevskiy
 * @author Iurii Beliaev
 */
public abstract class StaticComponent extends RigidBody {

  /**
   * Creates a static component with explicit geometry and material properties.
   *
   * @param x           initial x position
   * @param y           initial y position
   * @param rotation    initial angular position in radians
   * @param polygon     collision polygon in world coordinates
   * @param tag         lookup tag for gameplay queries
   * @param friction    friction coefficient (non-negative)
   * @param restitution restitution coefficient in [0, 1]
   */
  public StaticComponent(double x, double y, double rotation, ConvexPolygon polygon,
      String tag, double friction, double restitution) {
    super(x, y, rotation, polygon, tag, friction, restitution);
  }

  /**
   * Compatibility constructor for lightweight engine tests.
   */
  public StaticComponent(double x, double y, double rotation, double friction,
      double restitution) {
    this(x, y, rotation, ConvexPolygon.axisAlignedBox(x, y, 1, 1), "", friction, restitution);
  }


  @Override
  public double getInverseMass() {
    return 0.0;
  }

  @Override
  public double getInverseMomentOfInertia() {
    return 0.0;
  }

  @Override
  public void addForce(Vector force) {
    // immovable terrain: ignore
    //TODO: remove?
  }

  @Override
  public boolean isAffectedByGravity() {
    return false;
  }

  @Override
  public boolean isTrigger() {
    return false;
  }

}
