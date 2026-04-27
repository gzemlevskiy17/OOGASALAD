package oogasalad.model.authoring.api;

/**
 * Result of a successful AI level generation pipeline run.
 *
 * @param filePath absolute path to the saved generated level JSON file
 * @param gravity normalized gravity value associated with the generated level
 * @author Emilio Garcia
 */
public record GeneratedLevelResult(String filePath, double gravity) {
}
