package oogasalad.model.authoring.api;

/**
 * Signals that the fixed Good Piggies catalog could not be loaded or did not match the expected
 * schema-era constraints for AI level generation.
 *
 * @author Emilio Garcia
 */
public class GoodPiggiesCatalogException extends RuntimeException {

  /**
   * Creates a catalog exception with a message.
   *
   * @param message failure description
   * @author Emilio Garcia
   */
  public GoodPiggiesCatalogException(String message) {
    super(message);
  }

  /**
   * Creates a catalog exception with a message and cause.
   *
   * @param message failure description
   * @param cause underlying cause
   * @author Emilio Garcia
   */
  public GoodPiggiesCatalogException(String message, Throwable cause) {
    super(message, cause);
  }
}
