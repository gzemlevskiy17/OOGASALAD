package oogasalad.model.player.api;

import oogasalad.model.engine.api.World;

/** Evaluates the World state to determine if the player has achieved victory. */
public interface WinConditionChecker {
  /** @return true if the victory conditions are met. */
  boolean hasWon(World world);
}