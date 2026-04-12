package oogasalad.model.engine.internal.joints;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PassthroughComponent;
import oogasalad.model.engine.internal.components.StaticComponent;

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
    validatePair(this.objectA, this.objectB);
    this.restDistance = getWorldAnchorB().subtract(getWorldAnchorA()).magnitude();
  }

  @Override
  public GameObject getObjectA() {
    return objectA;
  }

  @Override
  public GameObject getObjectB() {
    return objectB;
  }

  /**
   * Returns the first endpoint as a concrete {@link GameComponent}.
   */
  protected GameComponent getComponentA() {
    return objectA;
  }

  /**
   * Returns the second endpoint as a concrete {@link GameComponent}.
   */
  protected GameComponent getComponentB() {
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
  protected double getCurrentDistance() {
    return getAnchorDelta().magnitude();
  }

  /**
   * Returns whether the component is a dynamic body.
   */
  protected boolean isDynamic(GameComponent component) {
    return component instanceof DynamicComponent;
  }

  /**
   * Returns whether the component is a static body.
   */
  protected boolean isStatic(GameComponent component) {
    return component instanceof StaticComponent;
  }

  /**
   * Returns whether the component is a passthrough/trigger-only body.
   */
  protected boolean isPassthrough(GameComponent component) {
    return component instanceof PassthroughComponent;
  }

  /**
   * Returns the anchor-to-anchor distance captured when the joint was created.
   */
  protected double getRestDistance() {
    return restDistance;
  }

  /**
   * Validates that the joint endpoints form a supported pair for the current engine.
   */
  private void validatePair(GameComponent objectA, GameComponent objectB) {
    if (isPassthrough(objectA) || isPassthrough(objectB)) {
      throw new IllegalArgumentException(EngineError.JOINT_UNSUPPORTED_PASSTHROUGH.message());
    }

    boolean objectAStatic = isStatic(objectA);
    boolean objectBStatic = isStatic(objectB);
    boolean objectADynamic = isDynamic(objectA);
    boolean objectBDynamic = isDynamic(objectB);

    if (objectAStatic && objectBStatic) {
      throw new IllegalArgumentException(EngineError.JOINT_UNSUPPORTED_STATIC_STATIC.message());
    }

    boolean supportedPair = (objectADynamic && objectBDynamic)
        || (objectADynamic && objectBStatic)
        || (objectAStatic && objectBDynamic);
    if (!supportedPair) {
      throw new IllegalArgumentException(EngineError.JOINT_UNSUPPORTED_PAIR.message());
    }
  }
}
