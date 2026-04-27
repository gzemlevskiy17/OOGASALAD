package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;
import oogasalad.model.authoring.api.GoodPiggiesCatalogException;
import oogasalad.support.TestGameConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests fixed Good Piggies catalog loading and validation.
 *
 * @author Emilio Garcia
 */
class GoodPiggiesCatalogProviderTest {

  @Test
  void LoadCatalog_ValidCatalogLoadedTwice_ReturnsCachedCatalog() {
    GoodPiggiesCatalogProvider provider =
        new GoodPiggiesCatalogProvider(fileName -> TestGameConfig.badPiggies());

    GoodPiggiesCatalog firstLoad = provider.loadCatalog();
    GoodPiggiesCatalog secondLoad = provider.loadCatalog();

    assertSame(firstLoad, secondLoad);
  }

  @Test
  void LoadCatalog_EmptyCatalogFields_UsesFallbackCatalogValues() {
    GoodPiggiesCatalogProvider provider = new GoodPiggiesCatalogProvider(fileName ->
        new TestGameConfig("badpiggies", "", List.of(), List.of(), List.of()));

    GoodPiggiesCatalog catalog = provider.loadCatalog();

    assertEquals("BigPig", catalog.primaryComponentId());
    assertEquals("GoalLine", catalog.staticComponentIds().getLast());
  }

  @Test
  void LoadCatalog_WrongGameId_ThrowsGoodPiggiesCatalogException() {
    GoodPiggiesCatalogProvider provider = new GoodPiggiesCatalogProvider(fileName ->
        new TestGameConfig("wronggame", "BigPig", List.of(), List.of(), List.of()));

    assertThrows(GoodPiggiesCatalogException.class, provider::loadCatalog);
  }
}
