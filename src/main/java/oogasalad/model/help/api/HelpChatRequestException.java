package oogasalad.model.help.api;

/**
 * Thrown when the help chatbot cannot complete a request to the LLM provider.
 * @author Emilio Garcia
 */
public class HelpChatRequestException extends RuntimeException {

  /**
   * Creates a request exception with the given message.
   *
   * @param message request failure description
   */
  public HelpChatRequestException(String message) {
    super(message);
  }

  /**
   * Creates a request exception with the given message and underlying cause.
   *
   * @param message request failure description
   * @param cause root cause
   */
  public HelpChatRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
