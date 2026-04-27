package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import oogasalad.data.api.GameConfig;
import org.junit.jupiter.api.Test;

class GameplayConfigSettingsTest {

  @Test
  void controllerSettings_withConfig_returnsConfig() {
    GameConfig config = new FakeGameConfig();

    assertSame(config, new GameplayConfigSettings(config).controllerSettings());
  }

  @Test
  void mainCharacterTag_readsCanonicalSetting() {
    FakeGameConfig config = new FakeGameConfig();
    config.mainCharacterTag = "dinosaur";

    assertEquals("dinosaur", new GameplayConfigSettings(config).mainCharacterTag());
  }

  @Test
  void lossTrackedTags_splitsCommaSeparatedSetting() {
    FakeGameConfig config = new FakeGameConfig();
    config.lossTrackedTags = "dinosaur, vehicle";

    assertEquals(List.of("dinosaur", "vehicle"),
        new GameplayConfigSettings(config).lossTrackedTags());
  }

  @Test
  void missingConfig_returnsDefaults() {
    GameplayConfigSettings settings = new GameplayConfigSettings(null);

    assertNull(settings.mainCharacterTag());
    assertTrue(settings.lossTrackedTags().isEmpty());
  }

  private static final class FakeGameConfig implements GameConfig {
    String mainCharacterTag;
    String lossTrackedTags;

    @Override public String gameId() { return ""; }
    @Override public String primaryComponentId() { return ""; }
    @Override public List<oogasalad.data.api.ComponentInfo> components() { return List.of(); }
    @Override public List<oogasalad.data.api.StaticComponentInfo> staticComponents() {
      return List.of();
    }
    @Override public List<oogasalad.data.api.TerrainInfo> terrainTypes() { return List.of(); }

    @Override
    public String getSetting(String key) {
      return switch (key) {
        case GameplayConfigSettings.MAIN_CHARACTER_TAG_KEY -> mainCharacterTag;
        case GameplayConfigSettings.LOSS_TRACKED_TAGS_KEY -> lossTrackedTags;
        default -> null;
      };
    }

    @Override public double getNumericSetting(String key) { return 0; }
  }
}
