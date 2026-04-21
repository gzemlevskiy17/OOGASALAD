package oogasalad.model.engine.internal.components;

import java.util.Set;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.view.core.api.SpriteRenderable;

/**
 * @author Xuye
 */
public abstract class VehicleComponent extends DynamicComponent implements SpriteRenderable {
  private static final Set<String> WHEEL_DISPLAY_NAMES = Set.of(
      "Wood Wheel", "Metal Wheel");

  private final String displayName;
  private final String spritePath;
  private final double bodyWidth;
  private final double bodyHeight;
  private double maxHp;
  private double currentHp;

  public VehicleComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag,
      double friction, double mass, double momentOfInertia, double restitution,
      boolean affectedByGravity,
      double maxHp, String displayName, String spritePath, double bodyWidth,
      double bodyHeight) {
    super(x, y, rotation, polygon, tag, friction, mass, momentOfInertia, restitution,
        affectedByGravity);
    setTag(DefaultTag.VEHICLE.value());
    if (maxHp < 0) {
      throw new IllegalArgumentException(EngineError.MAX_HP_MUST_BE_NON_NEGATIVE.message());
    }
    this.maxHp = maxHp;
    this.currentHp = maxHp;
    this.displayName = displayName;
    this.spritePath = spritePath;
    this.bodyWidth = bodyWidth;
    this.bodyHeight = bodyHeight;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getSpritePath() {
    return spritePath;
  }

  @Override
  public String getComponentType() {
    return displayName;
  }

  /** True for wheel parts; used for cheap wheel–terrain support in the engine. */
  public boolean isWheel() {
    return WHEEL_DISPLAY_NAMES.contains(displayName);
  }

  @Override
  public void updatePolygon() {
    setPolygon(ConvexPolygon.axisAlignedBox(
        getX() - bodyWidth / 2, getY() - bodyHeight / 2, bodyWidth, bodyHeight));
  }

  public double getBodyWidth() {
    return bodyWidth;
  }

  public double getBodyHeight() {
    return bodyHeight;
  }

  public double getMaxHp() {
    return maxHp;
  }

  public void setMaxHp(double maxHp) {
    if (maxHp < 0) {
      throw new IllegalArgumentException(EngineError.MAX_HP_MUST_BE_NON_NEGATIVE.message());
    }
    this.maxHp = maxHp;
  }

  public double getCurrentHp() {
    return currentHp;
  }

  public void setCurrentHp(double currentHp) {
    if (currentHp < 0 || currentHp > maxHp) {
      throw new IllegalArgumentException(EngineError.CURRENT_HP_OUT_OF_RANGE.message());
    }
    this.currentHp = currentHp;
  }

  public void damage(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException(EngineError.DAMAGE_MUST_BE_NON_NEGATIVE.message());
    }
    currentHp = Math.max(0, currentHp - amount);
    if (isDestroyed()) {
      deactivate();
    }
  }

  public void repair(double amount) {
    if (amount < 0) {
      throw new IllegalArgumentException(EngineError.REPAIR_MUST_BE_NON_NEGATIVE.message());
    }
    currentHp = Math.min(maxHp, currentHp + amount);
  }

  public boolean isDestroyed() {
    return currentHp <= 0;
  }

  @Override
  public void destroy() {
    currentHp = 0;
    super.destroy();
  }
}
