package oogasalad.data.api;

/**
 * Mutable model for level-wide settings editable in the authoring environment.
 *
 * Distinct from GameConfig (read-only, game-player-facing) — this is the
 * editor's in-memory representation of level metadata that gets serialized
 * into the level JSON alongside terrain and objects.
 */
public class LevelAttributes {

  private String levelName = "Untitled Level";
  private double gravity = -9.8;
  private String backgroundImage = "";

  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }

  public double getGravity() { return gravity; }
  public void setGravity(double gravity) { this.gravity = gravity; }

  public String getBackgroundImage() { return backgroundImage; }
  public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }
}
