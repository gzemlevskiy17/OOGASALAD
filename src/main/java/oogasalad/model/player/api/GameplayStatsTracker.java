package oogasalad.model.player.api;

import oogasalad.model.engine.api.World;

/** Per-run accumulator for gameplay stats. Sampled each frame; finalized on win/loss. */
public interface GameplayStatsTracker {

  /** Records one physics-frame worth of pig position and elapsed time. */
  void sample(World world, double dtSeconds);

  /** Produces an immutable snapshot of the run with the given outcome. */
  GameplayScore finish(boolean won);
}
