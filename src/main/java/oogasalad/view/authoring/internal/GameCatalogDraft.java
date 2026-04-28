package oogasalad.view.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;

/**
 * Mutable in-progress game catalog edited by the game authoring screen.
 *
 * @author Emilio Garcia
 */
final class GameCatalogDraft {

  private static final String DEFAULT_SAVE_FILE_PATH = "data/games/untitled/game.json";

  private final List<TerrainInfo> terrainTypes = new ArrayList<>();
  private final List<StaticComponentInfo> staticComponents = new ArrayList<>();
  private final List<ComponentInfo> components = new ArrayList<>();
  private final List<CheatInfo> cheats = new ArrayList<>();

  private String saveFilePath = DEFAULT_SAVE_FILE_PATH;

  List<TerrainInfo> terrainTypes() {
    return terrainTypes;
  }

  List<StaticComponentInfo> staticComponents() {
    return staticComponents;
  }

  List<ComponentInfo> components() {
    return components;
  }

  List<CheatInfo> cheats() {
    return cheats;
  }

  String saveFilePath() {
    return saveFilePath;
  }

  void setSaveFilePath(String saveFilePath) {
    this.saveFilePath = saveFilePath;
  }

  void loadFrom(GameConfig config, String sourcePath) {
    saveFilePath = sourcePath;
    terrainTypes.clear();
    terrainTypes.addAll(config.terrainTypes());
    staticComponents.clear();
    staticComponents.addAll(config.staticComponents());
    components.clear();
    components.addAll(config.components());
    cheats.clear();
    cheats.addAll(config.cheats());
  }

  void clear() {
    saveFilePath = DEFAULT_SAVE_FILE_PATH;
    terrainTypes.clear();
    staticComponents.clear();
    components.clear();
    cheats.clear();
  }
}
