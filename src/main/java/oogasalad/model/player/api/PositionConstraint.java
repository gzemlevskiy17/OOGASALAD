package oogasalad.model.player.api;

/**
 * Grid-wide position rules applied to a single placement. Local-neighbor rules live on
 * {@link BuildRules#requiredOpenSides()} instead.
 *
 * @author Paulina Vvedenskaya
 */
public enum PositionConstraint {
  NONE, BOTTOM_ROW_OF_GRID
}
