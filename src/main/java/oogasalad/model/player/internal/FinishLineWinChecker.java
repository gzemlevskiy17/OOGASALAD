package oogasalad.model.player.internal;

import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.player.api.WinConditionChecker;

/**
 * The player wins when the pig's horizontal center reaches the finish-line zone.
 *
 * @author Paulina Vvedenskaya
 */
public class FinishLineWinChecker implements WinConditionChecker {

  @Override
  public boolean hasWon(World world) {

    for (GameObject pig : world.getObjectsByTag(DefaultTag.PIGS.value())) {
      for (GameObject finish : world.getObjectsByTag(DefaultTag.FINISH_LINE.value())) {

        if (!(finish instanceof GoalLineComponent g)) continue;
        double fxL = g.getX() - g.getWidth()  / 2.0;
        double fxR = g.getX() + g.getWidth()  / 2.0;
        double fyT = g.getY() - g.getHeight() / 2.0;
        double fyB = g.getY() + g.getHeight() / 2.0;

        double pigHalf = 40.0 / 2.0; // PigComponent.DEFAULT_SIZE / 2
        double px = pig.getX(), py = pig.getY();

        boolean crossedHoriz = (px + pigHalf) >= fxL;       // pig's right edge reached finish's left edge
        boolean withinVert  = (py + pigHalf) >= fyT && (py - pigHalf) <= fyB;
        if (crossedHoriz && withinVert) return true;
      }
    }
    return false;
  }
}

