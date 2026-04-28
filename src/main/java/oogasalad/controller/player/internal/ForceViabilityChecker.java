package oogasalad.controller.player.internal;

import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.Vector;

/**
 * Checks whether authored button forces are strong enough to visibly move their components given
 * the world's gravity. Used at gameplay start to warn players, and in the authoring environment to
 * suggest viable force ranges.
 *
 * @author Paulina Vvedenskaya
 */
final class ForceViabilityChecker {

  private static final double VIABILITY_RATIO = 0.3;

  private ForceViabilityChecker() {
  }

  static List<String> check(GameConfig config, Vector gravity) {
    List<String> warnings = new ArrayList<>();
    if (config == null) {
      return warnings;
    }
    double gravityMag = gravity.magnitude();
    for (ComponentInfo ci : config.components()) {
      if (ci.buttons().isEmpty()) {
        continue;
      }
      double weight = ci.mass() * gravityMag;
      double maxForce = 0;
      for (ButtonInfo button : ci.buttons()) {
        double mag = new Vector(button.directionX(), button.directionY()).magnitude();
        if (mag > maxForce) {
          maxForce = mag;
        }
      }
      if (maxForce < weight * VIABILITY_RATIO) {
        warnings.add(ci.id() + ": max button force " + (int) maxForce
            + " is too small for weight " + (int) weight
            + " (need at least " + (int) (weight * VIABILITY_RATIO) + ")");
      }
    }
    return warnings;
  }

  static double suggestedMinForce(double mass, double gravityMagnitude) {
    return mass * gravityMagnitude * VIABILITY_RATIO;
  }
}
