package oogasalad.view.core.api;

/** Provides read-only graphical data for rendering. */
public interface Renderable {
  double getX();
  double getY();
  double getRotation();
  /** @return the identifier or path for the visual asset. */
  String getSprite();
}