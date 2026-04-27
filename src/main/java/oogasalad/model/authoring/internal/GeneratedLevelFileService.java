package oogasalad.model.authoring.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelFileException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Persists generated level JSON files to a dedicated output directory and returns the saved file
 * path for later authoring-screen loading.
 *
 * @author Emilio Garcia
 */
public class GeneratedLevelFileService {

  private static final Logger LOG = LogManager.getLogger(GeneratedLevelFileService.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelFileService");

  private final Path outputDirectory;
  private final DateTimeFormatter timestampFormatter;

  /**
   * Creates a generated-level file service using the configured output directory.
   *
   * @author Emilio Garcia
   */
  public GeneratedLevelFileService() {
    this(Paths.get(PROPS.getString("outputDirectory")));
  }

  /**
   * Creates a generated-level file service using the given output directory.
   *
   * @param outputDirectory directory where generated level files should be written
   * @author Emilio Garcia
   */
  public GeneratedLevelFileService(Path outputDirectory) {
    if (outputDirectory == null) {
      String message = PROPS.getString("nullOutputDirectoryError");
      LOG.error(message);
      throw new GeneratedLevelFileException(message);
    }
    this.outputDirectory = outputDirectory;
    this.timestampFormatter = DateTimeFormatter.ofPattern(PROPS.getString("timestampPattern"));
  }

  /**
   * Writes the provided generated level JSON to disk and returns the saved file path.
   *
   * @param levelJson generated level JSON tree to persist
   * @param levelName requested level name used to derive the output filename
   * @return absolute path to the saved generated level JSON file
   * @author Emilio Garcia
   */
  public String save(ObjectNode levelJson, String levelName) {
    if (levelJson == null) {
      String message = PROPS.getString("nullLevelJsonError");
      LOG.error(message);
      throw new GeneratedLevelFileException(message);
    }

    try {
      Files.createDirectories(outputDirectory);
      Path outputFile = outputDirectory.resolve(buildFileName(levelName));
      MAPPER.writerWithDefaultPrettyPrinter().writeValue(outputFile.toFile(), levelJson);
      Path absolutePath = outputFile.toAbsolutePath();
      LOG.info(PROPS.getString("saveSuccessLogMessage"), absolutePath);
      return absolutePath.toString();
    } catch (IOException exception) {
      String message = PROPS.getString("saveFailureError");
      LOG.error(message, exception);
      throw new GeneratedLevelFileException(message, exception);
    }
  }

  private String buildFileName(String levelName) {
    String baseName = sanitizeFileComponent(levelName);
    if (baseName.isBlank()) {
      baseName = PROPS.getString("defaultFileStem");
    }
    String timestamp = LocalDateTime.now().format(timestampFormatter);
    return baseName + PROPS.getString("fileNameSeparator") + timestamp
        + PROPS.getString("fileExtension");
  }

  private String sanitizeFileComponent(String rawValue) {
    if (rawValue == null) {
      return "";
    }
    return rawValue.trim()
        .toLowerCase()
        .replaceAll(PROPS.getString("invalidFileNamePattern"), PROPS.getString("replacementToken"))
        .replaceAll(PROPS.getString("duplicateReplacementPattern"),
            PROPS.getString("replacementToken"))
        .replaceAll(PROPS.getString("leadingTrailingReplacementPattern"), "");
  }
}
