package oogasalad.controller.authoring.api;

/** Thrown when an authoring placement request is invalid and cannot be applied. */
public class AuthoringPlacementException extends RuntimeException {

  public AuthoringPlacementException(String message) {
    super(message);
  }

  public AuthoringPlacementException(String message, Throwable cause) {
    super(message, cause);
  }
}
