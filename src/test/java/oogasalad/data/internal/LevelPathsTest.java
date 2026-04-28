package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests game-catalog path resolution for level files.
 * @author Emilio Garcia
 */
class LevelPathsTest {

  @Test
  void GameFileForLevel_BadPiggiesWithAlternateCatalog_PrefersGenerationCatalog() {
    assertEquals("data/games/badpiggies/ai-generation-catalog.json",
        LevelPaths.gameFileForLevel("data/levels/generated/generated-level.json", "badpiggies"));
  }

  @Test
  void GameFileForLevel_BlankGameType_UsesSiblingGameFile() {
    assertEquals("data/levels/example/game.json",
        LevelPaths.gameFileForLevel("data/levels/example/forest1.level.json", ""));
  }

  @Test
  void SiblingGameFile_LevelPathWithoutParent_ReturnsDefaultGameFileName() {
    assertEquals("game.json", LevelPaths.siblingGameFile("level.json"));
  }
}
