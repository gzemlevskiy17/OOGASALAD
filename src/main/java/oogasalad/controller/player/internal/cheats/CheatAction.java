package oogasalad.controller.player.internal.cheats;

import java.util.List;
import oogasalad.controller.player.api.Action;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gameplay action that executes configured cheat effects in declaration order.
 *
 * @author Emilio Garcia
 */
public class CheatAction implements Action {

  private static final Logger LOG = LogManager.getLogger(CheatAction.class);

  private final List<CheatEffect> effects;
  private final CheatRuntimeContext context;

  public CheatAction(List<CheatEffect> effects, CheatRuntimeContext context) {
    this.effects = List.copyOf(effects);
    this.context = context;
  }

  @Override
  public void execute() {
    effects.forEach(effect -> effect.apply(context));
    LOG.debug("Executed cheat action with {} effect(s)", effects.size());
  }
}
