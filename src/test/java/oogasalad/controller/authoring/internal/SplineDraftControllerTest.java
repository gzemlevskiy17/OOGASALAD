package oogasalad.controller.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SplineDraftController}.
 *
 * <p>Focus: knot-placement rules (rejects knots at or below the grid baseline),
 * commit/cancel semantics and the overlay contract with the canvas
 * (redraws happen after every mutation).
 *
 * <p>{@link SplineDraftController} renders draft overlays using JavaFX nodes. We bootstrap the
 * JavaFX toolkit once per class so node construction does not trip the "Toolkit not initialized"
 * guard on CI machines that have never run a JavaFX Application.
 */
class SplineDraftControllerTest {

  private static final double CELL_SIZE = 20.0;

  private FakeWorld world;
  private RecordingCanvas canvas;
  private SplineDraftController controller;

  @BeforeAll
  static void initJavaFx() {
    // Force Monocle (software) rendering so tests run on headless CI agents that have no
    // display server. Must be set before the toolkit is started.
    System.setProperty("java.awt.headless", "true");
    System.setProperty("testfx.robot", "glass");
    System.setProperty("glass.platform", "Monocle");
    System.setProperty("monocle.platform", "Headless");
    System.setProperty("prism.order", "sw");
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException alreadyStarted) {
      // Platform.startup throws IllegalStateException if the toolkit has already been initialized
      // by another test class. That is the desired no-op behaviour.
    }
  }

  @BeforeEach
  void setUp() {
    world = new FakeWorld();
    canvas = new RecordingCanvas();
    controller = new SplineDraftController(world, canvas);
  }

  @Test
  void initialState_emptyDraft() {
    assertEquals(0, controller.draftSize());
  }

  @Test
  void addKnot_belowSpawnGuide_isAccepted() {
    controller.addKnot(10.0, 20.0);

    assertEquals(1, controller.draftSize());
  }

  @Test
  void addKnot_aboveSpawnGuide_isAccepted() {
    controller.addKnot(10.0, 5.0);

    assertEquals(1, controller.draftSize());
  }

  @Test
  void addKnot_exactlyAtSpawnRow_isAccepted() {
    controller.addKnot(10.0, 15.0); // spawnY / cellSize = 15

    assertEquals(1, controller.draftSize());
  }

  @Test
  void addKnot_atOrBelowBaseline_isRejected() {
    // Baseline sits at the last grid row (rows=110). Anything at or past that is disallowed to
    // preserve the ParabolaSpline invariant "knot.y < baselineY".
    controller.addKnot(10.0, 110.0);
    controller.addKnot(10.0, 200.0);

    assertEquals(0, controller.draftSize());
  }

  @Test
  void addKnot_triggersOverlayRender() {
    controller.addKnot(10.0, 20.0);

    assertTrue(canvas.overlaysCleared >= 1);
    assertTrue(canvas.overlayAdditions >= 1); // at least the knot marker
  }

  @Test
  void moveCursor_triggersOverlayRender() {
    controller.addKnot(10.0, 20.0);
    int before = canvas.overlayAdditions;

    controller.moveCursor(12.0, 22.0);

    assertTrue(canvas.overlayAdditions > before);
  }

  @Test
  void commit_withFewerThanTwoKnots_returnsNullAndDoesNotTouchWorld() {
    controller.addKnot(10.0, 20.0);

    TerrainSpline committed = controller.commit();

    assertNull(committed);
    assertEquals(0, world.components.size());
    assertEquals(0, controller.draftSize());
  }

  @Test
  void commit_withTwoOrMoreKnots_addsTerrainSplineToWorld() {
    controller.addKnot(0.0, 20.0);
    controller.addKnot(10.0, 22.0);

    TerrainSpline committed = controller.commit();

    assertNotNull(committed);
    assertEquals(1, world.components.size());
    assertInstanceOf(TerrainSpline.class, world.components.get(0));
    assertEquals(0, controller.draftSize(), "draft should be cleared after commit");
  }

  @Test
  void cancel_clearsDraftAndOverlays() {
    controller.addKnot(10.0, 20.0);
    controller.addKnot(20.0, 25.0);

    controller.cancel();

    assertEquals(0, controller.draftSize());
    assertTrue(canvas.overlaysCleared > 0);
    assertTrue(canvas.refreshCalled);
  }

  // --- Test doubles ---

  private static final class FakeWorld implements World {
    final List<GameComponent> components = new ArrayList<>();
    @Override public List<GameComponent> getComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getAllComponents() { return List.copyOf(components); }
    @Override public List<GameComponent> getActiveComponents() { return List.copyOf(components); }
    @Override public List<Joint> getJoints() { return List.of(); }
    @Override public void addComponent(GameComponent c) { components.add(c); }
    @Override public void removeComponent(GameComponent c) { components.remove(c); }
    @Override public void clear() { components.clear(); }
    @Override public void addJoint(Joint j) {}
    @Override public void removeJoint(Joint j) {}
  }

  /** Counts overlay operations so we can verify the controller keeps the canvas in sync. */
  private static final class RecordingCanvas implements AuthoringCanvas {
    int overlaysCleared = 0;
    int overlayAdditions = 0;
    boolean refreshCalled = false;

    @Override public javafx.scene.Node getVisualRoot() { return null; }
    @Override public void refreshGrid() { refreshCalled = true; }
    @Override public boolean isWithinBounds(int col, int row) { return true; }
    @Override public double getCellSize() { return CELL_SIZE; }
    @Override public int getGridCols() { return 160; }
    @Override public int getGridRows() { return 110; }
    @Override public void setSelectedPlaceable(AuthoringPlaceable p) {}
    @Override public void placeBlock(int col, int row) {}
    @Override public void showHoverPreview(int col, int row) {}
    @Override public void clearHoverPreview() {}
    @Override public void setOnObjectSelected(Consumer<AuthoringPlaceable> h) {}
    @Override public void setZoom(double factor) {}
    @Override public double getZoom() { return 1.0; }
    @Override public void addOverlay(javafx.scene.Node node) { overlayAdditions++; }
    @Override public void clearOverlays() { overlaysCleared++; }
  }
}
