package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.api.Vector;

/**
 * Base palette specification for any vehicle part that behaves like a drive.
 *
 * <p>Drive parts carry their own mass, thrust direction, thrust force, friction, and hit points so
 * that the vehicle builder can instantiate either fixed- or relative-direction engines from the
 * same data model. Subclasses decide which concrete drive component to build, but they all rely on
 * this shared storage for the nontrivial drive configuration.
 *
 * <p>Assumptions: the supplied thrust direction should already represent the intended local or
 * world direction for the drive subtype, and the force should be positive if the drive is meant to
 * propel the vehicle forward. The default constructor uses the class defaults for mass and body
 * size; the full constructor is used when loading custom content from a game configuration.
 *
 * <p>Example: {@code new FixedDirectionDrivePartSpec("Balloon", new Vector(0, -1), 3000.0)}.
 *
 * @author Paulina Vvedenskaya
 * @author Georgiy Zemlevskiy
 */
public abstract class DrivePartSpec extends AbstractVehiclePartSpec {

  private static final double DEFAULT_MASS = 5.0;
  private static final double FRICTION = 0.3;
  private static final double MAX_HP = 100.0;
  private final double mass;
  private final Vector thrustDirection;
  private final double thrustForce;

  /**
   * Creates a drive spec with the standard drive mass and the default body dimensions inherited
   * from the base vehicle-part implementation.
   *
   * @param paletteName palette name used by the editor and vehicle factory
   * @param thrustDirection direction the drive should push in
   * @param thrustForce magnitude of the drive force
   */
  public DrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce) {
    super(paletteName, JointStyle.STRUCTURAL);
    this.mass = DEFAULT_MASS;
    this.thrustDirection = thrustDirection;
    this.thrustForce = thrustForce;
  }

  /**
   * Creates a drive spec with explicit physical properties for content loaded from configuration.
   *
   * @param paletteName palette name used by the editor and vehicle factory
   * @param thrustDirection direction the drive should push in
   * @param thrustForce magnitude of the drive force
   * @param mass component mass to use when the part is instantiated
   * @param width body width to use when the part is instantiated
   * @param height body height to use when the part is instantiated
   */
  public DrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce,
      double mass, double width, double height) {
    super(paletteName, JointStyle.STRUCTURAL, width, height);
    this.mass = mass;
    this.thrustDirection = thrustDirection;
    this.thrustForce = thrustForce;
  }

  public double getMass() {
    return mass;
  }

  public Vector getThrustDirection() {
    return thrustDirection;
  }

  public double getThrustForce() {
    return thrustForce;
  }

  public double getFriction() {
    return FRICTION;
  }

  public double getMaxHP() {
    return MAX_HP;
  }
}
