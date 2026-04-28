package oogasalad.controller.player.internal.cheats;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.input.KeyCode;

/**
 * Tracks currently pressed keys and fires matching cheat chords once per press cycle.
 *
 * @author Emilio Garcia
 */
public class CheatInputHandler {

  private final List<CheatKeyBinding> bindings;
  private final Set<KeyCode> pressedKeys = new HashSet<>();
  private final Set<CheatKeyBinding> activeBindings = new HashSet<>();

  public CheatInputHandler(List<CheatKeyBinding> bindings) {
    this.bindings = List.copyOf(bindings);
  }

  /**
   * Records a key press and executes any newly matched chord.
   */
  public void handleKeyPressed(KeyCode key) {
    pressedKeys.add(key);
    bindings.stream()
        .filter(binding -> binding.matches(pressedKeys))
        .filter(activeBindings::add)
        .forEach(binding -> binding.action().execute());
  }

  /**
   * Records a key release and allows its chord to fire again on a later press.
   */
  public void handleKeyReleased(KeyCode key) {
    pressedKeys.remove(key);
    activeBindings.removeIf(binding -> !binding.matches(pressedKeys));
  }

  /**
   * Clears all pressed-key state, useful when switching screens or pausing gameplay.
   */
  public void reset() {
    pressedKeys.clear();
    activeBindings.clear();
  }
}
