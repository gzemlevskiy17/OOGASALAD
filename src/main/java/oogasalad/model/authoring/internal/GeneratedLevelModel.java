package oogasalad.model.authoring.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelResult;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.LevelGenerationConfigurationException;
import oogasalad.model.authoring.api.LevelGenerationRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Orchestrates the full AI level generation pipeline from free-form prompt to saved level file.
 *
 * <p>This model coordinates LLM generation, JSON parsing, validation, normalization, runtime JSON
 * conversion, and persistence so the UI can trigger one high-level operation and receive a single
 * generated result.
 *
 * @author Emilio Garcia
 */
public class GeneratedLevelModel {

  private static final Logger LOG = LogManager.getLogger(GeneratedLevelModel.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelModel");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final LevelGenerationService generationService;
  private final GeneratedLevelSpecValidator validator;
  private final GeneratedLevelSpecNormalizer normalizer;
  private final GeneratedLevelJsonBuilder jsonBuilder;
  private final GeneratedLevelFileService fileService;

  /**
   * Creates a generated-level model with the default authoring generation collaborators.
   *
   * @author Emilio Garcia
   */
  public GeneratedLevelModel() {
    this(
        new LevelGenerationService(),
        new GeneratedLevelSpecValidator(),
        new GeneratedLevelSpecNormalizer(),
        new GeneratedLevelJsonBuilder(),
        new GeneratedLevelFileService());
  }

  /**
   * Creates a generated-level model with injected collaborators.
   *
   * @param generationService LLM-backed generation service
   * @param validator generated-spec validator
   * @param normalizer generated-spec normalizer
   * @param jsonBuilder runtime JSON builder
   * @param fileService generated-level file persistence service
   * @author Emilio Garcia
   */
  public GeneratedLevelModel(LevelGenerationService generationService,
      GeneratedLevelSpecValidator validator,
      GeneratedLevelSpecNormalizer normalizer,
      GeneratedLevelJsonBuilder jsonBuilder,
      GeneratedLevelFileService fileService) {
    this.generationService = requireCollaborator(generationService, "nullGenerationServiceError");
    this.validator = requireCollaborator(validator, "nullValidatorError");
    this.normalizer = requireCollaborator(normalizer, "nullNormalizerError");
    this.jsonBuilder = requireCollaborator(jsonBuilder, "nullJsonBuilderError");
    this.fileService = requireCollaborator(fileService, "nullFileServiceError");
  }

  /**
   * Generates a new level from the provided free-form prompt and saves the resulting schema-native
   * level JSON file to disk.
   *
   * @param prompt free-form user request describing the desired level
   * @return generated level result with saved file path and normalized gravity
   * @author Emilio Garcia
   */
  public GeneratedLevelResult generateLevel(String prompt) {
    String rawSpecJson = generationService.generateLevelSpecJson(prompt);
    GeneratedLevelSpec parsedSpec = parseSpec(rawSpecJson);
    validator.validate(parsedSpec);
    GeneratedLevelSpec normalizedSpec = normalizer.normalize(parsedSpec);
    ObjectNode levelJson = jsonBuilder.build(normalizedSpec);
    String filePath = fileService.save(levelJson, normalizedSpec.gameMetadata().levelName());
    LOG.info(PROPS.getString("generationSuccessLogMessage"), filePath);
    return new GeneratedLevelResult(filePath, normalizedSpec.gameMetadata().gravity());
  }

  private GeneratedLevelSpec parseSpec(String rawSpecJson) {
    try {
      GeneratedLevelSpec spec = MAPPER.readValue(rawSpecJson, GeneratedLevelSpec.class);
      if (spec == null) {
        String message = PROPS.getString("specParseFailureError");
        LOG.error(message);
        throw new LevelGenerationRequestException(message);
      }
      return spec;
    } catch (IOException exception) {
      String message = PROPS.getString("specParseFailureError");
      LOG.error(message, exception);
      throw new LevelGenerationRequestException(message, exception);
    }
  }

  private <T> T requireCollaborator(T collaborator, String messageKey) {
    if (collaborator == null) {
      String message = PROPS.getString(messageKey);
      LOG.error(message);
      throw new LevelGenerationConfigurationException(message);
    }
    return collaborator;
  }
}
