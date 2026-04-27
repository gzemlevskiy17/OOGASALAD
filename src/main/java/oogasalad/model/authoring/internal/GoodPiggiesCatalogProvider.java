package oogasalad.model.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.data.internal.GameLoaderImpl;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;
import oogasalad.model.authoring.api.GoodPiggiesCatalogException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads and validates the fixed Good Piggies game catalog used by the AI level-generation
 * pipeline.
 *
 * <p>This provider centralizes the temporary "one concrete game" assumption so later schema-aware
 * generation steps can validate ids and build prompts without hardcoding catalog strings across
 * the codebase.
 *
 * @author Emilio Garcia
 */
public class GoodPiggiesCatalogProvider {

  private static final Logger LOG = LogManager.getLogger(GoodPiggiesCatalogProvider.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GoodPiggiesCatalog");

  private final GameLoader gameLoader;
  private GoodPiggiesCatalog cachedCatalog;

  /**
   * Creates a provider that loads the Good Piggies catalog using the default game loader.
   *
   * @author Emilio Garcia
   */
  public GoodPiggiesCatalogProvider() {
    this(new GameLoaderImpl());
  }

  /**
   * Creates a provider with an injected catalog loader.
   *
   * @param gameLoader loader used to parse the fixed Good Piggies game catalog
   * @author Emilio Garcia
   */
  public GoodPiggiesCatalogProvider(GameLoader gameLoader) {
    if (gameLoader == null) {
      String message = PROPS.getString("nullGameLoaderError");
      LOG.error(message);
      throw new GoodPiggiesCatalogException(message);
    }
    this.gameLoader = gameLoader;
  }

  /**
   * Loads and validates the fixed Good Piggies catalog once, then reuses the parsed immutable
   * model for later calls.
   *
   * @return validated Good Piggies catalog
   * @author Emilio Garcia
   */
  public GoodPiggiesCatalog loadCatalog() {
    if (cachedCatalog != null) {
      return cachedCatalog;
    }

    String catalogPath = PROPS.getString("catalogPath");
    try {
      GameConfig config = gameLoader.loadGame(catalogPath);
      validateConfig(config, catalogPath);
      cachedCatalog = hydrateCatalog(config, catalogPath);
      LOG.info(PROPS.getString("catalogLoadSuccessLogMessage"), catalogPath);
      return cachedCatalog;
    } catch (RuntimeException exception) {
      String message = PROPS.getString("catalogLoadFailureError");
      LOG.error(message, exception);
      throw new GoodPiggiesCatalogException(message, exception);
    }
  }

  /**
   * Loads and returns the cached Good Piggies catalog.
   *
   * <p>This alias keeps newer generation-pipeline collaborators readable while preserving the
   * original explicit {@link #loadCatalog()} API.
   *
   * @return validated Good Piggies catalog
   * @author Emilio Garcia
   */
  public GoodPiggiesCatalog load() {
    return loadCatalog();
  }

  private void validateConfig(GameConfig config, String catalogPath) {
    String expectedGameId = PROPS.getString("expectedGameId");
    if (config == null) {
      String message = PROPS.getString("nullCatalogError");
      LOG.error(message);
      throw new GoodPiggiesCatalogException(message);
    }
    if (!expectedGameId.equals(config.gameId())) {
      String message = String.format(PROPS.getString("unexpectedGameIdError"),
          expectedGameId, catalogPath, config.gameId());
      LOG.error(message);
      throw new GoodPiggiesCatalogException(message);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private GoodPiggiesCatalog hydrateCatalog(GameConfig config, String catalogPath) {
    boolean usesFallback = isBlank(config.primaryComponentId())
        || config.components().isEmpty()
        || config.staticComponents().isEmpty()
        || config.terrainTypes().isEmpty();
    if (usesFallback) {
      LOG.warn(PROPS.getString("catalogFallbackLogMessage"), catalogPath);
    }

    String primaryComponentId = usesFallback && isBlank(config.primaryComponentId())
        ? PROPS.getString("fallbackPrimaryComponentId")
        : config.primaryComponentId();
    List<ComponentInfo> components = config.components().isEmpty()
        ? buildFallbackComponents()
        : config.components();
    List<StaticComponentInfo> staticComponents = config.staticComponents().isEmpty()
        ? buildFallbackStaticComponents()
        : config.staticComponents();
    List<TerrainInfo> terrainTypes = config.terrainTypes().isEmpty()
        ? buildFallbackTerrainTypes()
        : config.terrainTypes();

    return new GoodPiggiesCatalog(
        config.gameId(),
        primaryComponentId,
        components,
        staticComponents,
        terrainTypes);
  }

  private List<ComponentInfo> buildFallbackComponents() {
    List<ComponentInfo> components = new ArrayList<>();
    for (String id : csv("fallbackComponentIds")) {
      components.add(new ComponentInfo(id, "", 0.0, List.of()));
    }
    return components;
  }

  private List<StaticComponentInfo> buildFallbackStaticComponents() {
    List<String> ids = csv("fallbackStaticComponentIds");
    List<String> actionTypes = csv("fallbackCollisionTypes");
    List<Double> actionAmounts = csvDoubles("fallbackCollisionAmounts");
    List<StaticComponentInfo> staticComponents = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
      staticComponents.add(new StaticComponentInfo(
          ids.get(i),
          "",
          0.0,
          new CollisionAction(actionTypes.get(i), actionAmounts.get(i))));
    }
    return staticComponents;
  }

  private List<TerrainInfo> buildFallbackTerrainTypes() {
    List<String> ids = csv("fallbackTerrainIds");
    List<String> colors = csv("fallbackTerrainColors");
    List<Double> frictions = csvDoubles("fallbackTerrainFrictions");
    List<TerrainInfo> terrainTypes = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
      terrainTypes.add(new TerrainInfo(ids.get(i), "", colors.get(i), frictions.get(i)));
    }
    return terrainTypes;
  }

  private List<String> csv(String key) {
    return List.of(PROPS.getString(key).split("\\s*,\\s*"));
  }

  private List<Double> csvDoubles(String key) {
    return csv(key).stream().map(Double::parseDouble).toList();
  }
}
