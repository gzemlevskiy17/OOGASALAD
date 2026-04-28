package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.api.LevelLoadException;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LevelLoaderTerrainSplineTilingTest {

  @TempDir
  Path tempDir;

  @Test
  void loadLevel_terrainSplineFromCatalogAlwaysTilesSprite() throws IOException {
    Path gameFile = tempDir.resolve("game.json");
    Files.writeString(gameFile, """
        {
          "gameId": "test",
          "mainCharacterId": "Pig",
          "components": [],
          "staticComponents": [],
          "terrainTypes": [
            {
              "id": "DirtPath",
              "spriteUrl": "data/assets/back.png",
              "visualColor": "#8B5E3C",
              "friction": 0.4
            }
          ]
        }
        """);
    Path levelFile = tempDir.resolve("level.json");
    Files.writeString(levelFile, """
        {
          "gameMetadata": {
            "levelName": "test",
            "gameType": "test",
            "screenWidth": 1000,
            "screenHeight": 800,
            "gravity": -9.8,
            "drag": 0.01
          },
          "map": {
            "knotLocations": [
              {"x": 0, "y": 400},
              {"x": 500, "y": 420}
            ],
            "objects": [],
            "terrainOptions": ["DirtPath"],
            "spawnPoint": {"x": 0, "y": 0}
          },
          "mainCharacter": {
            "gridSize": 60,
            "elementsAllowed": [],
            "maxSelectable": 5,
            "maxHP": 100,
            "cameraFollows": "vehicle"
          }
        }
        """);
    WorldImpl world = new WorldImpl();

    new LevelLoaderImpl().loadLevel(levelFile.toString(), new GameConfigImpl(gameFile.toString()),
        world);

    TerrainSpline spline = firstTerrainSpline(world);
    assertEquals("DirtPath", spline.getDisplayName());
    assertEquals("data/assets/back.png", spline.getSprite());
    assertTrue(spline.isTiled());
  }

  @Test
  void loadLevel_malformedLevelMissingSpawnCoordinate_throws() throws IOException {
    Path levelFile = tempDir.resolve("bad-level.json");
    Files.writeString(levelFile, """
        {
          "gameMetadata": {
            "levelName": "bad",
            "gameType": "test",
            "screenWidth": 1000,
            "screenHeight": 800,
            "gravity": -9.8,
            "drag": 0.01
          },
          "map": {
            "knotLocations": [],
            "objects": [],
            "spawnPoint": {"x": 0}
          },
          "mainCharacter": {
            "gridSize": 60,
            "elementsAllowed": [],
            "maxSelectable": 5,
            "maxHP": 100
          }
        }
        """);

    LevelLoadException exception = assertThrows(LevelLoadException.class,
        () -> new LevelLoaderImpl().loadLevel(levelFile.toString(), GameConfigImpl.empty(),
            new WorldImpl()));
    assertNotNull(exception);
  }

  @Test
  void loadLevel_objectsWithoutGameConfig_throws() throws IOException {
    Path levelFile = tempDir.resolve("objects-without-game.json");
    Files.writeString(levelFile, levelWithOneObject());

    LevelLoadException exception = assertThrows(LevelLoadException.class,
        () -> new LevelLoaderImpl().loadLevel(levelFile.toString(), null, new WorldImpl()));
    assertNotNull(exception);
  }

  @Test
  void loadLevel_unknownCollisionAction_throws() throws IOException {
    Path gameFile = tempDir.resolve("unknown-action-game.json");
    Files.writeString(gameFile, """
        {
          "gameId": "test",
          "mainCharacterId": "Pig",
          "components": [],
          "staticComponents": [
            {
              "id": "mystery",
              "spriteUrl": "mystery.png",
              "mass": 0,
              "onCollisionAction": { "type": "teleport", "amount": 1 }
            }
          ],
          "terrainTypes": []
        }
        """);
    Path levelFile = tempDir.resolve("unknown-action-level.json");
    Files.writeString(levelFile, levelWithOneObject());

    LevelLoadException exception = assertThrows(LevelLoadException.class,
        () -> new LevelLoaderImpl().loadLevel(levelFile.toString(),
            new GameConfigImpl(gameFile.toString()), new WorldImpl()));
    assertNotNull(exception);
  }

  @Test
  void loadLevel_levelGameTypeDifferentFromCatalog_throws() throws IOException {
    Path gameFile = tempDir.resolve("wrong-game.json");
    Files.writeString(gameFile, """
        {
          "gameId": "different",
          "mainCharacterId": "Pig",
          "components": [],
          "staticComponents": [],
          "terrainTypes": [
            {
              "id": "known-terrain",
              "spriteUrl": null,
              "visualColor": "#00FF00",
              "friction": 0.5
            }
          ]
        }
        """);
    Path levelFile = tempDir.resolve("game-mismatch-level.json");
    Files.writeString(levelFile, validEmptyLevel());

    LevelLoadException exception = assertThrows(LevelLoadException.class,
        () -> new LevelLoaderImpl().loadLevel(levelFile.toString(),
            new GameConfigImpl(gameFile.toString()), new WorldImpl()));
    assertNotNull(exception);
  }

  @Test
  void loadLevel_objectTypeMissingFromCatalog_throws() throws IOException {
    Path gameFile = tempDir.resolve("missing-object-game.json");
    Files.writeString(gameFile, """
        {
          "gameId": "test",
          "mainCharacterId": "Pig",
          "components": [],
          "staticComponents": [],
          "terrainTypes": []
        }
        """);
    Path levelFile = tempDir.resolve("unknown-object-level.json");
    Files.writeString(levelFile, levelWithOneObject());

    LevelLoadException exception = assertThrows(LevelLoadException.class,
        () -> new LevelLoaderImpl().loadLevel(levelFile.toString(),
            new GameConfigImpl(gameFile.toString()), new WorldImpl()));
    assertNotNull(exception);
  }

  @Test
  void loadLevel_legacyTerrainOptionWithEmptyCatalog_throws() throws IOException {
    Path gameFile = tempDir.resolve("missing-terrain-game.json");
    Files.writeString(gameFile, """
        {
          "gameId": "test",
          "mainCharacterId": "Pig",
          "components": [],
          "staticComponents": [],
          "terrainTypes": []
        }
        """);
    Path levelFile = tempDir.resolve("unknown-terrain-level.json");
    Files.writeString(levelFile, levelWithUnknownTerrain());

    LevelLoadException exception = assertThrows(LevelLoadException.class,
        () -> new LevelLoaderImpl().loadLevel(levelFile.toString(),
            new GameConfigImpl(gameFile.toString()), new WorldImpl()));
    assertNotNull(exception);
  }

  private TerrainSpline firstTerrainSpline(WorldImpl world) {
    for (GameComponent component : world.getAllComponents()) {
      if (component instanceof TerrainSpline terrainSpline) {
        return terrainSpline;
      }
    }
    throw new IllegalStateException("Missing terrain spline");
  }

  private String levelWithOneObject() {
    return """
        {
          "gameMetadata": {
            "levelName": "test",
            "gameType": "test",
            "screenWidth": 1000,
            "screenHeight": 800,
            "gravity": -9.8,
            "drag": 0.01
          },
          "map": {
            "knotLocations": [],
            "objects": [
              { "type": "mystery", "position": {"x": 20, "y": 30} }
            ],
            "spawnPoint": {"x": 0, "y": 0}
          },
          "mainCharacter": {
            "gridSize": 60,
            "elementsAllowed": [],
            "maxSelectable": 5,
            "maxHP": 100
          }
        }
        """;
  }

  private String validEmptyLevel() {
    return """
        {
          "gameMetadata": {
            "levelName": "test",
            "gameType": "test",
            "screenWidth": 1000,
            "screenHeight": 800,
            "gravity": -9.8,
            "drag": 0.01
          },
          "map": {
            "knotLocations": [],
            "objects": [],
            "spawnPoint": {"x": 0, "y": 0}
          },
          "mainCharacter": {
            "gridSize": 60,
            "elementsAllowed": [],
            "maxSelectable": 5,
            "maxHP": 100
          }
        }
        """;
  }

  private String levelWithUnknownTerrain() {
    return """
        {
          "gameMetadata": {
            "levelName": "test",
            "gameType": "test",
            "screenWidth": 1000,
            "screenHeight": 800,
            "gravity": -9.8,
            "drag": 0.01
          },
          "map": {
            "knotLocations": [
              {"x": 0, "y": 400},
              {"x": 500, "y": 420}
            ],
            "objects": [],
            "terrainOptions": ["missing-terrain"],
            "spawnPoint": {"x": 0, "y": 0}
          },
          "mainCharacter": {
            "gridSize": 60,
            "elementsAllowed": [],
            "maxSelectable": 5,
            "maxHP": 100
          }
        }
        """;
  }
}
