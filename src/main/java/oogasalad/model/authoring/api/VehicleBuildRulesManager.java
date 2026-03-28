package oogasalad.authoring.api;

import oogasalad.model.engine.api.World;

/** * Interface for defining the constraints a level designer can place on a player.
 */
public interface VehicleBuildRulesManager {

  /** * Sets the maximum number of parts allowed for a vehicle in this level.
   * @param count the upper limit of GameObjects in a vehicle assembly.
   */
  void setMaxParts(int count);

  /** @return the maximum number of parts allowed. */
  int getMaxParts();

  /** * Evaluates the given world to see if it violates the designer's restrictions.
   * @param world the current simulation state to validate.
   * @return true if the build adheres to all active rules.
   */
  boolean validateBuild(World world);
}