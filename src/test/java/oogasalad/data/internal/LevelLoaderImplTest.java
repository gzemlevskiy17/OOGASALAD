package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.api.LevelAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LevelLoaderImplTest {

  @TempDir
  Path tempDir;

  @Test
  void loadAttributes_readsBackgroundImageFromGameMetadata() throws IOException {
    Path levelFile = tempDir.resolve("level.json");
    Files.writeString(levelFile, """
        {
          "gameMetadata": {
            "levelName": "sky test",
            "gameType": "test",
            "screenWidth": 1000,
            "screenHeight": 800,
            "gravity": -9.8,
            "drag": 0.01,
            "backgroundImage": "data/assets/background2.png"
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
        """);

    LevelAttributes attributes = new LevelLoaderImpl().loadAttributes(levelFile.toString());

    assertEquals("data/assets/background2.png", attributes.getBackgroundImage());
  }
}
