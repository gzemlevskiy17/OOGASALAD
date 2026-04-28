package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests game-catalog path resolution for level files.
 * @author Emilio Garcia
 */
class LevelPathsTest {

  @TempDir
  Path tempDir;

  @Test
  void GameFileForLevel_GameJsonWithCatalogContent_PrefersAuthoredCatalog() throws IOException {
    Path gamesDir = tempDir.resolve("games");
    Path gameDir = gamesDir.resolve("badpiggies");
    Files.createDirectories(gameDir);
    Files.writeString(gameDir.resolve(LevelPaths.GAME_FILE_NAME), """
        {
          "gameId": "badpiggies",
          "mainCharacterId": "BigPig",
          "components": [
            { "id": "BigPig", "spriteUrl": "pig.png", "mass": 1.0, "buttons": [] }
          ],
          "staticComponents": [],
          "terrainTypes": [],
          "cheats": [
            {
              "id": "finish",
              "name": "Finish",
              "enabled": true,
              "trigger": { "keys": ["F"] },
              "effects": [{ "type": "finishRun" }]
            }
          ]
        }
        """);
    Files.writeString(gameDir.resolve(LevelPaths.AI_GENERATION_CATALOG_FILE_NAME), """
        { "gameId": "badpiggies", "mainCharacterId": "BigPig",
          "components": [], "staticComponents": [], "terrainTypes": [] }
        """);

    assertEquals(gameDir.resolve(LevelPaths.GAME_FILE_NAME).toString(),
        LevelPaths.gameFileForLevel("data/levels/badpiggies/level1.json", "badpiggies",
            gamesDir));
  }

  @Test
  void GameFileForLevel_EmptyGameJsonWithAlternateCatalog_UsesGenerationCatalog()
      throws IOException {
    Path gamesDir = tempDir.resolve("games");
    Path gameDir = gamesDir.resolve("badpiggies");
    Files.createDirectories(gameDir);
    Files.writeString(gameDir.resolve(LevelPaths.GAME_FILE_NAME), """
        { "gameId": "badpiggies", "mainCharacterId": "BigPig",
          "components": [], "staticComponents": [], "terrainTypes": [] }
        """);
    Files.writeString(gameDir.resolve(LevelPaths.AI_GENERATION_CATALOG_FILE_NAME), """
        { "gameId": "badpiggies", "mainCharacterId": "BigPig",
          "components": [
            { "id": "BigPig", "spriteUrl": "pig.png", "mass": 1.0, "buttons": [] }
          ],
          "staticComponents": [], "terrainTypes": [] }
        """);

    assertEquals(gameDir.resolve(LevelPaths.AI_GENERATION_CATALOG_FILE_NAME).toString(),
        LevelPaths.gameFileForLevel("data/levels/generated/generated-level.json", "badpiggies",
            gamesDir));
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
