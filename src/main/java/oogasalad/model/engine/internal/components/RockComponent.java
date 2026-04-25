package oogasalad.model.engine.internal.components;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.ContactDamager;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.TypedComponent;

/**
 * A static map obstacle that damages each {@link Damageable} dynamic body it touches at most once.
 * Subsequent contacts from the same body are ignored, regardless of how many times the collision
 * solver fires per frame. Different rocks track their hits independently, so a body can still be
 * damaged by every rock it encounters.
 *
 * @author Paulina Vvedenskaya
 */
public class RockComponent extends StaticComponent implements TypedComponent, ContactDamager {

  private final double damagePerHit;
  private final Set<Damageable> alreadyHit =
      Collections.newSetFromMap(new IdentityHashMap<>());

  public RockComponent(double x, double y, double width, double height,
      double friction, double restitution, double damagePerHit) {
    super(x, y, 0,
        ConvexPolygon.axisAlignedBox(x - width / 2, y - height / 2, width, height),
        "", friction, restitution);
    if (damagePerHit < 0) {
      throw new IllegalArgumentException("damagePerHit must be non-negative");
    }
    this.damagePerHit = damagePerHit;
  }

  @Override
  public String getComponentType() {
    return "Rock";
  }

  @Override
  public void damageContact(Damageable target) {
    if (target.isDestroyed() || alreadyHit.contains(target)) {
      return;
    }
    alreadyHit.add(target);
    target.damage(damagePerHit);
    System.out.println("[rock] dealt " + damagePerHit + " damage to "
        + target.getClass().getSimpleName() + " (hp now " + target.getCurrentHp() + ")");
  }
}
