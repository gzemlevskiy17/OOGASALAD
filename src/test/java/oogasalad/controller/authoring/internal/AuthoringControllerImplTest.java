package oogasalad.controller.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.data.api.GameConfig;
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
        world, loader, saver, new FakeGameConfig(), toolSelector, canvas);
  }

  @Test
  void handleGridInteraction_drawTool_emptyCell_addsTerrainBlock() {
    toolSelector.setActiveTool(ToolType.DRAW);

    controller.handleGridInteraction(20.0, 20.0);

    assertEquals(1, world.objectCount());
  }

  @Test
  void handleGridInteraction_drawTool_occupiedCell_doesNotAddDuplicate() {
    toolSelector.setActiveTool(ToolType.DRAW);

    controller.handleGridInteraction(20.0, 20.0);
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

  // --- Test doubles ---

  private static class FakeGameConfig implements GameConfig {
    @Override public String getSetting(String key) { return null; }
    @Override public double getNumericSetting(String key) { return 0; }
  }

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
    @Override public void setSelectedPlaceable(AuthoringPlaceable placeable) {}
    @Override public void placeBlock(int col, int row) {}
    @Override public void showHoverPreview(int col, int row) {}
    @Override public void clearHoverPreview() {}
  }
}
