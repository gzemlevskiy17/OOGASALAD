package oogasalad.model.player.api;

/**
 * One of the four sides of a placed component. Used by {@link BuildRules} to describe which sides
 * accept attachment, which must face open space, etc.
 * <p>
 * Enum order is clockwise so that {@link #rotate(int)} can advance by ordinal arithmetic.
 *
 * @author Paulina Vvedenskaya
 */
public enum Side {
  TOP, RIGHT, BOTTOM, LEFT;

  public Side opposite() {
    return rotate(2);
  }

  /**
   * Returns this side rotated clockwise by {@code quarterTurns}. Negative values rotate CCW.
   */
  public Side rotate(int quarterTurns) {
    int n = ((ordinal() + quarterTurns) % 4 + 4) % 4;
    return values()[n];
  }
}
