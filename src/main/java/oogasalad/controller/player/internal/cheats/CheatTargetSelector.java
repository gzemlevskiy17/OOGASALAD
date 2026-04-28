package oogasalad.controller.player.internal.cheats;

import java.util.List;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.model.engine.api.GameObject;

/**
 * Resolves a configured target selector to concrete world objects.
 *
 * @author Emilio Garcia
 */
@FunctionalInterface
public interface CheatTargetSelector {

  List<GameObject> resolve(CheatTargetInfo target, CheatRuntimeContext context);
}
