package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.ContactDamager;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.TypedComponent;

/**
 * A static map obstacle that damages any {@link Damageable} dynamic body it touches. Cooldown is
 * counted in contact frames (calls to {@link #damageContact}); leaving contact freezes the
 * counter rather than resetting it, so a body that briefly bounces off and re-touches still has
 * to wait for its next hit window.
 *
 * @author Paulina Vvedenskaya
 */
public class RockComponent extends StaticComponent implements TypedComponent, ContactDamager {

  private final double damagePerHit;
  private final int cooldownFrames;
  private int framesUntilNextHit;

  public RockComponent(double x, double y, double width, double height,
      double friction, double restitution, double damagePerHit, int cooldownFrames) {
    super(x, y, 0,
        ConvexPolygon.axisAlignedBox(x - width / 2, y - height / 2, width, height),
        "", friction, restitution);
    if (damagePerHit < 0) {
      throw new IllegalArgumentException("damagePerHit must be non-negative");
    }
    if (cooldownFrames < 0) {
      throw new IllegalArgumentException("cooldownFrames must be non-negative");
    }
    this.damagePerHit = damagePerHit;
    this.cooldownFrames = cooldownFrames;
    this.framesUntilNextHit = 0;
  }

  @Override
  public String getComponentType() {
    return "Rock";
  }

  @Override
  public void damageContact(Damageable target) {
    if (framesUntilNextHit > 0) {
      framesUntilNextHit--;
      return;
    }
    target.damage(damagePerHit);
    framesUntilNextHit = cooldownFrames;
  }
}
