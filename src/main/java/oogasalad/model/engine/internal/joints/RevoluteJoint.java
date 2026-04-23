package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Hinge-style joint that keeps two local anchors coincident while allowing free relative rotation.
 */
public class RevoluteJoint extends AbstractJoint {

  /**
   * Creates a revolute joint using each body's local origin as the hinge anchor.
   */
  public RevoluteJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, new Vector(0, 0), new Vector(0, 0));
  }

  /**
   * Creates a revolute joint whose two local anchors must resolve to the same world-space point.
   */
  public RevoluteJoint(GameComponent objectA, GameComponent objectB, Vector localAnchorA,
      Vector localAnchorB) {
    super(objectA, objectB, localAnchorA, localAnchorB);
    if (getAnchorDelta().magnitude() > 1e-9) {
      throw new IllegalArgumentException(
          EngineError.REVOLUTE_JOINT_REQUIRES_SHARED_ANCHOR.message());
    }
  }

  /** Re-aligns the two anchors while leaving relative rotation unconstrained. */
  @Override
  public void apply(double dt) {
    Vector delta = getAnchorDelta();
    if (delta.magnitude() < 1e-9) {
      return;
    }
    applyLinearCorrection(delta, delta.multiply(0.2)); // TODO: determine the correct coefficient
  }
}
