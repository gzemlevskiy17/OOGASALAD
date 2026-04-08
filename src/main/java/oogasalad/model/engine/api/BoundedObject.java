package oogasalad.model.engine.api;

/**
 * Represents a backend object with rectangular bounds in world space.
 *
 * <p>This contract supports authoring-time placement validation without forcing
 * every engine object to encode editor policy. Implementations provide width
 * and height while inheriting position from {@link GameObject}.
 *
 * @author Emilio Garcia
 */
public interface BoundedObject extends GameObject {

  double getWidth();

  double getHeight();

  default double left() {
    return getX();
  }

  default double top() {
    return getY();
  }

  default double right() {
    return left() + getWidth();
  }

  default double bottom() {
    return top() + getHeight();
  }
}
