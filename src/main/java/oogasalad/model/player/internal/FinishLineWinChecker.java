package oogasalad.model.player.internal;

import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.player.api.WinConditionChecker;

/**
 * The player wins when the main character reaches the finish-line zone.
 *
 * @author Paulina Vvedenskaya
 */
public class FinishLineWinChecker implements WinConditionChecker {

  private static final double DEFAULT_MAIN_CHARACTER_SIZE = 40.0;

  private final String mainCharacterId;

  public FinishLineWinChecker() {
    this(null);
  }

  public FinishLineWinChecker(String mainCharacterId) {
    this.mainCharacterId = mainCharacterId;
  }

  @Override
  public boolean hasWon(World world) {

    for (GameObject mainCharacter : MainCharacterLookup.find(world, mainCharacterId)) {
      for (GameObject finish : world.getObjectsByTag(DefaultTag.FINISH_LINE.value())) {

        if (!(finish instanceof GoalLineComponent g)) {
          continue;
        }
        double fxL = g.x() - g.getWidth() / 2.0;
        double fyT = g.y() - g.getHeight() / 2.0;
        double fyB = g.y() + g.getHeight() / 2.0;

        double mainCharacterHalf = DEFAULT_MAIN_CHARACTER_SIZE / 2.0;
        double px = mainCharacter.x(), py = mainCharacter.y();

        boolean crossedHoriz =
            (px + mainCharacterHalf) >= fxL;
        boolean withinVert = (py + mainCharacterHalf) >= fyT
            && (py - mainCharacterHalf) <= fyB;
        if (crossedHoriz && withinVert) {
          return true;
        }
      }
    }
    return false;
  }
}

