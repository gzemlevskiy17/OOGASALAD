package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.internal.components.GameComponent;
/**
 * Shared base for concrete joint implementations.
 *
 * <p>Stores the two endpoint components and centralizes validation of the minimal joint rules used
 * by the current engine: only dynamic-dynamic and dynamic-static pairs are supported.
 */
public abstract class AbstractJoint implements Joint {

  private final GameComponent objectA;
  private final GameComponent objectB;
  private final oogasalad.model.engine.api.Vector localAnchorA;
  private final oogasalad.model.engine.api.Vector localAnchorB;
  private final double restDistance;

  /**
   * Creates a joint whose anchors default to each component's local origin.
   *
   * @param objectA first connected component
   * @param objectB second connected component
   */
  protected AbstractJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, new oogasalad.model.engine.api.Vector(0, 0),
        new oogasalad.model.engine.api.Vector(0, 0));
  }

  /**
   * Creates a joint with explicit local-space anchors on each endpoint.
   *
   * @param objectA      first connected component
   * @param objectB      second connected component
   * @param localAnchorA anchor relative to {@code objectA}'s local frame
   * @param localAnchorB anchor relative to {@code objectB}'s local frame
   */
  protected AbstractJoint(GameComponent objectA, GameComponent objectB,
      oogasalad.model.engine.api.Vector localAnchorA,
      oogasalad.model.engine.api.Vector localAnchorB) {
    this.objectA = objectA;
    this.objectB = objectB;
    this.localAnchorA = localAnchorA;
    this.localAnchorB = localAnchorB;
    this.restDistance = getWorldAnchorB().subtract(getWorldAnchorA()).magnitude();
  }

  @Override
  public GameComponent getComponentA() {
    return objectA;
  }
  @Override
  public GameComponent getComponentB() {
    return objectB;
  }

  /**
   * Returns the anchor stored in {@code objectA}'s local coordinate system.
   */
  public oogasalad.model.engine.api.Vector getLocalAnchorA() {
    return localAnchorA;
  }

  /**
   * Returns the anchor stored in {@code objectB}'s local coordinate system.
   */
  public oogasalad.model.engine.api.Vector getLocalAnchorB() {
    return localAnchorB;
  }

  /**
   * Resolves {@code objectA}'s local anchor into world-space coordinates.
   */
  public oogasalad.model.engine.api.Vector getWorldAnchorA() {
    return objectA.getState().getLinearPosition().add(localAnchorA.rotate(objectA.getRotation()));
  }

  /**
   * Resolves {@code objectB}'s local anchor into world-space coordinates.
   */
  public oogasalad.model.engine.api.Vector getWorldAnchorB() {
    return objectB.getState().getLinearPosition().add(localAnchorB.rotate(objectB.getRotation()));
  }

  /**
   * Returns the world-space vector from anchor A to anchor B.
   */
  protected oogasalad.model.engine.api.Vector getAnchorDelta() {
    return getWorldAnchorB().subtract(getWorldAnchorA());
  }

  /**
   * Returns the current distance between the two resolved world anchors.
   */
  protected double getCurrentDistance() {return getAnchorDelta().magnitude();}

  /**
   * Returns the anchor-to-anchor distance captured when the joint was created.
   */
  protected double getRestDistance() {return restDistance;}

  /**
   * Resolves a scalar distance error into equal-and-opposite positional corrections along the
   * current anchor direction.
   */
  protected void applyDistanceCorrection(double distanceError) {
    double currentDistance = getCurrentDistance();
    if (currentDistance == 0.0 || Math.abs(distanceError) < 1e-9) {
      return;
    }

    oogasalad.model.engine.api.Vector correction =
        getAnchorDelta().normalize().multiply(distanceError);
    applyLinearCorrection(correction, correction.multiply(-1));
  }

  /**
   * Applies equal-and-opposite positional corrections, distributing motion by inverse mass.
   *
   * <p>Bodies with non-positive mass receive zero motion, which makes static endpoints stay fixed
   * without type-specific branching in concrete joint implementations.
   */
  protected void applyLinearCorrection(
      oogasalad.model.engine.api.Vector correctionA,
      oogasalad.model.engine.api.Vector correctionB) {
    double inverseMassA = inverseMass(objectA);
    double inverseMassB = inverseMass(objectB);
    double totalInverseMass = inverseMassA + inverseMassB;

    if (totalInverseMass == 0.0) {
      return;
    }
    if (inverseMassA > 0.0) {
      objectA.getState().setLinearPosition(objectA.getState().getLinearPosition()
          .add(correctionA.multiply(inverseMassA / totalInverseMass)));
    }
    if (inverseMassB > 0.0) {
      objectB.getState().setLinearPosition(objectB.getState().getLinearPosition()
          .add(correctionB.multiply(inverseMassB / totalInverseMass)));
    }
  }

  /**
   * Applies equal-and-opposite linear forces to movable endpoints only.
   */
  protected void applyLinearForce(oogasalad.model.engine.api.Vector forceA,
      oogasalad.model.engine.api.Vector forceB) {
    if (inverseMass(objectA) > 0.0) {
      objectA.addForce(forceA);
    }
    if (inverseMass(objectB) > 0.0) {
      objectB.addForce(forceB);
    }
  }

  private double inverseMass(GameComponent component) {
    return component.getMass() > 0.0 ? 1.0 / component.getMass() : 0.0;
  }
}
