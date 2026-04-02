package oogasalad.data.api;

/** Thrown when a level file cannot be read or parsed. */
public class LevelLoadException extends RuntimeException {

  public LevelLoadException(String message, Throwable cause) {
    super(message, cause);
  }

  public LevelLoadException(String message) {
    super(message);
  }
}
