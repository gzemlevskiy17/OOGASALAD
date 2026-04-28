package oogasalad.controller.player.internal.cheats;

import java.util.Set;
import javafx.scene.input.KeyCode;
import oogasalad.controller.player.api.Action;

/**
 * Runtime binding between a simultaneous key chord and a gameplay action.
 *
 * @author Emilio Garcia
 */
public record CheatKeyBinding(Set<KeyCode> keys, Action action) {

  public CheatKeyBinding {
    keys = Set.copyOf(keys);
  }

  boolean matches(Set<KeyCode> pressedKeys) {
    return pressedKeys.containsAll(keys);
  }
}
