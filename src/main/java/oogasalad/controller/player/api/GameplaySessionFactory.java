package oogasalad.controller.player.api;

import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.internal.GameplaySession;
import oogasalad.model.player.api.VehicleLayout;
import oogasalad.view.player.internal.GamePlayAppScreen;

/**
 * Factory contract for creating one gameplay session bound to a gameplay screen.
 *
 * @author Iurii Beliaev
 * @author Paulina Vvedenskaya
 */
public interface GameplaySessionFactory {

  /**
   * Creates a gameplay session for the given screen instance.
   *
   * @param screen gameplay view used by renderer/camera integration
   * @param onOutcome callback for terminal gameplay outcomes
   * @return a new gameplay session
   */
  GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome,
      VehicleLayout layout);
}
