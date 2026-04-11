package oogasalad.model.engine;

public enum EngineError {
  MASS_MUST_BE_NON_NEGATIVE("mass must be non-negative"),
  FRICTION_MUST_BE_NON_NEGATIVE("friction must be non-negative"),
  THRUST_FORCE_MUST_BE_NON_NEGATIVE("thrust force must be non-negative"),
  MAX_HP_MUST_BE_NON_NEGATIVE("maxHp must be non-negative"),
  CURRENT_HP_OUT_OF_RANGE("currentHp must be within [0, maxHp]"),
  DAMAGE_MUST_BE_NON_NEGATIVE("damage amount must be non-negative"),
  REPAIR_MUST_BE_NON_NEGATIVE("repair amount must be non-negative"),
  JOINT_UNSUPPORTED_PASSTHROUGH("passthrough components cannot participate in joints"),
  JOINT_UNSUPPORTED_STATIC_STATIC("static-static joints are not supported"),
  JOINT_UNSUPPORTED_PAIR("joint pair must be dynamic-dynamic or dynamic-static"),
  JOINT_DISTANCE_PARAMS_NON_NEGATIVE("distance joint stiffness and damping must be non-negative"),
  JOINT_DISTANCE_RANGE_INVALID("distance joint minDistance cannot exceed maxDistance"),
  SPRING_JOINT_REQUIRES_POSITIVE_STIFFNESS("SpringJoint requires positive stiffness"),
  ROPE_JOINT_MAX_DISTANCE_TOO_SMALL("rope max distance cannot be less than initial distance"),
  REVOLUTE_JOINT_REQUIRES_SHARED_ANCHOR(
      "revolute joint anchors must resolve to the same world point on creation");

  private final String message;

  EngineError(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
