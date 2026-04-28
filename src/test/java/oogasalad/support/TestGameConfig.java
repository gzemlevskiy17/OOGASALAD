package oogasalad.support;

import java.util.List;
import java.util.Map;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;

/**
 * In-memory game catalog fixture for unit tests.
 * @author Emilio Garcia
 */
public final class TestGameConfig implements GameConfig {

  private final String gameId;
  private final String mainCharacterId;
  private final List<ComponentInfo> components;
  private final List<StaticComponentInfo> staticComponents;
  private final List<TerrainInfo> terrainTypes;
  private final List<CheatInfo> cheats;
  private final Map<String, String> settings;

  public TestGameConfig(String gameId, String mainCharacterId,
      List<ComponentInfo> components,
      List<StaticComponentInfo> staticComponents,
      List<TerrainInfo> terrainTypes) {
    this(gameId, mainCharacterId, components, staticComponents, terrainTypes, List.of());
  }

  public TestGameConfig(String gameId, String mainCharacterId,
      List<ComponentInfo> components,
      List<StaticComponentInfo> staticComponents,
      List<TerrainInfo> terrainTypes,
      List<CheatInfo> cheats) {
    this.gameId = gameId;
    this.mainCharacterId = mainCharacterId;
    this.components = List.copyOf(components);
    this.staticComponents = List.copyOf(staticComponents);
    this.terrainTypes = List.copyOf(terrainTypes);
    this.cheats = List.copyOf(cheats);
    this.settings = Map.of();
  }

  public static TestGameConfig badPiggies() {
    return new TestGameConfig(
        "badpiggies",
        "BigPig",
        List.of(
            component("BigPig"),
            component("SmallPig"),
            driveComponent("Fan"),
            component("Frame"),
            component("Wood Wheel")),
        List.of(
            staticComponent("GoalLine"),
            staticComponent("Coin"),
            staticComponent("Rock"),
            staticComponent("XPBoost"),
            staticComponent("Bomb"),
            staticComponent("Heart")),
        List.of(terrain("GrassHills"), terrain("DirtPath")));
  }

  public static ComponentInfo component(String id) {
    return new ComponentInfo(id, "", 1.0, List.of());
  }

  public static ComponentInfo driveComponent(String id) {
    return new ComponentInfo(id, "", 1.0, List.of(new ButtonInfo("Push", 1.0, 0.0)));
  }

  public static StaticComponentInfo staticComponent(String id) {
    return new StaticComponentInfo(id, "", 0.0, null);
  }

  public static TerrainInfo terrain(String id) {
    return new TerrainInfo(id, "", "#00AA00", 0.5);
  }

  @Override
  public String gameId() {
    return gameId;
  }

  @Override
  public String mainCharacterId() {
    return mainCharacterId;
  }

  @Override
  public List<ComponentInfo> components() {
    return List.copyOf(components);
  }

  @Override
  public List<StaticComponentInfo> staticComponents() {
    return List.copyOf(staticComponents);
  }

  @Override
  public List<TerrainInfo> terrainTypes() {
    return List.copyOf(terrainTypes);
  }

  @Override
  public List<CheatInfo> cheats() {
    return List.copyOf(cheats);
  }

  @Override
  public String getSetting(String key) {
    return settings.get(key);
  }

  @Override
  public double getNumericSetting(String key) {
    return Double.parseDouble(settings.get(key));
  }
}
