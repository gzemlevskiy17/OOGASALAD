package oogasalad.model.engine.internal.components;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.HasCollisionEffect;
import oogasalad.model.engine.api.TypedComponent;

/**
 * A static map obstacle that fires its {@link CollisionEffect} on the first contact with each
 * dynamic body. Subsequent contacts from the same body are ignored, so the effect can't double-
 * apply when the solver fires multiple times per frame. Different rocks track their hits
 * independently.
 *
 * @author Paulina Vvedenskaya
 */
public class RockComponent extends StaticComponent implements TypedComponent, HasCollisionEffect {

  private final CollisionEffect effect;
  private final Set<GameObject> alreadyHit =
      Collections.newSetFromMap(new IdentityHashMap<>());

  public RockComponent(double x, double y, double width, double height,
      double friction, double restitution, CollisionEffect effect) {
    super(x, y, 0,
        ConvexPolygon.axisAlignedBox(x - width / 2, y - height / 2, width, height),
        "", friction, restitution);
    this.effect = Objects.requireNonNull(effect);
  }

  @Override
  public String getComponentType() {
    return "Rock";
  }

  @Override
  public CollisionEffect getCollisionEffect() {
    return target -> {
      if (alreadyHit.add(target)) {
        effect.apply(target);
      }
    };
  }
}
