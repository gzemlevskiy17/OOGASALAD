package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.model.engine.WorldImpl;
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

    TerrainSpline spline = world.getAllComponents().stream()
        .filter(TerrainSpline.class::isInstance)
        .map(TerrainSpline.class::cast)
        .findFirst()
        .orElseThrow();
    assertEquals("DirtPath", spline.getDisplayName());
    assertEquals("data/assets/back.png", spline.getSprite());
    assertTrue(spline.isTiled());
  }
}
