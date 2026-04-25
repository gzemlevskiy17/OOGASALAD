package oogasalad.model.authoring.api;

/**
 * Raised when a generated level JSON file cannot be persisted to disk.
 *
 * @author Emilio Garcia
 */
public class GeneratedLevelFileException extends RuntimeException {

  /**
   * Creates a file persistence exception with the given message.
   *
   * @param message file persistence failure description
   * @author Emilio Garcia
   */
  public GeneratedLevelFileException(String message) {
    super(message);
  }

  /**
   * Creates a file persistence exception with the given message and root cause.
   *
   * @param message file persistence failure description
   * @param cause underlying cause
   * @author Emilio Garcia
   */
  public GeneratedLevelFileException(String message, Throwable cause) {
    super(message, cause);
  }
}
