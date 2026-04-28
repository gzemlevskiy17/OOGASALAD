package oogasalad.controller.player.internal.cheats;

import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.World;
import oogasalad.model.player.api.ScoreService;

/**
 * Runtime dependencies that cheat effects are allowed to mutate or notify.
 *
 * @author Emilio Garcia
 */
public record CheatRuntimeContext(
    World world,
    GameConfig gameConfig,
    String mainCharacterId,
    Consumer<Outcome> terminalOutcome,
    ScoreService scoreService) {

}
