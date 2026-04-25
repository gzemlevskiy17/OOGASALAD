package oogasalad.controller.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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

    controller.handleGridInteraction(80.0, 40.0);

    assertEquals(9, world.objectCount());
  }

  @Test
  void autoFill_noClosedShape_fillsEntireOpenRegion() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(200.0, 200.0);
    assertEquals(1, world.objectCount());

    controller.handleGridInteraction(200.0, 200.0);

    assertEquals(100, world.objectCount());
  }

  @Test
  void autoFill_unevenSides_fillsSmallerSide() {
    toolSelector.setActiveTool(ToolType.DRAW);
    for (int r = 0; r < 10; r++) {
      controller.handleGridInteraction(140.0, r * 40.0 + 20.0);
    }
    assertEquals(10, world.objectCount());

    controller.handleGridInteraction(140.0, 220.0);

    assertEquals(40, world.objectCount());
  }

  // --- Erase integration tests ---

  @Test
  void erase_placedBlock_worldIsEmpty() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);

    toolSelector.setActiveTool(ToolType.ERASE);
    controller.handleGridInteraction(20.0, 20.0);

    assertEquals(0, world.objectCount());
  }

  @Test
  void erase_onlyRemovesTargetBlock_notNeighbor() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);   // col=0
    controller.handleGridInteraction(160.0, 20.0);  // col=4
    assertEquals(2, world.objectCount());

    toolSelector.setActiveTool(ToolType.ERASE);
    controller.handleGridInteraction(20.0, 20.0);

    assertEquals(1, world.objectCount());
  }

  @Test
  void erase_emptyCell_worldUnchanged() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);

    toolSelector.setActiveTool(ToolType.ERASE);
    controller.handleGridInteraction(160.0, 20.0);

    assertEquals(1, world.objectCount());
  }

  @Test
  void drawThenEraseAll_worldIsEmpty() {
    toolSelector.setActiveTool(ToolType.DRAW);
    controller.handleGridInteraction(20.0, 20.0);
    controller.handleGridInteraction(160.0, 20.0);
    controller.handleGridInteraction(20.0, 160.0);

    toolSelector.setActiveTool(ToolType.ERASE);
    controller.handleGridInteraction(20.0, 20.0);
    controller.handleGridInteraction(160.0, 20.0);
    controller.handleGridInteraction(20.0, 160.0);

    assertEquals(0, world.objectCount());
  }

  // --- Infinite canvas expansion tests ---

  @Test
  void drawBeyondInitialBounds_canvasExpands() {
    ExpandingSpyCanvas expanding = new ExpandingSpyCanvas(5, 5);
    AuthoringControllerImpl ctrl = new AuthoringControllerImpl(
        world, loader, saver, new LevelAttributes(), toolSelector, expanding);

    toolSelector.setActiveTool(ToolType.DRAW);
    ctrl.handleGridInteraction(400.0, 20.0); // col=10, beyond initial maxCol of 5

    assertTrue(expanding.wasExpanded);
    assertEquals(1, world.objectCount());
  }

  @Test
  void drawBeyondInitialBounds_blockIsStillPlaced() {
    ExpandingSpyCanvas expanding = new ExpandingSpyCanvas(5, 5);
    AuthoringControllerImpl ctrl = new AuthoringControllerImpl(
        world, loader, saver, new LevelAttributes(), toolSelector, expanding);

    toolSelector.setActiveTool(ToolType.DRAW);
    ctrl.handleGridInteraction(400.0, 400.0);
    ctrl.handleGridInteraction(400.0, 400.0); // second draw — autoFill kicks in, block already there

    assertTrue(world.objectCount() >= 1);
  }

  @Test
  void drawWithinBounds_canvasDoesNotExpand() {
    ExpandingSpyCanvas expanding = new ExpandingSpyCanvas(5, 5);
    AuthoringControllerImpl ctrl = new AuthoringControllerImpl(
        world, loader, saver, new LevelAttributes(), toolSelector, expanding);

    toolSelector.setActiveTool(ToolType.DRAW);
    ctrl.handleGridInteraction(20.0, 20.0); // col=0, row=0 — within initial 5×5

    assertFalse(expanding.wasExpanded);
  }

  // --- Test doubles ---

  private static class FakeWorld implements World {
    private final List<GameComponent> components = new ArrayList<>();

    @Override
    public List<GameComponent> getComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<GameComponent> getAllComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<GameComponent> getActiveComponents() {
      return List.copyOf(components);
    }

    @Override
    public List<Joint> getJoints() {
      return List.of();
    }
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
    @Override public void loadLevel(String f, World w, LevelAttributes a, GameConfig c) { lastFileName = f; }
  }

  private static class SpyLevelSaver implements LevelSaver {
    String lastFileName;
    @Override public void saveLevel(String f, World w, LevelAttributes a) { lastFileName = f; }
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
    @Override public void setZoom(double factor) {}
    @Override public double getZoom() { return 1.0; }
    @Override public void addOverlay(javafx.scene.Node node) {}
    @Override public void clearOverlays() {}
  }

  /**
   * Canvas spy that mirrors the real AuthoringCanvasImpl auto-expansion logic,
   * allowing tests to verify that drawing beyond the initial grid triggers expansion.
   */
  private static class ExpandingSpyCanvas extends SpyCanvas {
    private int maxCol;
    private int maxRow;
    boolean wasExpanded = false;

    ExpandingSpyCanvas(int initialCols, int initialRows) {
      this.maxCol = initialCols;
      this.maxRow = initialRows;
    }

    @Override
    public boolean isWithinBounds(int col, int row) {
      if (col < 0 || row < 0) {
        return false;
      }
      if (col >= maxCol || row >= maxRow) {
        maxCol = Math.max(maxCol, col + 1);
        maxRow = Math.max(maxRow, row + 1);
        wasExpanded = true;
      }
      return true;
    }
  }
}
