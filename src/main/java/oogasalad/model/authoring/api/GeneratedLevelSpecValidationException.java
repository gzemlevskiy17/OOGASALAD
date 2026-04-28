package oogasalad.model.authoring.api;

/**
 * Raised when an AI-generated level specification violates the accepted authoring-generation
 * contract and cannot safely proceed to normalization or JSON conversion.
 * @author Emilio Garcia
 */
public class GeneratedLevelSpecValidationException extends RuntimeException {

  /**
   * Creates a validation exception with the given message.
   *
   * @param message validation failure description
   */
  public GeneratedLevelSpecValidationException(String message) {
    super(message);
  }

  /**
   * Creates a validation exception with the given message and root cause.
   *
   * @param message validation failure description
   * @param cause underlying cause
   */
  public GeneratedLevelSpecValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
