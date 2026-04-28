package oogasalad.model.player.api;

/**
 * Describes whether a component must be placed inside another component (e.g. a pig inside a
 * frame). Extend as more container types appear.
 *
 * @author Paulina Vvedenskaya
 * @author Iurii Beliaev
 */
public enum Containment {
  NONE, INSIDE_FRAME, INSIDE_OR_OUTSIDE_FRAME
}
