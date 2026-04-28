package oogasalad.view.authoring.internal.level;

/**
 * Thrown when the user attempts to select more vehicle components than the palette allows.
 * @author Emilio Garcia
 */
public class VehicleSelectionLimitException extends RuntimeException {

  /**
   * Creates a new exception with the given message.
   *
   * @param message description of the selection-limit violation
   */
  public VehicleSelectionLimitException(String message) {
    super(message);
  }
}
