package oogasalad.model.engine;

import java.util.List;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 *
 * @author Georgiy Zemlevskiy
 */
public class DynamicComponentUtilities {

  public static void resetForces(List<DynamicComponent> dynamicComponents) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.resetForces();
    }
  }

  public static void applyForces(List<DynamicComponent> dynamicComponents, World world) {
    PhysicsEnvironment physics = world.getPhysicsEnvironment();
    Vector gravity = physics.gravity();
    double drag = physics.drag();
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      applyForcesToComponent(dynamicComponent, gravity, drag);
    }
  }

  public static void applyForcesToAcceleration(List<DynamicComponent> dynamicComponents) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.applyForceToAcceleration();
    }
  }

  public static void updateSleepStates(List<DynamicComponent> dynamicComponents, double elapsedTime) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.updateSleepState(elapsedTime);
    }
  }

  private static void applyForcesToComponent(DynamicComponent dynamicComponent, Vector gravity,
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

  public static void integrateDynamicComponents(List<DynamicComponent> dynamicComponents,
      double elapsedTime) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.integrateObjectState(elapsedTime);
    }
  }

  public static void updateDynamicPolygons(List<DynamicComponent> dynamicComponents) {
    for (DynamicComponent dynamicComponent : dynamicComponents) {
      dynamicComponent.updatePolygon();
    }
  }
}
