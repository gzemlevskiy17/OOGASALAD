package oogasalad.model.engine.internal.components;

import java.util.Objects;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.PickupEffect;

/**
 * A pass-through world object that, on contact with a dynamic body, fires a single
 * {@link PickupEffect} and removes itself. The kind of pickup (heal, damage, score, buff) is
 * determined entirely by the effect it composes — there is no per-kind subclass.
 *
 * <p>Consumption is idempotent: a second {@code applyTo} call after consumption is a no-op. This
 * guards against the same collision being dispatched more than once before {@link #destroy()} takes
 * effect within a frame.
 *
 * @author Paulina Vvedenskaya
 */
public final class PickupComponent extends PassThroughComponent {

  private final PickupEffect effect;
  private boolean consumed;

  public PickupComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      PickupEffect effect) {
    super(x, y, rotation, polygon, tag);
    this.effect = Objects.requireNonNull(effect);
    this.consumed = false;
  }

  public boolean isConsumed() {
    return consumed;
  }

  public void applyTo(DynamicComponent target) {
    Objects.requireNonNull(target);
    if (consumed) {
      return;
    }
    consumed = true;
    effect.apply(target);
    destroy();
  }
}
