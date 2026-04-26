package oogasalad.data.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable model for level-wide settings that get serialized into the level JSON.
 *
 * <p>Fields map directly to the new level schema sections:
 * <ul>
 *   <li>{@code gameMetadata}: levelName, gameType, screenWidth, screenHeight, gravity, drag</li>
 *   <li>{@code mainCharacter}: gridSize, elementsAllowed, maxSelectable, maxHP</li>
 * </ul>
 *
 * @author Paulina Vvedenskaya
 */
public class LevelAttributes {

  // gameMetadata
  private String levelName = "Untitled Level";
  private String gameType = "";
  private double screenWidth = 3200;
  private double screenHeight = 1600;
  private double gravity = -9.8;
  private double drag = 0.01;

  // mainCharacter
  private double gridSize = 60;
  private List<String> elementsAllowed = new ArrayList<>();
  private int maxSelectable = 5;
  private double maxHP = 100;

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
    this.elementsAllowed =
        elementsAllowed == null ? new ArrayList<>() : new ArrayList<>(elementsAllowed);
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

  // ── Legacy aliases kept so existing callers compile without changes ──

  /**
   * @deprecated use {@link #getElementsAllowed()}
   */
  @Deprecated
  public List<String> getAllowedVehicleComponents() {
    return getElementsAllowed();
  }

  /**
   * @deprecated use {@link #setElementsAllowed(List)}
   */
  @Deprecated
  public void setAllowedVehicleComponents(List<String> components) {
    setElementsAllowed(components);
  }

  /**
   * @deprecated use {@link #getMaxSelectable()}
   */
  @Deprecated
  public int getMaxSelectableVehicleItems() {
    return getMaxSelectable();
  }

  /**
   * @deprecated use {@link #setMaxSelectable(int)}
   */
  @Deprecated
  public void setMaxSelectableVehicleItems(int max) {
    setMaxSelectable(max);
  }
}
