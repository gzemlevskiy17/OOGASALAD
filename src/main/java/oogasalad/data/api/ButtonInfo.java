package oogasalad.data.api;

/**
 * On-screen control contributed by a movable component.
 *
 * @param name      label shown on the button, e.g. "go right"
 * @param directionX X component of the movement vector applied while held
 * @param directionY Y component of the movement vector applied while held
 */
public record ButtonInfo(String name, double directionX, double directionY) {}
