package oogasalad.model.player.api;

/**
 * Result of a {@link PlacementValidator} check. When {@link #valid()} is false, {@link #reason()}
 * carries a short human-readable explanation the builder UI can surface as a tooltip.
 *
 * @author Paulina Vvedenskaya
 */
public record ValidationResult(boolean valid, String reason) {

  public static ValidationResult ok() {
    return new ValidationResult(true, null);
  }

  public static ValidationResult fail(String reason) {
    return new ValidationResult(false, reason);
  }
}
