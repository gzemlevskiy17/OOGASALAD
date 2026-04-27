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
  private final String mainCharacterId;
  private Double startX;
  private Double startY;
  private Double finishX;
  private Double finishY;
  private double lastPigX;
  private double lastPigY;
  private double distanceTraveled;
  private double elapsedSeconds;

  public GameplayStatsTrackerImpl(String levelId) {
    this(levelId, null);
  }

  public GameplayStatsTrackerImpl(String levelId, String mainCharacterId) {
    this.levelId = levelId;
    this.mainCharacterId = mainCharacterId;
  }

  @Override
  public void sample(World world, double dtSeconds) {
    List<GameObject> mainCharacters = MainCharacterLookup.find(world, mainCharacterId);
    List<GameObject> finishes = world.getObjectsByTag(DefaultTag.FINISH_LINE.value());
    if (mainCharacters.isEmpty() || finishes.isEmpty()) {
      return;
    }
    double mainCharacterX = mainCharacters.get(0).x();
    double mainCharacterY = mainCharacters.get(0).y();
    if (startX == null) {
      startX = mainCharacterX;
      startY = mainCharacterY;
      finishX = finishes.get(0).x();
      finishY = finishes.get(0).y();
      lastPigX = mainCharacterX;
      lastPigY = mainCharacterY;
    } else {
      distanceTraveled += Math.hypot(mainCharacterX - lastPigX, mainCharacterY - lastPigY);
      lastPigX = mainCharacterX;
      lastPigY = mainCharacterY;
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
  public double currentProgressPercent() {
    if (startX == null || finishX == null) {
      return 0.0;
    }
    double span = Math.hypot(finishX - startX, finishY - startY);
    if (span <= 0) {
      return 0.0;
    }
    double remaining = Math.hypot(finishX - lastPigX, finishY - lastPigY);
    return Math.max(0.0, Math.min(1.0, 1.0 - remaining / span));
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
