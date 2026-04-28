package oogasalad.model.engine;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Enumerates engine-facing error messages loaded from {@code EngineError.properties} (bundle base
 * name {@code oogasalad.model.engine.EngineError}).
 */
public enum EngineError {

  NULL_PARAMETER,

  MASS_MUST_BE_NON_NEGATIVE,
  MOMENT_OF_INERTIA_MUST_BE_NON_NEGATIVE,
  FRICTION_MUST_BE_NON_NEGATIVE,
  RESTITUTION_OUT_OF_RANGE,
  THRUST_FORCE_MUST_BE_NON_NEGATIVE,
  MAX_HP_MUST_BE_NON_NEGATIVE,
  CURRENT_HP_OUT_OF_RANGE,
  DAMAGE_MUST_BE_NON_NEGATIVE,
  REPAIR_MUST_BE_NON_NEGATIVE,

  WORLD_ONLY_SUPPORTS_GAME_COMPONENTS,

  JOINT_UNSUPPORTED_PASSTHROUGH,
  JOINT_UNSUPPORTED_STATIC_STATIC,
  JOINT_UNSUPPORTED_PAIR,
  JOINT_DISTANCE_PARAMS_NON_NEGATIVE,
  JOINT_DISTANCE_RANGE_INVALID,
  JOINT_ANGLE_RANGE_INVALID,
  JOINT_MOTOR_PARAMS_NON_NEGATIVE,

  SPRING_JOINT_REQUIRES_POSITIVE_STIFFNESS,
  ROPE_JOINT_MAX_DISTANCE_TOO_SMALL,
  REVOLUTE_JOINT_REQUIRES_SHARED_ANCHOR,

  RIGID_TRIANGLE_LIST_SIZE;

  private static final ResourceBundle BUNDLE =
      ResourceBundle.getBundle("oogasalad.model.engine.EngineError");

  /**
   * Non-parameterized message from the bundle for this constant.
   */
  public String message() {
    try {
      return BUNDLE.getString(name());
    } catch (MissingResourceException ex) {
      return name();
    }
  }

  /**
   * Message from the bundle with {@link MessageFormat} applied (use for keys containing
   * {@code {0}}, etc.).
   */
  public String format(Object... args) {
    return MessageFormat.format(message(), args);
  }
}
