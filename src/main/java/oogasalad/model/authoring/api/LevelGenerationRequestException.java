package oogasalad.model.authoring.api;

/**
 * Raised when an AI level generation request fails after initialization.
 *
 * @author Emilio Garcia
 */
public class LevelGenerationRequestException extends RuntimeException {

  /**
   * Creates a request exception with the given message.
   *
   * @param message request failure description
   * @author Emilio Garcia
   */
  public LevelGenerationRequestException(String message) {
    super(message);
  }

  /**
   * Creates a request exception with the given message and root cause.
   *
   * @param message request failure description
   * @param cause underlying cause
   * @author Emilio Garcia
   */
  public LevelGenerationRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
