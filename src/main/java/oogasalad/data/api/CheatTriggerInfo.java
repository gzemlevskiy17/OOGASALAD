package oogasalad.data.api;

import java.util.List;

/**
 * Key trigger for an author-defined cheat.
 *
 * <p>The first runtime implementation can interpret {@code keys} as a simultaneous key chord
 * such as {@code ["SHIFT", "H"]}. Keeping this separate from {@link CheatInfo} leaves room for
 * future trigger modes such as sequences without changing cheat effects.
 *
 * @param keys JavaFX {@code KeyCode} names that activate the cheat
 * @author Emilio Garcia
 */
public record CheatTriggerInfo(List<String> keys) {

  public CheatTriggerInfo {
    keys = List.copyOf(keys);
  }
}
