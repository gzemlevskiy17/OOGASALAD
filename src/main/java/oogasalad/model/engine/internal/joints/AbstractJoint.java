package oogasalad.model.engine.internal.joints;

import java.util.Objects;
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

  protected AbstractJoint(GameComponent objectA, GameComponent objectB) {
    this(objectA, objectB, new oogasalad.model.engine.api.Vector(0, 0),
        new oogasalad.model.engine.api.Vector(0, 0));
  }

  protected AbstractJoint(GameComponent objectA, GameComponent objectB,
      oogasalad.model.engine.api.Vector localAnchorA,
      oogasalad.model.engine.api.Vector localAnchorB) {
    this.objectA = Objects.requireNonNull(objectA, EngineError.NULL_GAME_COMPONENT.message());
    this.objectB = Objects.requireNonNull(objectB, EngineError.NULL_GAME_COMPONENT.message());
    this.localAnchorA = Objects.requireNonNull(localAnchorA, EngineError.NULL_VECTOR.message());
    this.localAnchorB = Objects.requireNonNull(localAnchorB, EngineError.NULL_VECTOR.message());
    validatePair(this.objectA, this.objectB);
    this.restDistance = getWorldAnchorB().subtract(getWorldAnchorA()).magnitude();
  }

  @Override
  public GameObject getObjectA() {return objectA;}
  @Override
  public GameObject getObjectB() {return objectB;}

  protected GameComponent getComponentA() {return objectA;}

  protected GameComponent getComponentB() {return objectB;}

  public oogasalad.model.engine.api.Vector getLocalAnchorA() {
    return localAnchorA;
  }

  public oogasalad.model.engine.api.Vector getLocalAnchorB() {
    return localAnchorB;
  }

  public oogasalad.model.engine.api.Vector getWorldAnchorA() {
    return objectA.getState().getLinearPosition().add(localAnchorA.rotate(objectA.getRotation()));
  }

  public oogasalad.model.engine.api.Vector getWorldAnchorB() {
    return objectB.getState().getLinearPosition().add(localAnchorB.rotate(objectB.getRotation()));
  }

  protected oogasalad.model.engine.api.Vector getAnchorDelta() {
    return getWorldAnchorB().subtract(getWorldAnchorA());
  }

  protected double getCurrentDistance() {return getAnchorDelta().magnitude();}

  protected boolean isDynamic(GameComponent component) {return component instanceof DynamicComponent;}

  protected boolean isStatic(GameComponent component) {return component instanceof StaticComponent;}

  protected boolean isPassthrough(GameComponent component) {return component instanceof PassthroughComponent;}

  protected double getRestDistance() {return restDistance;}

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
