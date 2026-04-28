package oogasalad.controller.player.internal.cheats;

/**
 * Signals an invalid author-defined cheat configuration that cannot be bound safely at runtime.
 *
 * @author Emilio Garcia
 */
public class CheatConfigurationException extends RuntimeException {

  public CheatConfigurationException(String message) {
    super(message);
  }

  public CheatConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
