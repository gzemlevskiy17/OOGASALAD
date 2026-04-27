package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests game-catalog path resolution for level files.
 *
 * @author Emilio Garcia
 */
class LevelPathsTest {

  @Test
  void gameFileForLevel_badPiggiesWithAlternateCatalog_prefersGenerationCatalog() {
    assertEquals("data/games/badpiggies/ai-generation-catalog.json",
        LevelPaths.gameFileForLevel("data/levels/generated/generated-level.json", "badpiggies"));
  }

  @Test
  void gameFileForLevel_blankGameType_usesSiblingGameFile() {
    assertEquals("data/levels/example/game.json",
        LevelPaths.gameFileForLevel("data/levels/example/forest1.level.json", ""));
  }
}
