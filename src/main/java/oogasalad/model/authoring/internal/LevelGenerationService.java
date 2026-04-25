package oogasalad.model.authoring.internal;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.LevelGenerationConfigurationException;
import oogasalad.model.authoring.api.LevelGenerationRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles communication with the LLM provider used to generate intermediate AI level
 * specifications from free-form user prompts.
 *
 * @author Emilio Garcia
 */
public class LevelGenerationService {

  private static final Logger LOG = LogManager.getLogger(LevelGenerationService.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.LevelGenerationService");
  private static final Map<String, String> DOT_ENV = loadDotEnv();

  private final OpenAIClient client;
  private final String modelName;
  private final String systemPrompt;

  /**
   * Creates the level generation service using runtime configuration and the packaged generation
   * prompt resource.
   *
   * @author Emilio Garcia
   */
  public LevelGenerationService() {
    String apiKey = getEnv(PROPS.getString("apiKeyEnvVarName"));
    String baseUrl = getEnv(PROPS.getString("baseUrlEnvVarName"));
    validateRuntimeConfiguration(apiKey, baseUrl);
    this.modelName = PROPS.getString("modelName");
    this.systemPrompt = loadSystemPrompt();
    try {
      client = OpenAIOkHttpClient.builder()
          .apiKey(apiKey)
          .baseUrl(baseUrl)
          .build();
    } catch (RuntimeException exception) {
      String message = PROPS.getString("clientInitializationFailureError");
      LOG.error(message, exception);
      throw new LevelGenerationConfigurationException(message, exception);
    }
  }

  /**
   * Generates a raw JSON level specification string from the user's free-form level prompt.
   *
   * @param userPrompt free-form authored level request
   * @return raw JSON string representing the generated intermediate level spec
   * @author Emilio Garcia
   */
  public String generateLevelSpecJson(String userPrompt) {
    validatePrompt(userPrompt);
    try {
      ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
          .model(modelName)
          .addDeveloperMessage(systemPrompt)
          .addUserMessage(userPrompt.trim())
          .build();

      StringBuilder response = new StringBuilder();
      client.chat()
          .completions()
          .create(params)
          .choices()
          .stream()
          .flatMap(choice -> choice.message().content().stream())
          .forEach(response::append);

      String result = response.toString().trim();
      if (result.isBlank()) {
        String message = PROPS.getString("blankResponseError");
        LOG.error(message);
        throw new LevelGenerationRequestException(message);
      }
      return result;
    } catch (LevelGenerationRequestException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      String message = PROPS.getString("requestFailureError");
      LOG.error(message, exception);
      throw new LevelGenerationRequestException(message, exception);
    }
  }

  private void validateRuntimeConfiguration(String apiKey, String baseUrl) {
    if (apiKey == null || apiKey.isBlank()) {
      String message = PROPS.getString("missingApiKeyError");
      LOG.error("{} envVar={}", message, PROPS.getString("apiKeyEnvVarName"));
      throw new LevelGenerationConfigurationException(message);
    }
    if (baseUrl == null || baseUrl.isBlank()) {
      String message = PROPS.getString("missingBaseUrlError");
      LOG.error("{} envVar={}", message, PROPS.getString("baseUrlEnvVarName"));
      throw new LevelGenerationConfigurationException(message);
    }
  }

  private void validatePrompt(String userPrompt) {
    if (userPrompt == null || userPrompt.isBlank()) {
      String message = PROPS.getString("blankUserPromptError");
      LOG.warn(message);
      throw new LevelGenerationRequestException(message);
    }
    if (systemPrompt.isBlank()) {
      String message = PROPS.getString("blankSystemPromptError");
      LOG.error(message);
      throw new LevelGenerationConfigurationException(message);
    }
  }

  private String loadSystemPrompt() {
    String resourcePath = PROPS.getString("systemPromptResource");
    try (InputStream inputStream = LevelGenerationService.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        String message = PROPS.getString("promptLoadFailureError");
        LOG.error("{} resource={}", message, resourcePath);
        throw new LevelGenerationConfigurationException(message);
      }
      String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
      if (prompt.isBlank()) {
        String message = PROPS.getString("blankSystemPromptError");
        LOG.error("{} resource={}", message, resourcePath);
        throw new LevelGenerationConfigurationException(message);
      }
      return prompt;
    } catch (IOException exception) {
      String message = PROPS.getString("promptLoadFailureError");
      LOG.error("{} resource={}", message, resourcePath, exception);
      throw new LevelGenerationConfigurationException(message, exception);
    }
  }

  /**
   * Reads from OS env first, then falls back to the local .env file.
   */
  private static String getEnv(String key) {
    String value = System.getenv(key);
    return value != null ? value : DOT_ENV.get(key);
  }

  private static Map<String, String> loadDotEnv() {
    Map<String, String> map = new HashMap<>();
    File file = new File(".env");
    if (!file.exists()) {
      return map;
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.strip();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        int separator = line.indexOf('=');
        if (separator > 0) {
          map.put(line.substring(0, separator).strip(), line.substring(separator + 1).strip());
        }
      }
    } catch (IOException exception) {
      LOG.warn(PROPS.getString("dotEnvReadWarning"), exception.getMessage());
    }
    return map;
  }
}
