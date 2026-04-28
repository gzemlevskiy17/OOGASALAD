package oogasalad.controller.player.internal.cheats;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.scene.input.KeyCode;
import oogasalad.controller.player.api.Action;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.GameConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Converts author-defined cheat data from a game catalog into runtime key bindings.
 *
 * @author Emilio Garcia
 */
public class CheatKeyBinder {

  private static final Logger LOG = LogManager.getLogger(CheatKeyBinder.class);

  private final CheatEffectRegistry effectRegistry;

  public CheatKeyBinder(CheatEffectRegistry effectRegistry) {
    this.effectRegistry = effectRegistry;
  }

  /**
   * Creates the default binder for gameplay.
   */
  public static CheatKeyBinder defaults() {
    return new CheatKeyBinder(CheatEffectRegistry.defaults());
  }

  /**
   * Builds a handler containing all enabled cheat key bindings for the configured game.
   */
  public CheatInputHandler bind(GameConfig gameConfig, CheatRuntimeContext context) {
    List<CheatKeyBinding> bindings = gameConfig.cheats().stream()
        .filter(CheatInfo::enabled)
        .map(cheat -> bindingFor(cheat, context))
        .toList();
    LOG.info("Bound {} cheat key chord(s) for game '{}'", bindings.size(), gameConfig.gameId());
    return new CheatInputHandler(bindings);
  }

  private CheatKeyBinding bindingFor(CheatInfo cheat, CheatRuntimeContext context) {
    Set<KeyCode> keys = keysFor(cheat);
    Action action = new CheatAction(
        cheat.effects().stream().map(effectRegistry::create).toList(),
        context);
    return new CheatKeyBinding(keys, action);
  }

  private Set<KeyCode> keysFor(CheatInfo cheat) {
    EnumSet<KeyCode> keys = EnumSet.noneOf(KeyCode.class);
    cheat.trigger().keys().stream()
        .map(this::parseKey)
        .forEach(keys::add);
    if (keys.isEmpty()) {
      throw new CheatConfigurationException(
          "Cheat '" + cheat.id() + "' must define at least one trigger key");
    }
    return Set.copyOf(keys);
  }

  private KeyCode parseKey(String keyName) {
    String normalized = keyName.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    try {
      return KeyCode.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new CheatConfigurationException("Invalid cheat key: " + keyName, e);
    }
  }
}
