package oogasalad.model.auth.api;

/**
 * The outcome of an authentication or account operation.
 * <p>
 * Pattern-match on this to handle success and failure cases:
 * <pre>
 *   if (result instanceof AuthResult.Success s) { ... s.profile() ... }
 *   if (result instanceof AuthResult.Failure f) { ... f.reason() ... }
 * </pre>
 *
 * @author Paulina Vvedenskaya
 */
public sealed interface AuthResult permits AuthResult.Success, AuthResult.Failure {

  /**
   * The operation succeeded. Contains the player's profile.
   */
  record Success(PlayerProfile profile) implements AuthResult {

  }

  /**
   * The operation failed. Contains a human-readable explanation.
   */
  record Failure(String reason) implements AuthResult {}
}
