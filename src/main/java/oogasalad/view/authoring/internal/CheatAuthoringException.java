package oogasalad.view.authoring.internal;

/**
 * Signals invalid cheat data entered in the game authoring screen.
 *
 * @author Emilio Garcia
 */
public class CheatAuthoringException extends RuntimeException {

  public CheatAuthoringException(String message) {
    super(message);
  }
}
