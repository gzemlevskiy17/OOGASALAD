package oogasalad.controller.player.internal.parts;

import oogasalad.model.engine.api.Vector;

/**
 * @author Paulina Vvedenskaya
 */
public abstract class DrivePartSpec extends AbstractVehiclePartSpec {

  private static final double DEFAULT_MASS = 5.0;
  private static final double FRICTION = 0.3;
  private static final double MAX_HP = 100.0;
  private final double mass;
  private final Vector thrustDirection;
  private final double thrustForce;

  public DrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce) {
    super(paletteName, JointStyle.STRUCTURAL);
    this.mass = DEFAULT_MASS;
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

  public DrivePartSpec(String paletteName, Vector thrustDirection, double thrustForce,
      double mass, double width, double height) {
    super(paletteName, JointStyle.STRUCTURAL, width, height);
    this.mass = mass;
    this.thrustDirection = thrustDirection;
    this.thrustForce = thrustForce;
  }
}
