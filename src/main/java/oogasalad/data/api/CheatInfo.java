package oogasalad.data.api;

import java.util.List;

/**
 * Author-defined cheat key available to a game catalog.
 *
 * <p>Cheats are data-driven: authors define the trigger and ordered effect specs, while gameplay
 * runtime code resolves each effect type through a registry of supported primitive effects.
 *
 * @param id      unique cheat identifier within a game catalog
 * @param name    display name for authoring/debug UI
 * @param enabled whether gameplay should bind this cheat
 * @param trigger key trigger used to activate the cheat
 * @param effects ordered primitive effect specs executed when the cheat fires
 * @author Emilio Garcia
 */
public record CheatInfo(
    String id,
    String name,
    boolean enabled,
    CheatTriggerInfo trigger,
    List<CheatEffectInfo> effects) {

  public CheatInfo {
    effects = List.copyOf(effects);
  }
}
