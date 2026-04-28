package oogasalad.model.help.api;

import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Immutable chat message exchanged between the user and the help assistant.
 *
 * @param role speaker role associated with the message
 * @param content non-blank message content
 * @author Emilio Garcia
 */
public record ChatMessage(ChatMessage.Role role, String content) {

  private static final Logger LOG = LogManager.getLogger(ChatMessage.class);
  private static final ResourceBundle MESSAGES =
      ResourceBundle.getBundle("oogasalad.model.help.ChatMessage");

  /**
   * Supported chat speaker roles for the help assistant conversation.
   */
  public enum Role {
    USER,
    ASSISTANT,
    DEVELOPER
  }

  /**
   * Creates a validated chat message.
   *
   * @throws IllegalArgumentException when the role is null or content is blank
   */
  public ChatMessage {
    if (role == null) {
      String message = MESSAGES.getString("nullRoleError");
      LOG.error(message);
      throw new IllegalArgumentException(message);
    }
    if (content == null || content.isBlank()) {
      String message = MESSAGES.getString("blankContentError");
      LOG.warn("{} role={}", message, role);
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Creates a user-authored chat message.
   *
   * @param content message text
   * @return user chat message
   */
  public static ChatMessage userMessage(String content) {
    return new ChatMessage(Role.USER, content);
  }

  /**
   * Creates an assistant-authored chat message.
   *
   * @param content message text
   * @return assistant chat message
   */
  public static ChatMessage assistantMessage(String content) {
    return new ChatMessage(Role.ASSISTANT, content);
  }

  /**
   * Creates a developer/system instruction message for prompt construction.
   *
   * @param content instruction text
   * @return developer chat message
   */
  public static ChatMessage developerMessage(String content) {
    return new ChatMessage(Role.DEVELOPER, content);
  }
}
