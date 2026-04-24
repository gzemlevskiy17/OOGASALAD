package oogasalad.model.help.api;

/**
 * Thrown when the help chatbot runtime configuration is invalid or incomplete.
 *
 * @author Emilio Garcia
 */
public class HelpChatConfigurationException extends RuntimeException {

  /**
   * Creates a configuration exception with the given message.
   *
   * @param message configuration failure description
   * @author Emilio Garcia
   */
  public HelpChatConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates a configuration exception with the given message and underlying cause.
   *
   * @param message configuration failure description
   * @param cause root cause
   * @author Emilio Garcia
   */
  public HelpChatConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
