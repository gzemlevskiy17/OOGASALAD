package oogasalad.model.auth.api;

/**
 * A live view of the current login session.
 *
 * Always reflects the latest state — if the player logs out, {@link #isLoggedIn}
 * returns false immediately. Check {@link #isLoggedIn} before calling
 * {@link #currentUser} to avoid a null return.
 */
public interface SessionContext {

  /**
   * Returns the profile of the currently logged-in player,
   * or null if no player is logged in.
   */
  PlayerProfile currentUser();

  /** Returns true if a player is currently logged in. */
  boolean isLoggedIn();
}
