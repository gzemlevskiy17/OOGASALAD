package oogasalad.model.player.api;

/**
 * Resolves a component type to its {@link BuildRules}. Each type declares its rules once and the
 * validator reads through this registry — no component-type branching inside the validator.
 *
 * @author Paulina Vvedenskaya
 */
public interface BuildRulesRegistry {

  BuildRules lookup(String componentType);
}
