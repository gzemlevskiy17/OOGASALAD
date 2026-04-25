package oogasalad.data.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable model for level-wide settings editable in the authoring environment.
 *
 * Distinct from GameConfig (read-only, game-player-facing) — this is the
 * editor's in-memory representation of level metadata that gets serialized
 * into the level JSON alongside terrain and objects.
 *
 * @author Emilio Garcia
 */
public class LevelAttributes {

  private String levelName = "Untitled Level";
  private double gravity = -9.8;
  private String backgroundImage = "";
  private int maxSelectableVehicleItems = 5;
  private List<String> allowedVehicleComponents = new ArrayList<>();

  /**
   * Returns the current level name.
   *
   * @return editable level name
   * @author Emilio Garcia
   */
  public String getLevelName() { return levelName; }

  /**
   * Updates the current level name.
   *
   * @param levelName new level name
   * @author Emilio Garcia
   */
  public void setLevelName(String levelName) { this.levelName = levelName; }

  /**
   * Returns the configured gravity value.
   *
   * @return level gravity
   * @author Emilio Garcia
   */
  public double getGravity() { return gravity; }

  /**
   * Updates the configured gravity value.
   *
   * @param gravity new gravity
   * @author Emilio Garcia
   */
  public void setGravity(double gravity) { this.gravity = gravity; }

  /**
   * Returns the background image path.
   *
   * @return background image path
   * @author Emilio Garcia
   */
  public String getBackgroundImage() { return backgroundImage; }

  /**
   * Updates the background image path.
   *
   * @param backgroundImage new background image path
   * @author Emilio Garcia
   */
  public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }

  /**
   * Returns the maximum number of vehicle items that may be selected for this level.
   *
   * @return selection cap for vehicle items
   * @author Emilio Garcia
   */
  public int getMaxSelectableVehicleItems() {
    return maxSelectableVehicleItems;
  }

  /**
   * Updates the maximum number of vehicle items that may be selected for this level.
   *
   * @param maxSelectableVehicleItems new vehicle-item selection cap
   * @author Emilio Garcia
   */
  public void setMaxSelectableVehicleItems(int maxSelectableVehicleItems) {
    this.maxSelectableVehicleItems = maxSelectableVehicleItems;
  }

  /**
   * Returns the stable IDs of vehicle components available in this level.
   *
   * @return copy of allowed vehicle component IDs
   * @author Emilio Garcia
   */
  public List<String> getAllowedVehicleComponents() {
    return List.copyOf(allowedVehicleComponents);
  }

  /**
   * Replaces the allowed vehicle component IDs for this level.
   *
   * @param allowedVehicleComponents new stable component IDs to persist
   * @author Emilio Garcia
   */
  public void setAllowedVehicleComponents(List<String> allowedVehicleComponents) {
    this.allowedVehicleComponents = allowedVehicleComponents == null
        ? new ArrayList<>()
        : new ArrayList<>(allowedVehicleComponents);
  }
}
