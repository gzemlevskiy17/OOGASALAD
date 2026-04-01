package oogasalad.model.engine.internal;

import oogasalad.model.engine.EngineError;
import oogasalad.model.engine.api.Pose;

public abstract class VehicleComponent extends GameComponent {
  private double maxHp;
  private double currentHp;

  public VehicleComponent(Pose pose, double mass, double friction, double maxHp) {
    super(pose, mass, friction);
    if (maxHp < 0) {throw new IllegalArgumentException(EngineError.MAX_HP_MUST_BE_NON_NEGATIVE.message());}
    this.maxHp = maxHp;
    this.currentHp = maxHp;
  }

  public double getMaxHp() {return maxHp;}
  public void setMaxHp(double maxHp) {
    if (maxHp < 0) {throw new IllegalArgumentException(EngineError.MAX_HP_MUST_BE_NON_NEGATIVE.message());}
    this.maxHp = maxHp;}

  public double getCurrentHp() {return currentHp;}

  public void setCurrentHp(double currentHp) {
    if (currentHp < 0 || currentHp > maxHp) {throw new IllegalArgumentException(EngineError.CURRENT_HP_OUT_OF_RANGE.message());}
    this.currentHp = currentHp;
  }

  public void damage(double amount) {
    if (amount < 0) {throw new IllegalArgumentException(EngineError.DAMAGE_MUST_BE_NON_NEGATIVE.message());}
    currentHp = Math.max(0, currentHp - amount);
    if (isDestroyed()) {deactivate();}
  }

  public void repair(double amount) {
    if (amount < 0) {throw new IllegalArgumentException(EngineError.REPAIR_MUST_BE_NON_NEGATIVE.message());}
    currentHp = Math.min(maxHp, currentHp + amount);
  }

  public boolean isDestroyed() {return currentHp <= 0;}

  @Override
  public void destroy() {
    currentHp = 0;
    super.destroy();
  }
}
