package oogasalad.model.help.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests immutable help-chat message construction.
 *
 * @author Emilio Garcia
 */
class ChatMessageTest {

  @Test
  void UserMessage_NonBlankContent_ReturnsUserRoleMessage() {
    ChatMessage message = ChatMessage.userMessage("How do I build?");

    assertEquals(ChatMessage.Role.USER, message.role());
    assertEquals("How do I build?", message.content());
  }

  @Test
  void AssistantMessage_NonBlankContent_ReturnsAssistantRoleMessage() {
    ChatMessage message = ChatMessage.assistantMessage("Place a frame first.");

    assertEquals(ChatMessage.Role.ASSISTANT, message.role());
    assertEquals("Place a frame first.", message.content());
  }

  @Test
  void Constructor_BlankContent_ThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class,
        () -> new ChatMessage(ChatMessage.Role.USER, " "));
  }

  @Test
  void Constructor_NullRole_ThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> new ChatMessage(null, "Help"));
  }
}
