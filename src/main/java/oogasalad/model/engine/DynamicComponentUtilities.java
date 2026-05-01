package oogasalad.model.engine;

import java.util.List;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.PhysicsEnvironment;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 * Utility helpers for batch-updating dynamic components during the engine step.
 *
 * <p>The engine uses these helpers to reset forces, apply accumulated forces, integrate motion,
 * refresh polygons after movement or positional correction, and update sleep state.
 *
 * @author Paulina Vvedenskaya
 * @author Georgiy Zemlevskiy
 * @author Iurii Beliaev
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

    Vector externalForce = dynamicComponent.getExternalForceTotal();
    if (externalForce.magnitude() > 0) {
      if (dynamicComponent.isSleeping()) {
        dynamicComponent.setSleeping(false);
      }
      dynamicComponent.addForce(externalForce.rotate(dynamicComponent.getStaticState().getAngularPosition()));
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
