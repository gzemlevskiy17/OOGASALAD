package oogasalad.model.player.api;

import oogasalad.data.api.GameConfig;

/**
 * Resolves a component type to its {@link BuildRules}. Each type declares its rules once and the
 * validator reads through this registry — no component-type branching inside the validator.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 */
public interface BuildRulesRegistry {

  /**
   * Registers or overlays build rules for the selected game's component catalog.
   *
   * @param config selected game catalog
   * @author Emilio Garcia
   */
  default void registerFromConfig(GameConfig config) {
  }

  BuildRules lookup(String componentType);
}
