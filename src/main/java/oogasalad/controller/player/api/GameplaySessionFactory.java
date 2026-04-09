package oogasalad.controller.player.api;

import java.util.function.Consumer;
import oogasalad.controller.core.api.Outcome;
import oogasalad.controller.player.internal.GameplaySession;
import oogasalad.view.player.internal.GamePlayAppScreen;

// Define how a gameplay run gets assembled from the dependencies
public interface GameplaySessionFactory {
  GameplaySession create(GamePlayAppScreen screen, Consumer<Outcome> onOutcome);
}
