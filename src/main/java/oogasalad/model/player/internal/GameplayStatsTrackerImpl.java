package oogasalad.model.player.internal;

import java.time.Instant;
import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.GameplayStatsTracker;

/**
 * Samples the pig's x-position each frame to accumulate path length and elapsed time, and
 * captures the level span ({@code finishX - startX}) on the first frame so the produced score
 * can be expressed as a progress percentage. Sampling is a no-op until both a pig and a
 * finish-line exist in the world.
 */
public class GameplayStatsTrackerImpl implements GameplayStatsTracker {

  private final String levelId;
  private Double startX;
  private Double finishX;
  private double lastPigX;
  private double distanceTraveled;
  private double elapsedSeconds;

  public GameplayStatsTrackerImpl(String levelId) {
    this.levelId = levelId;
  }

  @Override
  public void sample(World world, double dtSeconds) {
    List<GameObject> pigs = world.getObjectsByTag(DefaultTag.PIGS.value());
    List<GameObject> finishes = world.getObjectsByTag(DefaultTag.FINISH_LINE.value());
    if (pigs.isEmpty() || finishes.isEmpty()) {
      return;
    }
    double pigX = pigs.get(0).getX();
    if (startX == null) {
      startX = pigX;
      finishX = finishes.get(0).getX();
      lastPigX = pigX;
    } else {
      distanceTraveled += Math.abs(pigX - lastPigX);
      lastPigX = pigX;
    }
    elapsedSeconds += dtSeconds;
  }

  @Override
  public GameplayScore finish(boolean won) {
    double span = 0.0;
    double progress = 0.0;
    double remaining = 0.0;
    if (startX != null && finishX != null) {
      span = finishX - startX;
      if (span > 0) {
        progress = Math.max(0.0, Math.min(1.0, (lastPigX - startX) / span));
      }
      remaining = Math.max(0.0, finishX - lastPigX);
    }
    return new GameplayScore(
        levelId, distanceTraveled, remaining, span, progress, elapsedSeconds, won, Instant.now()
    );
  }
}
