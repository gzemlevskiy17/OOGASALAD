package oogasalad.controller.player.internal.cheats;

/**
 * Runtime executable form of a configured cheat primitive.
 *
 * @author Emilio Garcia
 */
@FunctionalInterface
public interface CheatEffect {

  void apply(CheatRuntimeContext context);
}
