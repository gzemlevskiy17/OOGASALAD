package oogasalad.model.authoring.api;

import java.util.List;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;

/**
 * Immutable Good Piggies catalog used to constrain AI level generation to one concrete game.
 *
 * @param gameId configured game identifier
 * @param primaryComponentId catalog id of the primary player subject
 * @param components buildable vehicle components
 * @param staticComponents placeable map objects such as pickups, hazards, and goal line
 * @param terrainTypes terrain materials available to authored levels
 * @author Emilio Garcia
 */
public record GoodPiggiesCatalog(
    String gameId,
    String primaryComponentId,
    List<ComponentInfo> components,
    List<StaticComponentInfo> staticComponents,
    List<TerrainInfo> terrainTypes) {

  public GoodPiggiesCatalog {
    components = List.copyOf(components);
    staticComponents = List.copyOf(staticComponents);
    terrainTypes = List.copyOf(terrainTypes);
  }

  /**
   * @return ordered component ids from the Good Piggies game catalog
   */
  public List<String> componentIds() {
    return components.stream().map(ComponentInfo::id).toList();
  }

  /**
   * @return ordered static object ids from the Good Piggies game catalog
   */
  public List<String> staticComponentIds() {
    return staticComponents.stream().map(StaticComponentInfo::id).toList();
  }

  /**
   * @return ordered terrain ids from the Good Piggies game catalog
   */
  public List<String> terrainTypeIds() {
    return terrainTypes.stream().map(TerrainInfo::id).toList();
  }
}
