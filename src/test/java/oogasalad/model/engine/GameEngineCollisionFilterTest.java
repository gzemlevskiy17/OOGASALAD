package oogasalad.model.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import oogasalad.model.engine.api.CollisionDetector;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.internal.components.PigComponent;
import oogasalad.model.engine.internal.components.StructuralComponent;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link GameEngineImpl} skips collision evaluation for pig↔vehicle pairs so a pig
 * rigid-jointed inside a frame isn't pushed out on the first step.
 */
class GameEngineCollisionFilterTest {

  private static double boxMoI(double mass, double width, double height) {
    return mass * (width * width + height * height) / 12.0;
  }

  private boolean containsPair(List<Pair> pairs, GameObject x, GameObject y) {
    return pairs.stream().anyMatch(p ->
        (p.a() == x && p.b() == y) || (p.a() == y && p.b() == x));
  }

  @Test
  void step_pigAndFrameSameCell_collisionDetectorNotCalledForThePair() {
    WorldImpl world = new WorldImpl();
    double pw = 40.0;
    double ph = 40.0;
    PigComponent pig = new PigComponent(
        100,
        100,
        0,
        ConvexPolygon.axisAlignedBox(100, 100, pw, ph),
        "",
        0.3,
        2.0,
        boxMoI(2.0, pw, ph),
        0.5,
        true);
    double fw = 60.0;
    double fh = 60.0;
    StructuralComponent frame =
        new StructuralComponent(
            100,
            100,
            0,
            ConvexPolygon.axisAlignedBox(100, 100, fw, fh),
            "",
            0.3,
            1.5,
            boxMoI(1.5, fw, fh),
            0.5,
            true,
            100,
            "Frame",
            "",
            fw,
            fh);
    world.addComponent(pig);
    world.addComponent(frame);

    RecordingDetector detector = new RecordingDetector();
    GameEngineImpl engine = new GameEngineImpl(world, detector);

    engine.step(1.0 / 60.0);

    assertFalse(containsPair(detector.pairs, pig, frame),
        "Engine should skip collision detection for pig↔vehicle pair");
  }

  @Test
  void step_twoFrames_collisionDetectorStillSkipsVehicleVehicle() {
    WorldImpl world = new WorldImpl();
    double fw = 60.0;
    double fh = 60.0;
    StructuralComponent frameA =
        new StructuralComponent(
            100,
            100,
            0,
            ConvexPolygon.axisAlignedBox(100, 100, fw, fh),
            "",
            0.3,
            1.5,
            boxMoI(1.5, fw, fh),
            0.5,
            true,
            100,
            "Frame",
            "",
            fw,
            fh);
    StructuralComponent frameB =
        new StructuralComponent(
            200,
            100,
            0,
            ConvexPolygon.axisAlignedBox(200, 100, fw, fh),
            "",
            0.3,
            1.5,
            boxMoI(1.5, fw, fh),
            0.5,
            true,
            100,
            "Frame",
            "",
            fw,
            fh);
    world.addComponent(frameA);
    world.addComponent(frameB);

    RecordingDetector detector = new RecordingDetector();
    GameEngineImpl engine = new GameEngineImpl(world, detector);

    engine.step(1.0 / 60.0);

    assertFalse(containsPair(detector.pairs, frameA, frameB),
        "Vehicle↔vehicle collisions should still be filtered (regression)");
  }

  @Test
  void step_pigAndTerrain_collisionDetectorIsCalled() {
    WorldImpl world = new WorldImpl();
    double pw = 40.0;
    double ph = 40.0;
    PigComponent pig = new PigComponent(
        100,
        100,
        0,
        ConvexPolygon.axisAlignedBox(100, 100, pw, ph),
        "",
        0.3,
        2.0,
        boxMoI(2.0, pw, ph),
        0.5,
        true);
    oogasalad.model.engine.internal.components.TerrainBlock terrain =
        new oogasalad.model.engine.internal.components.TerrainBlock(
            0, 200, 400, 20, "t.png", false, 0.5, 0.5);
    world.addComponent(pig);
    world.addComponent(terrain);

    RecordingDetector detector = new RecordingDetector();
    GameEngineImpl engine = new GameEngineImpl(world, detector);

    engine.step(1.0 / 60.0);

    assertTrue(containsPair(detector.pairs, pig, terrain),
        "Pig vs terrain should still collide so the pig can land on ground");
  }

  private record Pair(GameObject a, GameObject b) {

  }

  private static class RecordingDetector implements CollisionDetector {

    final List<Pair> pairs = new ArrayList<>();

    @Override
    public Optional<Collision> detectCollision(GameObject a, GameObject b) {
      pairs.add(new Pair(a, b));
      return Optional.empty();
    }
  }
}
