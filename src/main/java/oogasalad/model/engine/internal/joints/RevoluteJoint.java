package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.internal.components.GameComponent;

/**
 * Hinge-style joint that keeps two local anchors coincident while allowing free relative rotation.
 */
public class RevoluteJoint extends AbstractJoint {

  /** Creates a revolute joint using each body's local origin as the hinge anchor. */
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
      throw new IllegalArgumentException(EngineError.REVOLUTE_JOINT_REQUIRES_SHARED_ANCHOR.message());
    }
  }

  /** Re-aligns the two anchors while leaving relative rotation unconstrained. */
  @Override
  public void apply(double dt) {
    GameComponent objectA = getComponentA();
    GameComponent objectB = getComponentB();
    Vector delta = getAnchorDelta();
    if (delta.magnitude() < 1e-9) {
      return;
    }

    Vector positionA = objectA.getState().getLinearPosition();
    Vector positionB = objectB.getState().getLinearPosition();

    if (isDynamic(objectA) && isDynamic(objectB)) {
      Vector halfCorrection = delta.multiply(0.5);
      objectA.getState().setLinearPosition(positionA.add(halfCorrection));
      objectB.getState().setLinearPosition(positionB.subtract(halfCorrection));
      return;
    }
    if (isDynamic(objectA) && isStatic(objectB)) {
      objectA.getState().setLinearPosition(positionA.add(delta));
      return;
    }
    if (isStatic(objectA) && isDynamic(objectB)) {
      objectB.getState().setLinearPosition(positionB.subtract(delta));
    }
  }
}
