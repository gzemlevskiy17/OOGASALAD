package oogasalad.controller.authoring.internal;

import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Immutable snapshot of a single canvas modification (block placed or removed).
 */
public record CanvasAction(ActionType type, int col, int row,
    double width, double height, String color, String sprite,
                           boolean tiled, double friction, double restitution, String catalogName) {

  /** Whether the action added or removed a block. */
  public enum ActionType { PLACE, REMOVE }

  /** Creates a PLACE action from the given placeable's current state. */
  public static CanvasAction place(AuthoringPlaceable p) {
    return new CanvasAction(ActionType.PLACE,
        (int) p.x(), (int) p.y(),
        p.getWidth(), p.getHeight(),
        p.getColor(), p.getSprite(),
        p.isTiled(), p.getFriction(), p.getRestitution(), p.getCatalogName());
  }

  /** Creates a REMOVE action from the given placeable's current state. */
  public static CanvasAction remove(AuthoringPlaceable p) {
    return new CanvasAction(ActionType.REMOVE,
        (int) p.x(), (int) p.y(),
        p.getWidth(), p.getHeight(),
        p.getColor(), p.getSprite(),
        p.isTiled(), p.getFriction(), p.getRestitution(), p.getCatalogName());
  }
}
