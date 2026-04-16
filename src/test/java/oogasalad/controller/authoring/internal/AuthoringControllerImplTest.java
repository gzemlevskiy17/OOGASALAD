package oogasalad.controller.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.LevelAttributes;
import oogasalad.data.api.LevelLoader;
import oogasalad.data.api.LevelSaver;
import oogasalad.model.engine.api.Joint;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthoringControllerImplTest {

  private AuthoringControllerImpl controller;
  private FakeWorld world;
  private SpyLevelLoader loader;
  private SpyLevelSaver saver;
  private FakeToolSelector toolSelector;
  private SpyCanvas canvas;

  @BeforeEach
  void setUp() {
    world = new FakeWorld();
    loader = new SpyLevelLoader();
    saver = new SpyLevelSaver();
    toolSelector = new FakeToolSelector();
    canvas = new SpyCanvas();

    controller = new AuthoringControllerImpl(
        world, loader, saver, new LevelAttributes(), toolSelector, canvas);
  }

  @Test
  void handleGridInteraction_drawTool_emptyCell_addsTerrainBlock() {
    toolSelector.setActiveTool(ToolType.DRAW);

    controller.handleGridInteraction(20.0, 20.0);

    assertEquals(1, world.objectCount());
  }

  @Test
  void handleGridInteraction_anyTool_alwaysRefreshesCanvas() {
    toolSelector.setActiveTool(ToolType.DRAW);

    controller.handleGridInteraction(20.0, 20.0);

    assertTrue(canvas.refreshCalled);
  }

  @Test
  void saveLevel_delegatesToLevelSaver() {
    controller.saveLevel("output.json");

    assertEquals("output.json", saver.lastFileName);
  }

  @Test
  void loadLevel_delegatesToLevelLoader() {
    controller.loadLevel("input.json");

    assertEquals("input.json", loader.lastFileName);
  }

  @Test
  void loadLevel_afterLoad_refreshesCanvas() {
    controller.loadLevel("input.json");

    assertTrue(canvas.refreshCalled);
  }

  @Test
  void loadLevel_clearsExistingObjects() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);
    assertEquals(1, world.objectCount());

    controller.loadLevel("input.json");

    assertEquals(0, world.objectCount());
  }

  @Test
  void setActiveTool_switchesToErase_removesPlacedObject() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);
    assertEquals(1, world.objectCount());

    toolSelector.setActiveTool(ToolType.ERASE);
    controller.handleGridInteraction(20.0, 20.0);

    assertEquals(0, world.objectCount());
  }

  @Test
  void setActiveTool_switchesToolType() {
    controller.setActiveTool(ToolType.ERASE);
    assertEquals(ToolType.ERASE, toolSelector.getActiveTool());

    controller.setActiveTool(ToolType.DRAW);
    assertEquals(ToolType.DRAW, toolSelector.getActiveTool());
  }

  @Test
  void clearLevel_removesAllPlacedObjects() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);
    controller.handleGridInteraction(60.0, 60.0);
    assertEquals(2, world.objectCount());

    controller.clearLevel();

    assertEquals(0, world.objectCount());
  }

  @Test
  void clearLevel_emptyCanvas_remainsEmpty() {
    assertEquals(0, world.objectCount());

    controller.clearLevel();

    assertEquals(0, world.objectCount());
  }

  @Test
  void clearLevel_refreshesCanvas() {
    controller.clearLevel();

    assertTrue(canvas.refreshCalled);
  }

  @Test
  void clearLevel_afterLoad_removesLoadedObjects() {
    controller.loadLevel("input.json");
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);
    assertEquals(1, world.objectCount());

    controller.clearLevel();

    assertEquals(0, world.objectCount());
  }

  @Test
  void paint_onEmptyGrid_fillsAllConnectedCells() {
    toolSelector.setActiveTool(ToolType.PAINT);

    controller.handleGridInteraction(20.0, 20.0);

    assertEquals(100, world.objectCount());
  }

  @Test
  void paint_onOccupiedCell_doesNothing() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);
    assertEquals(1, world.objectCount());

    toolSelector.setActiveTool(ToolType.PAINT);
    controller.handleGridInteraction(20.0, 20.0);

    assertEquals(1, world.objectCount());
  }

  @Test
  void autoFill_closedShape_fillsSmallerInterior() {
    // Build a 3x3 hollow rectangle whose interior is the single cell (2, 2).
    // Perimeter cells: (1,1)(2,1)(3,1)(1,2)(3,2)(1,3)(2,3)(3,3) — 8 blocks.
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(40.0, 40.0);
    controller.handleGridInteraction(80.0, 40.0);
    controller.handleGridInteraction(120.0, 40.0);
    controller.handleGridInteraction(40.0, 80.0);
    controller.handleGridInteraction(120.0, 80.0);
    controller.handleGridInteraction(40.0, 120.0);
    controller.handleGridInteraction(80.0, 120.0);
    controller.handleGridInteraction(120.0, 120.0);
    assertEquals(8, world.objectCount());

    // Drawing on occupied wall cell (2, 1) triggers overlap detection.
    // The interior region (1 cell) is smaller than the exterior (91 cells),
    // so auto-fill places a block in the interior only.
    controller.handleGridInteraction(80.0, 40.0);

    assertEquals(9, world.objectCount());
  }

  @Test
  void autoFill_noClosedShape_fillsEntireOpenRegion() {
    // With only a single block placed, clicking on it triggers auto-fill.
    // Since grid edges are treated as boundaries, the surrounding open region
    // is considered "enclosed" and gets filled entirely.
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(200.0, 200.0);
    assertEquals(1, world.objectCount());

    controller.handleGridInteraction(200.0, 200.0);

    assertEquals(100, world.objectCount());
  }

  @Test
  void autoFill_unevenSides_fillsSmallerSide() {
    // Vertical wall at column 3 divides the grid into a left region
    // (3 cols x 10 rows = 30 cells) and a right region (6 cols x 10 rows = 60).
    toolSelector.setActiveTool(ToolType.DRAW);
    for (int r = 0; r < 10; r++) {
      controller.handleGridInteraction(140.0, r * 40.0 + 20.0);
    }
    assertEquals(10, world.objectCount());

    // Drawing on the wall at (3, 5) triggers auto-fill. The smaller left
    // region gets filled (30 cells), not the larger right region.
    controller.handleGridInteraction(140.0, 220.0);

    assertEquals(40, world.objectCount());
  }

  // --- Test doubles ---

  private static class FakeWorld implements World {
    private final List<GameComponent> components = new ArrayList<>();
    @Override public Collection<GameComponent> getComponents() { return Collections.unmodifiableList(components); }
    @Override public Collection<GameComponent> getAllComponents() { return Collections.unmodifiableList(components); }
    @Override public Collection<GameComponent> getActiveComponents() { return Collections.unmodifiableList(components); }
    @Override public Collection<Joint> getJoints() { return List.of(); }
    @Override public void addComponent(GameComponent component) { components.add(component); }
    @Override public void removeComponent(GameComponent component) { components.remove(component); }
    @Override public void clear() { components.clear(); }
    @Override public void addJoint(Joint j) {}
    @Override public void removeJoint(Joint j) {}
    int objectCount() { return components.size(); }
  }

  private static class FakeToolSelector implements oogasalad.controller.authoring.api.ToolSelector {
    private ToolType active = ToolType.DRAW;
    @Override public void setActiveTool(ToolType t) { active = t; }
    @Override public ToolType getActiveTool() { return active; }
  }

  private static class SpyLevelLoader implements LevelLoader {
    String lastFileName;
    @Override public void loadLevel(String f, World w, GameConfig c) { lastFileName = f; }
  }

  private static class SpyLevelSaver implements LevelSaver {
    String lastFileName;
    @Override public void saveLevel(String f, World w) { lastFileName = f; }
  }

  private static class SpyCanvas implements AuthoringCanvas {
    boolean refreshCalled = false;
    @Override public javafx.scene.Node getVisualRoot() { return null; }
    @Override public void refreshGrid() { refreshCalled = true; }
    @Override public boolean isWithinBounds(int col, int row) { return true; }
    @Override public double getCellSize() { return 40.0; }
    @Override public int getGridCols() { return 10; }
    @Override public int getGridRows() { return 10; }
    @Override public void setSelectedPlaceable(AuthoringPlaceable placeable) {}
    @Override public void placeBlock(int col, int row) {}
    @Override public void showHoverPreview(int col, int row) {}
    @Override public void clearHoverPreview() {}
    @Override public void setOnObjectSelected(java.util.function.Consumer<AuthoringPlaceable> handler) {}
  }
}
