package oogasalad.data.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable in-memory representation of a level's metadata + main-character settings, mirroring
 * the {@code gameMetadata} and {@code mainCharacter} sections of the level.json schema. Map
 * placements (knots, spawn point, objects) live in the {@code World} at runtime, not here.
 *
 * <p>Distinct from {@link Settings} (key/value bag for player input bindings) and from
 * {@link GameConfig} (the immutable per-game catalog).
 *
 * @author Paulina Vvedenskaya
 */
public class LevelAttributes {

  private final GameMetadata gameMetadata = new GameMetadata();
  private final MainCharacter mainCharacter = new MainCharacter();

  public GameMetadata getGameMetadata() {
    return gameMetadata;
  }

  public MainCharacter getMainCharacter() {
    return mainCharacter;
  }

  /**
   * {@code gameMetadata} section: per-level metadata + physics constants.
   */
  public static class GameMetadata {

    private String levelName = "Untitled Level";
    private String gameType = "";
    private double screenWidth = 1920;
    private double screenHeight = 1080;
    private double gravity = -9.8;
    private double drag = 0.02;

    public String getLevelName() {
      return levelName;
    }

    public void setLevelName(String levelName) {
      this.levelName = levelName;
    }

    public String getGameType() {
      return gameType;
    }

    public void setGameType(String gameType) {
      this.gameType = gameType;
    }

    public double getScreenWidth() {
      return screenWidth;
    }

    public void setScreenWidth(double screenWidth) {
      this.screenWidth = screenWidth;
    }

    public double getScreenHeight() {
      return screenHeight;
    }

    public void setScreenHeight(double screenHeight) {
      this.screenHeight = screenHeight;
    }

    public double getGravity() {
      return gravity;
    }

    public void setGravity(double gravity) {
      this.gravity = gravity;
    }

    public double getDrag() {
      return drag;
    }

    public void setDrag(double drag) {
      this.drag = drag;
    }
  }

  /**
   * {@code mainCharacter} section: vehicle-builder allowlist and chassis HP.
   */
  public static class MainCharacter {

    private double gridSize = 60;
    private List<String> elementsAllowed = new ArrayList<>();
    private int maxSelectable = 5;
    private double maxHP = 100;

    public double getGridSize() {
      return gridSize;
    }

    public void setGridSize(double gridSize) {
      this.gridSize = gridSize;
    }

    public List<String> getElementsAllowed() {
      return List.copyOf(elementsAllowed);
    }

    public void setElementsAllowed(List<String> elementsAllowed) {
      this.elementsAllowed = elementsAllowed == null
          ? new ArrayList<>()
          : new ArrayList<>(elementsAllowed);
    }

    public int getMaxSelectable() {
      return maxSelectable;
    }

    public void setMaxSelectable(int maxSelectable) {
      this.maxSelectable = maxSelectable;
    }

    public double getMaxHP() {
      return maxHP;
    }

    public void setMaxHP(double maxHP) {
      this.maxHP = maxHP;
    }
  }
}
