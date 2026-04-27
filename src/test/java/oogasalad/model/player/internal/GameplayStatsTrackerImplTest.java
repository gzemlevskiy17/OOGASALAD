package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.player.api.GameplayScore;
import oogasalad.model.player.api.GameplayStatsTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameplayStatsTrackerImplTest {

  private static final double DT = 1.0 / 60.0;

  private static PigComponent newPig() {
    return new PigComponent(0, 0, 0,
        ConvexPolygon.axisAlignedBox(-20, -20, 40, 40),
        "", 0, 1.0, 1.0, 0.1, true, 100, "pig");
  }

  private World world;
  private PigComponent pig;
  private GameplayStatsTracker tracker;

  @BeforeEach
  void setUp() {
    world = new WorldImpl();
    pig = newPig();
    world.addComponent(pig);
    world.addComponent(new GoalLineComponent(1000, 0, 20, 400));
    tracker = new GameplayStatsTrackerImpl("data/levels/test.json");
  }

  @Test
  void finishBeforeSampling_producesZeros() {
    GameplayScore score = tracker.finish(false);

    assertEquals(0.0, score.distanceTraveled());
    assertEquals(0.0, score.elapsedSeconds());
    assertEquals(0.0, score.levelSpan());
    assertEquals(0.0, score.progressPercent());
    assertFalse(score.won());
  }

  @Test
  void forwardSamples_accumulateDistanceAndTime_andReportSpanAndProgress() {
    tracker.sample(world, DT);
    pig.setPosition(100, 0, 0);
    tracker.sample(world, DT);
    pig.setPosition(250, 0, 0);
    tracker.sample(world, DT);

    GameplayScore score = tracker.finish(true);

    assertEquals(250.0, score.distanceTraveled(), 1e-6);
    assertEquals(3 * DT, score.elapsedSeconds(), 1e-9);
    assertEquals(1000.0, score.levelSpan(), 1e-6);
    assertEquals(0.25, score.progressPercent(), 1e-6);
    assertEquals(750.0, score.remainingDistance(), 1e-6);
    assertTrue(score.won());
  }

  @Test
  void backtracking_inflatesDistanceTraveled_butProgressReflectsFinalPosition() {
    tracker.sample(world, DT);
    pig.setPosition(400, 0, 0);
    tracker.sample(world, DT);
    pig.setPosition(100, 0, 0);
    tracker.sample(world, DT);

    GameplayScore score = tracker.finish(false);

    assertEquals(700.0, score.distanceTraveled(), 1e-6);
    assertEquals(0.1, score.progressPercent(), 1e-6);
    assertEquals(900.0, score.remainingDistance(), 1e-6);
  }

  @Test
  void verticalMotion_contributesToDistanceAndPullsRemainingAndProgress() {
    tracker.sample(world, DT);
    pig.setPosition(0, 30, 0);
    tracker.sample(world, DT);
    pig.setPosition(40, 30, 0);
    tracker.sample(world, DT);

    GameplayScore score = tracker.finish(false);

    // Path lengths: (0,0)->(0,30)=30, (0,30)->(40,30)=40 → total 70
    assertEquals(70.0, score.distanceTraveled(), 1e-6);
    // Span is Euclidean start→finish: (0,0)→(1000,0) = 1000.
    // Remaining is Euclidean current→finish: (40,30)→(1000,0) = hypot(960, 30).
    double expectedRemaining = Math.hypot(960, 30);
    assertEquals(1000.0, score.levelSpan(), 1e-6);
    assertEquals(expectedRemaining, score.remainingDistance(), 1e-6);
    assertEquals(1.0 - expectedRemaining / 1000.0, score.progressPercent(), 1e-6);
  }

  @Test
  void sampleIsNoOp_whenPigIsMissing() {
    World onlyFinish = new WorldImpl();
    onlyFinish.addComponent(new GoalLineComponent(1000, 0, 20, 400));
    GameplayStatsTracker lonely = new GameplayStatsTrackerImpl("x");

    lonely.sample(onlyFinish, DT);
    GameplayScore score = lonely.finish(false);

    assertEquals(0.0, score.elapsedSeconds());
    assertEquals(0.0, score.levelSpan());
  }

  @Test
  void sampleIsNoOp_whenFinishLineIsMissing() {
    World onlyPig = new WorldImpl();
    onlyPig.addComponent(newPig());
    GameplayStatsTracker lonely = new GameplayStatsTrackerImpl("x");

    lonely.sample(onlyPig, DT);
    GameplayScore score = lonely.finish(false);

    assertEquals(0.0, score.elapsedSeconds());
    assertEquals(0.0, score.levelSpan());
  }
}
