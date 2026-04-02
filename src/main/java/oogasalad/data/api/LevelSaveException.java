package oogasalad.data.api;

/** Thrown when a level file cannot be serialized or written to disk. */
public class LevelSaveException extends RuntimeException {

  public LevelSaveException(String message, Throwable cause) {
    super(message, cause);
  }

  public LevelSaveException(String message) {
    super(message);
  }
}
