package oogasalad.model.engine.internal.components;

import java.util.Objects;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.PickupEffect;
import oogasalad.model.engine.api.TypedComponent;

/**
 * A pass-through world object that, on contact with a dynamic body, fires a single
 * {@link PickupEffect} and removes itself. The kind of pickup (heal, damage, score, buff) is
 * determined entirely by the effect it composes — there is no per-kind subclass. The
 * {@code typeName} drives sprite resolution via the palette (e.g. "Heart" → bp_heart.png).
 *
 * <p>Consumption is idempotent: a second {@code applyTo} call after consumption is a no-op. This
 * guards against the same collision being dispatched more than once before {@link #destroy()} takes
 * effect within a frame.
 *
 * @author Paulina Vvedenskaya
 */
public final class PickupComponent extends PassThroughComponent implements TypedComponent {

  private final PickupEffect effect;
  private final String typeName;
  private boolean consumed;
  private Runnable onDestroyHook = () -> {
  };

  public PickupComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      String typeName, PickupEffect effect) {
    super(x, y, rotation, polygon, tag);
    this.typeName = Objects.requireNonNull(typeName);
    this.effect = Objects.requireNonNull(effect);
    this.consumed = false;
  }

  private static String describe(DynamicComponent target) {
    String tag = target.getTag();
    return target.getClass().getSimpleName() + (tag.isEmpty() ? "" : " (" + tag + ")");
  }

  /**
   * Sets a hook the pickup runs after consumption. Used by the level loader to bind
   * {@code world.destroyObject(this)} so the pickup is fully removed (not merely deactivated) when
   * used.
   */
  public void setOnDestroyHook(Runnable hook) {
    this.onDestroyHook = Objects.requireNonNull(hook);
  }

  public boolean isConsumed() {
    return consumed;
  }

  @Override
  public String getComponentType() {
    return typeName;
  }

  public void applyTo(DynamicComponent target) {
    Objects.requireNonNull(target);
    if (consumed) {
      return;
    }
    consumed = true;
    System.out.println("[pickup] " + typeName + " consumed by " + describe(target));
    effect.apply(target);
    destroy();
    onDestroyHook.run();
  }
}
