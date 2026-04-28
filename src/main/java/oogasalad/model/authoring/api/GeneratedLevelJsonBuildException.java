package oogasalad.model.authoring.api;

/**
 * Raised when a normalized generated-level specification cannot be converted into the repository's
 * runtime level JSON shape.
 * @author Emilio Garcia
 */
public class GeneratedLevelJsonBuildException extends RuntimeException {

  /**
   * Creates a JSON build exception with the given message.
   *
   * @param message build failure description
   */
  public GeneratedLevelJsonBuildException(String message) {
    super(message);
  }

  /**
   * Creates a JSON build exception with the given message and root cause.
   *
   * @param message build failure description
   * @param cause underlying cause
   */
  public GeneratedLevelJsonBuildException(String message, Throwable cause) {
    super(message, cause);
  }
}
