package oogasalad.model.help.internal;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.help.api.ChatMessage;
import oogasalad.model.help.api.HelpChatConfigurationException;
import oogasalad.model.help.api.HelpChatRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles communication with the LLM provider used by the in-game help chatbot.
 *
 * @author Emilio Garcia
 */
public class HelpLLMService {

  private static final Logger LOG = LogManager.getLogger(HelpLLMService.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.help.HelpChat");

  private final OpenAIClient client;
  private final String modelName;

  /**
   * Creates the help chatbot LLM service using runtime environment configuration.
   *
   * @author Emilio Garcia
   */
  public HelpLLMService() {
    validateRuntimeConfiguration();
    modelName = PROPS.getString("modelName");
    try {
      client = OpenAIOkHttpClient.fromEnv();
    } catch (RuntimeException exception) {
      String message = PROPS.getString("llmRequestFailureError");
      LOG.error("Failed to initialize help chat LLM client", exception);
      throw new HelpChatConfigurationException(message, exception);
    }
  }

  /**
   * Sends the current help chat conversation to the LLM and returns the assistant response.
   *
   * @param systemPrompt non-blank prompt that describes Good Piggies help behavior
   * @param conversation prior user and assistant messages to include in the request
   * @return assistant response text
   * @author Emilio Garcia
   */
  public String getLLMResponse(String systemPrompt, List<ChatMessage> conversation) {
    validateRequest(systemPrompt, conversation);
    try {
      ChatCompletionCreateParams.Builder createParams = ChatCompletionCreateParams.builder()
          .model(modelName)
          .addDeveloperMessage(systemPrompt);
      appendConversation(createParams, conversation);

      StringBuilder response = new StringBuilder();
      client.chat()
          .completions()
          .create(createParams.build())
          .choices()
          .stream()
          .flatMap(choice -> choice.message().content().stream())
          .forEach(response::append);

      String result = response.toString().trim();
      if (result.isBlank()) {
        String message = PROPS.getString("llmRequestFailureError");
        LOG.error("Received blank response from help chatbot model {}", modelName);
        throw new HelpChatRequestException(message);
      }

      return result;
    } catch (HelpChatRequestException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      String message = PROPS.getString("llmRequestFailureError");
      LOG.error("LLM request failed for help chatbot model {}", modelName, exception);
      throw new HelpChatRequestException(message, exception);
    }
  }

  private void validateRuntimeConfiguration() {
    String apiKeyEnvVar = PROPS.getString("apiKeyEnvVarName");
    String baseUrlEnvVar = PROPS.getString("baseUrlEnvVarName");

    String apiKey = System.getenv(apiKeyEnvVar);
    if (apiKey == null || apiKey.isBlank()) {
      String message = PROPS.getString("missingApiKeyError");
      LOG.error("{} envVar={}", message, apiKeyEnvVar);
      throw new HelpChatConfigurationException(message);
    }

    String baseUrl = System.getenv(baseUrlEnvVar);
    if (baseUrl == null || baseUrl.isBlank()) {
      String message = PROPS.getString("missingBaseUrlError");
      LOG.error("{} envVar={}", message, baseUrlEnvVar);
      throw new HelpChatConfigurationException(message);
    }
  }

  private void validateRequest(String systemPrompt, List<ChatMessage> conversation) {
    if (systemPrompt == null || systemPrompt.isBlank()) {
      String message = PROPS.getString("blankSystemPromptError");
      LOG.error(message);
      throw new HelpChatConfigurationException(message);
    }
    if (conversation == null || conversation.isEmpty()) {
      String message = PROPS.getString("emptyConversationError");
      LOG.warn(message);
      throw new HelpChatRequestException(message);
    }
  }

  private void appendConversation(ChatCompletionCreateParams.Builder createParams,
      List<ChatMessage> conversation) {
    for (ChatMessage message : conversation) {
      switch (message.role()) {
        case USER -> createParams.addUserMessage(message.content());
        case ASSISTANT -> createParams.addAssistantMessage(message.content());
        case DEVELOPER -> createParams.addDeveloperMessage(message.content());
      }
    }
  }
}
