package oogasalad.model.player.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.WorldImpl;
import oogasalad.model.engine.internal.components.GoalLineComponent;
import oogasalad.model.engine.internal.components.StaticComponent;
import org.junit.jupiter.api.Test;

class FinishLineWinCheckerTest {

  @Test
  void hasWon_configuredMainCharacterTag_returnsTrueWhenMainCharacterReachesFinish() {
    WorldImpl world = new WorldImpl();
    world.addComponent(new TaggedBody("hero", 90, 0));
    GoalLineComponent goal = new GoalLineComponent(100, 0, 20, 80);
    world.addComponent(goal);

    assertTrue(new FinishLineWinChecker("hero").hasWon(world));
  }

  @Test
  void hasWon_configuredMainCharacterTagDoesNotUsePigs() {
    WorldImpl world = new WorldImpl();
    world.addComponent(new TaggedBody("pigs", 90, 0));
    GoalLineComponent goal = new GoalLineComponent(100, 0, 20, 80);
    world.addComponent(goal);

    assertFalse(new FinishLineWinChecker("hero").hasWon(world));
  }

  private static final class TaggedBody extends StaticComponent {
    TaggedBody(String tag, double x, double y) {
      super(x, y, 0, ConvexPolygon.axisAlignedBox(x - 20, y - 20, 40, 40), tag, 0, 0);
    }
  }
}
