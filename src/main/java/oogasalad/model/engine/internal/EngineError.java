package oogasalad.model.engine.internal;

public enum EngineError {
  NULL_POSE("pose must not be null"),
  NULL_VECTOR("vector must not be null"),
  NULL_HITBOX("hitbox must not be null"),
  MASS_MUST_BE_NON_NEGATIVE("mass must be non-negative"),
  FRICTION_MUST_BE_NON_NEGATIVE("friction must be non-negative"),
  MAX_HP_MUST_BE_NON_NEGATIVE("maxHp must be non-negative"),
  CURRENT_HP_OUT_OF_RANGE("currentHp must be within [0, maxHp]"),
  DAMAGE_MUST_BE_NON_NEGATIVE("damage amount must be non-negative"),
  REPAIR_MUST_BE_NON_NEGATIVE("repair amount must be non-negative");

  private final String message;

  EngineError(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
