package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Fully rigid attachment between two bodies: keeps a shared anchor coincident in world space (like
 * a pin) AND preserves the initial relative rotation (what distinguishes it from a revolute
 * joint).
 *
 * <p>A single WeldJoint removes all 3 relative degrees of freedom in 2D, so one joint per pair
 * is enough to lock the pair as a single rigid body.
 *
 * @author Paulina Vvedenskaya
 */
public class WeldJoint extends AbstractJoint {

  private final double initialRelativeRotation;

  public WeldJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0));
  }

  public WeldJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB) {
    super(objectA, objectB, localAnchorA, localAnchorB);
    if (getAnchorDelta().magnitude() > 1e-9) {
      throw new IllegalArgumentException(
          "WeldJoint requires anchors that resolve to the same world point");
    }
    this.initialRelativeRotation = objectB.getRotation() - objectA.getRotation();
  }

  @Override
  public void apply(double dt) {
    correctPin();
    correctRotation();
  }

  private void correctPin() {
    Vector delta = getAnchorDelta();
    if (delta.magnitude() < 1e-9) {
      return;
    }
    applyLinearCorrection(delta, delta.multiply(-1));
  }

  private void correctRotation() {
    GameComponent a = getComponentA();
    GameComponent b = getComponentB();
    double error = (b.getRotation() - a.getRotation()) - initialRelativeRotation;
    if (Math.abs(error) < 1e-9) {
      return;
    }

    double invMassA = a.getMass() > 0.0 ? 1.0 / a.getMass() : 0.0;
    double invMassB = b.getMass() > 0.0 ? 1.0 / b.getMass() : 0.0;
    double total = invMassA + invMassB;
    if (total == 0.0) {
      return;
    }

    if (invMassA > 0.0) {
      a.getState().setAngularPosition(
          a.getState().getAngularPosition() + error * (invMassA / total));
    }
    if (invMassB > 0.0) {
      b.getState().setAngularPosition(
          b.getState().getAngularPosition() - error * (invMassB / total));
    }
  }
}
