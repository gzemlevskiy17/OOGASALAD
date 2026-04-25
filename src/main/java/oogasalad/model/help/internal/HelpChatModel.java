package oogasalad.model.help.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.help.api.ChatMessage;
import oogasalad.model.help.api.HelpChatConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stateful chat model for the in-game help assistant.
 *
 * <p>This class owns the conversation transcript and delegates response generation to the
 * configured LLM service.
 *
 * @author Emilio Garcia
 */
public class HelpChatModel {

  private static final Logger LOG = LogManager.getLogger(HelpChatModel.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.help.HelpChat");

  private final List<ChatMessage> conversation;
  private final HelpLLMService llmService;
  private final String systemPrompt;

  /**
   * Creates a help chat model using the default LLM service and configured prompt resource.
   *
   * @author Emilio Garcia
   */
  public HelpChatModel() {
    this(new HelpLLMService());
  }

  /**
   * Creates a help chat model with an injected LLM service.
   *
   * @param llmService service used to generate assistant responses
   * @author Emilio Garcia
   */
  public HelpChatModel(HelpLLMService llmService) {
    if (llmService == null) {
      String message = PROPS.getString("llmRequestFailureError");
      LOG.error("Help chat model was created with a null LLM service");
      throw new HelpChatConfigurationException(message);
    }
    this.llmService = llmService;
    this.conversation = new ArrayList<>();
    this.systemPrompt = loadSystemPrompt();
  }

  /**
   * Returns an immutable snapshot of the current conversation transcript.
   *
   * @return current chat transcript
   * @author Emilio Garcia
   */
  public List<ChatMessage> getConversation() {
    return List.copyOf(conversation);
  }

  /**
   * Sends a user message to the help assistant and appends the response to the transcript.
   *
   * @param userMessage non-blank user message
   * @return assistant reply
   * @author Emilio Garcia
   */
  public ChatMessage getResponse(String userMessage) {
    ChatMessage request = ChatMessage.userMessage(userMessage);
    conversation.add(request);
    String response = llmService.getLLMResponse(systemPrompt, conversation);
    ChatMessage reply = ChatMessage.assistantMessage(response);
    conversation.add(reply);
    return reply;
  }

  /**
   * Clears the current transcript.
   *
   * @author Emilio Garcia
   */
  public void clearConversation() {
    conversation.clear();
  }

  /**
   * Returns the configured system prompt currently used by the help assistant.
   *
   * @return configured system prompt text
   * @author Emilio Garcia
   */
  public String getSystemPrompt() {
    return systemPrompt;
  }

  private String loadSystemPrompt() {
    String resourcePath = PROPS.getString("systemPromptResource");
    try (InputStream inputStream = HelpChatModel.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        String message = PROPS.getString("promptLoadFailureError");
        LOG.error("{} resource={}", message, resourcePath);
        throw new HelpChatConfigurationException(message);
      }
      String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
      if (prompt.isBlank()) {
        String message = PROPS.getString("blankSystemPromptError");
        LOG.error("{} resource={}", message, resourcePath);
        throw new HelpChatConfigurationException(message);
      }
      return prompt;
    } catch (IOException exception) {
      String message = PROPS.getString("promptLoadFailureError");
      LOG.error("{} resource={}", message, resourcePath, exception);
      throw new HelpChatConfigurationException(message, exception);
    }
  }
}
