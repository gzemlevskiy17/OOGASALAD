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
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.model.engine.internal.joints.RigidJoint;
import org.junit.jupiter.api.Test;

/**
 * Verifies collision-detection filtering in {@link GameEngineImpl}: jointed pairs are skipped,
 * overlapping pairs reach narrow phase, and separated pairs are culled by the broad phase.
 *
 * @author Paulina Vvedenskaya
 */
class GameEngineCollisionFilterTest {

  private static final double DT = 1.0 / 60.0;

  private static double boxMoI(double mass, double width, double height) {
    return mass * (width * width + height * height) / 12.0;
  }

  private boolean containsPair(List<Pair> pairs, GameObject x, GameObject y) {
    for (Pair p : pairs) {
      if ((p.a() == x && p.b() == y) || (p.a() == y && p.b() == x)) {
        return true;
      }
    }
    return false;
  }

  @Test
  void step_jointedMainCharacterAndFrame_collisionDetectorSkipsPair() {
    WorldImpl world = new WorldImpl();
    double pw = 40.0;
    double ph = 40.0;
    PigComponent mainCharacter = new PigComponent(
        100, 100, 0,
        ConvexPolygon.axisAlignedBox(100, 100, pw, ph),
        "", 0.3, 2.0, boxMoI(2.0, pw, ph), 0.5, true, 100.0, "MainCharacter");
    double fw = 60.0;
    double fh = 60.0;
    StructuralComponent frame = new StructuralComponent(
        100, 100, 0,
        ConvexPolygon.axisAlignedBox(100, 100, fw, fh),
        "", 0.3, 1.5, boxMoI(1.5, fw, fh), 0.5, true, 100, "Frame", fw, fh);

    RigidJoint joint = new RigidJoint(mainCharacter, frame);
    world.addComponent(mainCharacter);
    world.addComponent(frame);
    world.addJoint(joint);

    RecordingDetector detector = new RecordingDetector();
    GameEngineImpl engine = new GameEngineImpl(world, detector);
    engine.step(DT);

    assertFalse(containsPair(detector.pairs, mainCharacter, frame),
        "Jointed pair should be skipped by collision detection");
  }

  @Test
  void step_twoSeparatedFrames_broadPhaseSkipsPair() {
    WorldImpl world = new WorldImpl();
    double fw = 60.0;
    double fh = 60.0;
    StructuralComponent frameA = new StructuralComponent(
        100, 100, 0,
        ConvexPolygon.axisAlignedBox(100, 100, fw, fh),
        "", 0.3, 1.5, boxMoI(1.5, fw, fh), 0.5, true, 100, "Frame", fw, fh);
    StructuralComponent frameB = new StructuralComponent(
        200, 100, 0,
        ConvexPolygon.axisAlignedBox(200, 100, fw, fh),
        "", 0.3, 1.5, boxMoI(1.5, fw, fh), 0.5, true, 100, "Frame", fw, fh);
    world.addComponent(frameA);
    world.addComponent(frameB);

    RecordingDetector detector = new RecordingDetector();
    GameEngineImpl engine = new GameEngineImpl(world, detector);
    engine.step(DT);

    assertFalse(containsPair(detector.pairs, frameA, frameB),
        "Separated frames should not reach narrow phase");
  }

  @Test
  void step_mainCharacterOverlappingTerrain_collisionDetectorIsCalled() {
    WorldImpl world = new WorldImpl();
    double pw = 40.0;
    double ph = 40.0;
    PigComponent mainCharacter = new PigComponent(
        100, 160, 0,
        ConvexPolygon.axisAlignedBox(100, 160, pw, ph),
        "", 0.3, 2.0, boxMoI(2.0, pw, ph), 0.5, true, 100.0, "MainCharacter");
    TerrainBlock terrain = new TerrainBlock(0, 190, 400, 20, "t.png", false, 0.5, 0.5);
    world.addComponent(mainCharacter);
    world.addComponent(terrain);

    RecordingDetector detector = new RecordingDetector();
    GameEngineImpl engine = new GameEngineImpl(world, detector);
    engine.step(DT);

    assertTrue(containsPair(detector.pairs, mainCharacter, terrain),
        "Overlapping main character and terrain should reach narrow phase");
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
