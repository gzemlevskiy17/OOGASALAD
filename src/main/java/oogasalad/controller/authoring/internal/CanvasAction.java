package oogasalad.controller.authoring.internal;

import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Immutable snapshot of a single canvas modification (block placed or removed).
 *
 * <p>The {@code object} field holds the original {@link AuthoringPlaceable} reference so that
 * undo/redo can re-add the exact same object (correct type, full geometry) rather than
 * reconstructing it from the flattened fields.
 */
public record CanvasAction(ActionType type, int col, int row,
    double width, double height, String color, String sprite,
    boolean tiled, double friction, double restitution,
    String displayName, AuthoringPlaceable object) {

  public CanvasAction(ActionType type, int col, int row,
      double width, double height, String color, String sprite,
      boolean tiled, double friction, double restitution) {
    this(type, col, row, width, height, color, sprite, tiled, friction, restitution, "Terrain",
        null);
  }

  public CanvasAction(ActionType type, int col, int row,
      double width, double height, String color, String sprite,
      boolean tiled, double friction, double restitution, String displayName) {
    this(type, col, row, width, height, color, sprite, tiled, friction, restitution, displayName,
        null);
  }

  /** Whether the action added or removed a block. */
  public enum ActionType { PLACE, REMOVE }

  /** Creates a PLACE action from the given placeable's current state. */
  public static CanvasAction place(AuthoringPlaceable p) {
    return new CanvasAction(ActionType.PLACE,
        (int) p.getX(), (int) p.getY(),
        p.getWidth(), p.getHeight(),
        p.getColor(), p.getSprite(),
        p.isTiled(), p.getFriction(), p.getRestitution(), p.getDisplayName(), p);
  }

  /** Creates a REMOVE action from the given placeable's current state. */
  public static CanvasAction remove(AuthoringPlaceable p) {
    return new CanvasAction(ActionType.REMOVE,
        (int) p.getX(), (int) p.getY(),
        p.getWidth(), p.getHeight(),
        p.getColor(), p.getSprite(),
        p.isTiled(), p.getFriction(), p.getRestitution(), p.getDisplayName(), p);
  }
}
