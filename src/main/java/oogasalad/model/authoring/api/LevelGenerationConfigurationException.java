package oogasalad.model.authoring.api;

/**
 * Raised when AI level generation is configured incorrectly and cannot initialize its LLM client
 * or prompt resources.
 *
 * @author Emilio Garcia
 */
public class LevelGenerationConfigurationException extends RuntimeException {

  /**
   * Creates a configuration exception with the given message.
   *
   * @param message configuration failure description
   * @author Emilio Garcia
   */
  public LevelGenerationConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates a configuration exception with the given message and root cause.
   *
   * @param message configuration failure description
   * @param cause underlying cause
   * @author Emilio Garcia
   */
  public LevelGenerationConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
