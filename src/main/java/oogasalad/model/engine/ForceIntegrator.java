package oogasalad.model.engine;

import java.util.Collection;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 * Applies external forces (gravity, drag, drive) to dynamic components and manages sleep states.
 *
 * @author Paulina Vvedenskaya
 */
class ForceIntegrator {

  private final Vector gravity;
  private final World world;

  ForceIntegrator(World world, Vector gravity) {
    this.world = world;
    this.gravity = gravity;
  }

  void resetForces(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.resetForces();
      }
    }
  }

  void applyForces(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        applyForcesToComponent(dynamicComponent);
      }
    }
  }

  void applyForcesToAcceleration(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.applyForceToAcceleration();
      }
    }
  }

  void updateSleepStates(Collection<GameObject> objects, double elapsedTime) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.updateSleepState(elapsedTime);
      }
    }
  }

  private void applyForcesToComponent(DynamicComponent dynamicComponent) {
    if (dynamicComponent.isAffectedByGravity()) {
      dynamicComponent.addForce(gravity.multiply(dynamicComponent.getMass()));
      dynamicComponent.addForce(dynamicComponent.getDynamicState().getLinearVelocity()
          .multiply(-world.getDragCoefficient()));
      dynamicComponent.addTorque(dynamicComponent.getDynamicState().getAngularVelocity()
          * -world.getDragCoefficient());
    }

    if (dynamicComponent instanceof Driveable driveable) {
      dynamicComponent.addForce(driveable.getDriveForce());
    }
  }
}
