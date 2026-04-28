package oogasalad.model.engine;

import java.util.Collection;
import java.util.List;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 * Applies external forces (gravity, drag, drive) to dynamic components and manages sleep states.
 *
 * @author Paulina Vvedenskaya
 */
class ForceIntegrator {

  private final World world;

  ForceIntegrator(World world) {
    this.world = world;
  }

  void resetForces(List<DynamicComponent> dynamicComponents) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.resetForces();
    }
  }

  void applyForces(List<DynamicComponent> dynamicComponents) {
    PhysicsEnvironment physics = world.getPhysicsEnvironment();
    Vector gravity = physics.gravity();
    double drag = physics.drag();
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      applyForcesToComponent(dynamicComponent, gravity, drag);
    }
  }

  void applyForcesToAcceleration(List<DynamicComponent> dynamicComponents) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.applyForceToAcceleration();
    }
  }

  void updateSleepStates(List<DynamicComponent> dynamicComponents, double elapsedTime) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.updateSleepState(elapsedTime);
    }
  }

  private void applyForcesToComponent(DynamicComponent dynamicComponent, Vector gravity,
      double drag) {
    if (dynamicComponent.isAffectedByGravity()) {
      dynamicComponent.addForce(gravity.multiply(dynamicComponent.getMass()));
      dynamicComponent.addForce(dynamicComponent.getDynamicState().getLinearVelocity()
          .multiply(-drag));
      dynamicComponent.addTorque(dynamicComponent.getDynamicState().getAngularVelocity()
          * -drag);
    }

    if (dynamicComponent instanceof Driveable driveable) {
      dynamicComponent.addForce(driveable.getDriveForce());
    }
  }
}
