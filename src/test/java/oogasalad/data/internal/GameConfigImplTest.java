package oogasalad.data.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import oogasalad.data.api.LevelLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GameConfigImplTest {

  @TempDir
  Path tempDir;

  private File writeConfig(String json) throws IOException {
    File file = tempDir.resolve("game_config.json").toFile();
    Files.writeString(file.toPath(), json);
    return file;
  }

  @Test
  void getSetting_topLevelKey_returnsValue() throws IOException {
    File config = writeConfig("{\"game_type\": \"bad_piggies\"}");
    GameConfigImpl gc = new GameConfigImpl(config.getAbsolutePath());

    assertEquals("bad_piggies", gc.getSetting("game_type"));
  }

  @Test
  void getSetting_nestedDotPath_returnsValue() throws IOException {
    File config = writeConfig("{\"camera\": {\"mode\": \"follow_player\"}}");
    GameConfigImpl gc = new GameConfigImpl(config.getAbsolutePath());

    assertEquals("follow_player", gc.getSetting("camera.mode"));
  }

  @Test
  void getSetting_missingKey_returnsNull() throws IOException {
    File config = writeConfig("{\"game_type\": \"bad_piggies\"}");
    GameConfigImpl gc = new GameConfigImpl(config.getAbsolutePath());

    assertNull(gc.getSetting("nonexistent_key"));
  }

  @Test
  void getNumericSetting_validKey_returnsDouble() throws IOException {
    File config = writeConfig("{\"camera\": {\"zoom\": 1.5}}");
    GameConfigImpl gc = new GameConfigImpl(config.getAbsolutePath());

    assertEquals(1.5, gc.getNumericSetting("camera.zoom"), 0.001);
  }

  @Test
  void getNumericSetting_missingKey_throwsIllegalArgument() throws IOException {
    File config = writeConfig("{\"game_type\": \"bad_piggies\"}");
    GameConfigImpl gc = new GameConfigImpl(config.getAbsolutePath());

    assertThrows(IllegalArgumentException.class,
        () -> gc.getNumericSetting("missing.key"));
  }

  @Test
  void constructor_nonExistentFile_throwsLevelLoadException() {
    assertThrows(LevelLoadException.class,
        () -> new GameConfigImpl("does_not_exist.json"));
  }
}
