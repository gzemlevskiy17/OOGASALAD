package oogasalad.model.player.internal;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.player.api.WinConditionChecker;

/**
 * The player wins when the main character reaches the finish-line zone.
 *
 * @author Paulina Vvedenskaya
 */
public class FinishLineWinChecker implements WinConditionChecker {

  private static final double FALLBACK_MAIN_CHARACTER_SIZE = 40.0;

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

        double mainCharacterHalf = halfSizeOf(mainCharacter);
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

  private static double halfSizeOf(GameObject obj) {
    double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
    double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    for (ConvexPolygon poly : obj.getCollisionPolygons()) {
      for (Vector pt : poly.points()) {
        minX = Math.min(minX, pt.x()); maxX = Math.max(maxX, pt.x());
        minY = Math.min(minY, pt.y()); maxY = Math.max(maxY, pt.y());
      }
    }
    if (minX == Double.MAX_VALUE) {
      return FALLBACK_MAIN_CHARACTER_SIZE / 2.0;
    }
    return Math.max(maxX - minX, maxY - minY) / 2.0;
  }
}

