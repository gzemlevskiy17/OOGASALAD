package oogasalad.model.player.internal;

import java.time.Instant;
import java.util.List;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.GameplayStatsTracker;

/**
 * Samples the pig's position each frame to accumulate Euclidean path length and elapsed time,
 * and captures the start→finish straight-line distance on the first frame so the produced score
 * can be expressed as a progress percentage. Sampling is a no-op until both a pig and a
 * finish-line exist in the world.
 */
public class GameplayStatsTrackerImpl implements GameplayStatsTracker {

  private final String levelId;
  private Double startX;
  private Double startY;
  private Double finishX;
  private Double finishY;
  private double lastPigX;
  private double lastPigY;
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
    double pigY = pigs.get(0).getY();
    if (startX == null) {
      startX = pigX;
      startY = pigY;
      finishX = finishes.get(0).getX();
      finishY = finishes.get(0).getY();
      lastPigX = pigX;
      lastPigY = pigY;
    } else {
      distanceTraveled += Math.hypot(pigX - lastPigX, pigY - lastPigY);
      lastPigX = pigX;
      lastPigY = pigY;
    }
    elapsedSeconds += dtSeconds;
  }

  @Override
  public double currentDistanceTraveled() {
    return distanceTraveled;
  }

  @Override
  public double currentElapsedSeconds() {
    return elapsedSeconds;
  }

  @Override
  public GameplayScore finish(boolean won) {
    double span = 0.0;
    double progress = 0.0;
    double remaining = 0.0;
    if (startX != null && finishX != null) {
      span = Math.hypot(finishX - startX, finishY - startY);
      remaining = Math.hypot(finishX - lastPigX, finishY - lastPigY);
      if (span > 0) {
        progress = Math.max(0.0, Math.min(1.0, 1.0 - remaining / span));
      }
    }
    return new GameplayScore(
        levelId, distanceTraveled, remaining, span, progress, elapsedSeconds, won, Instant.now()
    );
  }
}
