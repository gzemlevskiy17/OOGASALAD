package oogasalad.model.help.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests custom help-chat exception constructors.
 *
 * @author Emilio Garcia
 */
class HelpChatExceptionTest {

  @Test
  void HelpChatConfigurationException_MessageProvided_StoresMessage() {
    HelpChatConfigurationException exception =
        new HelpChatConfigurationException("missing api key");

    assertEquals("missing api key", exception.getMessage());
  }

  @Test
  void HelpChatConfigurationException_CauseProvided_StoresCause() {
    RuntimeException rootCause = new RuntimeException("root");

    HelpChatConfigurationException exception =
        new HelpChatConfigurationException("missing api key", rootCause);

    assertSame(rootCause, exception.getCause());
  }

  @Test
  void HelpChatRequestException_MessageProvided_StoresMessage() {
    HelpChatRequestException exception = new HelpChatRequestException("request failed");

    assertEquals("request failed", exception.getMessage());
  }

  @Test
  void HelpChatRequestException_CauseProvided_StoresCause() {
    RuntimeException rootCause = new RuntimeException("root");

    HelpChatRequestException exception =
        new HelpChatRequestException("request failed", rootCause);

    assertSame(rootCause, exception.getCause());
  }
}
