package oogasalad.model.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.joints.RevoluteJoint;
import oogasalad.model.engine.internal.joints.RigidJoint;

/**
 * Iteratively enforces joint constraints (position-level and velocity-level).
 *
 * @author Paulina Vvedenskaya
 */
class JointSolver {

  private final World world;

  JointSolver(World world) {
    this.world = world;
  }

  void applyAll(double elapsedTime, int iterations) {
    for (int i = 0; i < iterations; i++) {
      List<Joint> joints = shuffledJoints();
      for (Joint joint : joints) {
        joint.apply(elapsedTime);
      }
    }
  }

  void applyPositionConstraints(double elapsedTime, int iterations) {
    for (int i = 0; i < iterations; i++) {
      List<Joint> joints = shuffledJoints();
      for (Joint joint : joints) {
        if (isPositionConstraint(joint)) {
          joint.apply(elapsedTime);
        }
      }
    }
  }

  void applyVelocityConstraints() {
    List<Joint> joints = shuffledJoints();
    for (Joint joint : joints) {
      joint.applyVelocity();
    }
  }

  private boolean isPositionConstraint(Joint joint) {
    if (joint instanceof RevoluteJoint) {
      return true;
    }
    return joint instanceof RigidJoint;
  }

  private List<Joint> shuffledJoints() {
    List<Joint> joints = new ArrayList<>(world.getJoints());
    Collections.shuffle(joints);
    return joints;
  }
}
