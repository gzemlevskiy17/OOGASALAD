package oogasalad.model.authoring.internal;

import java.util.List;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;
import oogasalad.support.TestGameConfig;

/**
 * Shared in-memory generated-level fixtures.
 * @author Emilio Garcia
 */
final class GeneratedLevelFixtures {

  private GeneratedLevelFixtures() {
  }

  static GoodPiggiesCatalog catalog() {
    TestGameConfig config = TestGameConfig.badPiggies();
    return new GoodPiggiesCatalog(
        config.gameId(),
        config.mainCharacterId(),
        config.components(),
        config.staticComponents(),
        config.terrainTypes());
  }

  static GeneratedLevelSpec validSpec() {
    return new GeneratedLevelSpec(
        new GeneratedLevelSpec.GameMetadataSpec(
            "Hill Run", "badpiggies", 3200.0, 1600.0, -9.8, 0.01),
        new GeneratedLevelSpec.MapSpec(
            List.of(
                new GeneratedLevelSpec.PointSpec(0.0, 1100.0),
                new GeneratedLevelSpec.PointSpec(900.0, 1050.0),
                new GeneratedLevelSpec.PointSpec(1800.0, 1100.0)),
            new GeneratedLevelSpec.PointSpec(300.0, 900.0),
            List.of(
                new GeneratedLevelSpec.MapObjectSpec(
                    "GoalLine", new GeneratedLevelSpec.PointSpec(2500.0, 900.0)),
                new GeneratedLevelSpec.MapObjectSpec(
                    "Coin", new GeneratedLevelSpec.PointSpec(1200.0, 850.0))),
            List.of("GrassHills")),
        new GeneratedLevelSpec.MainCharacterSpec(
            60.0, List.of("Frame", "BigPig"), 5, 100.0, "vehicle"));
  }

  static GoodPiggiesCatalogProvider provider(GoodPiggiesCatalog catalog) {
    return new StubCatalogProvider(catalog);
  }

  private static final class StubCatalogProvider extends GoodPiggiesCatalogProvider {

    private final GoodPiggiesCatalog catalog;

    private StubCatalogProvider(GoodPiggiesCatalog catalog) {
      super(fileName -> TestGameConfig.badPiggies());
      this.catalog = catalog;
    }

    @Override
    public GoodPiggiesCatalog loadCatalog() {
      return catalog;
    }

    @Override
    public GoodPiggiesCatalog load() {
      return catalog;
    }
  }
}
