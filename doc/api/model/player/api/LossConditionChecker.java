package oogasalad.model.player.api;

import oogasalad.model.engine.api.World;

/** Evaluates the World state to determine if the player has failed. */
public interface LossConditionChecker {
  /** @return true if any failure conditions are met. */
  boolean hasLost(World world);
}
