package oogasalad.controller.player.internal.cheats;

import oogasalad.data.api.CheatEffectInfo;

/**
 * Converts author-configured cheat effect data into executable runtime behavior.
 *
 * @author Emilio Garcia
 */
@FunctionalInterface
public interface CheatEffectFactory {

  CheatEffect create(CheatEffectInfo info, CheatTargetResolver targetResolver);
}
