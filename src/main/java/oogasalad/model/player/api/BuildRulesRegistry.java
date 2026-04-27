package oogasalad.model.player.api;

import oogasalad.data.api.GameConfig;

/**
 * Resolves a component type to its {@link BuildRules}. Each type declares its rules once and the
 * validator reads through this registry — no component-type branching inside the validator.
 *
 * @author Paulina Vvedenskaya
 */
public interface BuildRulesRegistry {

  BuildRules lookup(String componentType);

  /**
   * Registers placement rules for component ids loaded from a game catalog.
   *
   * @param config game catalog whose component ids should be buildable
   */
  default void registerFromConfig(GameConfig config) {
  }
}
