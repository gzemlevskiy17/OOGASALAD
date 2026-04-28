package oogasalad.model.engine;

import java.util.Collection;
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

  private static final boolean DEBUG_FORCES = true;
  private int debugTickCounter = 0;

  private final World world;

  ForceIntegrator(World world) {
    this.world = world;
  }

  void resetForces(Collection<GameObject> objects) {
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        dynamicComponent.resetForces();
      }
    }
  }

  void applyForces(Collection<GameObject> objects) {
    PhysicsEnvironment physics = world.getPhysicsEnvironment();
    Vector gravity = physics.gravity();
    double drag = physics.drag();
    for (GameObject object : objects) {
      if (object instanceof DynamicComponent dynamicComponent) {
        applyForcesToComponent(dynamicComponent, gravity, drag);
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

    Vector externalForce = dynamicComponent.getExternalForceTotal();
    if (externalForce.magnitude() > 0) {
      if (dynamicComponent.isSleeping()) {
        dynamicComponent.setSleeping(false);
      }
      dynamicComponent.addForce(externalForce);
    }

    if (DEBUG_FORCES && externalForce.magnitude() > 0) {
      debugTickCounter++;
      if (debugTickCounter % 60 == 1) {
        Vector totalForce = dynamicComponent.getForce();
        Vector vel = dynamicComponent.getDynamicState().getLinearVelocity();
        Vector pos = dynamicComponent.getStaticState().getLinearPosition();
        System.out.println("[ForceDebug] tag=" + dynamicComponent.getTag()
            + " mass=" + dynamicComponent.getMass()
            + " sleeping=" + dynamicComponent.isSleeping()
            + " extForce=(" + externalForce.x() + ", " + externalForce.y() + ")"
            + " totalForce=(" + totalForce.x() + ", " + totalForce.y() + ")"
            + " vel=(" + String.format("%.2f", vel.x()) + ", " + String.format("%.2f", vel.y()) + ")"
            + " pos=(" + String.format("%.1f", pos.x()) + ", " + String.format("%.1f", pos.y()) + ")"
            + " gravity=(" + gravity.x() + ", " + gravity.y() + ")");
      }
    }
  }
}
