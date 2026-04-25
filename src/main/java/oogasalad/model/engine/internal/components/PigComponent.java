package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.TypedComponent;

/**
 * Concrete dynamic component tagged as pigs. Carries health so reward items (heart, bomb) can
 * affect the pig directly. Reaching zero HP is a loss condition handled by a checker; the pig
 * itself does not auto-deactivate at zero.
 *
 * @author Paulina Vvedenskaya
 */
public class PigComponent extends DynamicComponent implements TypedComponent, Damageable {

  private static final double DEFAULT_SIZE = 40.0;

  private final String componentType;
  private double maxHp;
  private double currentHp;

  public PigComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double mass, double momentOfInertia, double restitution,
      boolean affectedByGravity, double maxHp, String componentType) {
    super(x, y, rotation, polygon, tag, friction, mass, momentOfInertia, restitution,
        affectedByGravity);
    if (maxHp < 0) {
      throw new IllegalArgumentException(EngineError.MAX_HP_MUST_BE_NON_NEGATIVE.message());
    }
    this.maxHp = maxHp;
    this.currentHp = maxHp;
    this.componentType = componentType;
    setTag(DefaultTag.PIGS.value());
  }

  @Override
  public void updatePolygon() {
    setPolygon(ConvexPolygon.axisAlignedBox(
        x() - DEFAULT_SIZE / 2, y() - DEFAULT_SIZE / 2, DEFAULT_SIZE, DEFAULT_SIZE));
  }

  @Override
  public String getComponentType() {
    return componentType;
  }

  @Override
  public double getCurrentHp() {
    return currentHp;
  }

  @Override
  public void setCurrentHp(double hp) {
    if (hp < 0 || hp > maxHp) {
      throw new IllegalArgumentException(EngineError.CURRENT_HP_OUT_OF_RANGE.message());
    }
    this.currentHp = hp;
  }

  @Override
  public double getMaxHp() {
    return maxHp;
  }

  @Override
  public void setMaxHp(double maxHp) {
    if (maxHp < 0) {
      throw new IllegalArgumentException(EngineError.MAX_HP_MUST_BE_NON_NEGATIVE.message());
    }
    this.maxHp = maxHp;
  }
}
