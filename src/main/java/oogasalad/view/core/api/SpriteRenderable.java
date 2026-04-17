package oogasalad.view.core.api;

public interface SpriteRenderable extends Renderable {

  String getSpritePath();

  double getSpriteWidth();

  double getSpriteHeight();

  default String getHubSpritePath() {
    return null;
  }

  default String getWheelSpritePath() {
    return null;
  }
}
