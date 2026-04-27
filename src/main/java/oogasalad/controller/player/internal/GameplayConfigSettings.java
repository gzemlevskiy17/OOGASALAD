package oogasalad.controller.player.internal;

import java.util.Arrays;
import java.util.List;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.Settings;

/**
 * Provides typed access to the gameplay settings stored in {@link GameConfig}.
 *
 * <p>This class is the boundary between JSON/dot-path configuration keys and the player runtime.
 * The session factory asks this object for domain concepts, such as the main character tag, instead
 * of reading raw setting strings directly.
 *
 * @author Paulina Vvedenskaya
 */
final class GameplayConfigSettings {

  static final String MAIN_CHARACTER_TAG_KEY = "win_condition.mainCharacterTag";
  static final String LOSS_TRACKED_TAGS_KEY = "loss_conditions.trackedTags";

  private static final Settings EMPTY_SETTINGS = new Settings() {
    @Override
    public String getSetting(String key) {
      return "";
    }

    @Override
    public double getNumericSetting(String key) {
      return 0.0;
    }
  };

  private final GameConfig config;

  /**
   * Creates a settings view for an optional loaded game config.
   *
   * @param config loaded game config, or null when the level has no catalog
   */
  GameplayConfigSettings(GameConfig config) {
    this.config = config;
  }

  /**
   * Returns the settings object used by {@link GameControllerImpl} for input mappings.
   *
   * @return loaded config when present, otherwise an empty settings object
   */
  Settings controllerSettings() {
    return config != null ? config : EMPTY_SETTINGS;
  }

  /**
   * Reads which tag represents the playable main character for win/progress rules.
   *
   * @return configured main character tag, or null to use checker defaults
   */
  String mainCharacterTag() {
    return setting(MAIN_CHARACTER_TAG_KEY);
  }

  /**
   * Reads which object tags should be watched for fall-off-world loss.
   *
   * @return tracked tags from config, or an empty list to use loss-checker defaults
   */
  List<String> lossTrackedTags() {
    String raw = setting(LOSS_TRACKED_TAGS_KEY);
    if (raw == null) {
      return List.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(tag -> !tag.isEmpty())
        .toList();
  }

  private String setting(String key) {
    if (config == null) {
      return null;
    }
    String value = config.getSetting(key);
    return value == null || value.isBlank() ? null : value;
  }
}
