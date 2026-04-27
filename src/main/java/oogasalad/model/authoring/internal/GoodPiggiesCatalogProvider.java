package oogasalad.model.authoring.internal;

import java.util.ResourceBundle;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;
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
      cachedCatalog = new GoodPiggiesCatalog(
          config.gameId(),
          config.primaryComponentId(),
          config.components(),
          config.staticComponents(),
          config.terrainTypes());
      LOG.info(PROPS.getString("catalogLoadSuccessLogMessage"), catalogPath);
      return cachedCatalog;
    } catch (RuntimeException exception) {
      String message = PROPS.getString("catalogLoadFailureError");
      LOG.error(message, exception);
      throw new GoodPiggiesCatalogException(message, exception);
    }
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
    if (isBlank(config.primaryComponentId())) {
      String message = PROPS.getString("missingPrimaryComponentError");
      LOG.error(message);
      throw new GoodPiggiesCatalogException(message);
    }
    if (config.components().isEmpty()) {
      String message = PROPS.getString("emptyComponentsError");
      LOG.error(message);
      throw new GoodPiggiesCatalogException(message);
    }
    if (config.staticComponents().isEmpty()) {
      String message = PROPS.getString("emptyStaticComponentsError");
      LOG.error(message);
      throw new GoodPiggiesCatalogException(message);
    }
    if (config.terrainTypes().isEmpty()) {
      String message = PROPS.getString("emptyTerrainTypesError");
      LOG.error(message);
      throw new GoodPiggiesCatalogException(message);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
