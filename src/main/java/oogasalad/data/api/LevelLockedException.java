package oogasalad.data.api;

/**
 * Thrown when a user tries to play a level they haven't unlocked yet. Carries both the level
 * they attempted and the furthest level they've reached so the view layer can render a useful
 * popup (e.g. "Level forest/second is locked — you've reached forest/first").
 *
 * <p>{@code furthestReached} may be null when the user has no progression row yet (brand new,
 * no wins). UI should treat null as "you need to complete level 1 first".
 */
public class LevelLockedException extends RuntimeException {

  private final LevelId attempted;
  private final LevelId furthestReached;

  public LevelLockedException(LevelId attempted, LevelId furthestReached) {
    super("Level " + attempted + " is locked"
        + (furthestReached != null ? "; furthest reached: " + furthestReached : ""));
    this.attempted = attempted;
    this.furthestReached = furthestReached;
  }

  public LevelId attempted() {
    return attempted;
  }

  /** May be null if the user has no progression history yet. */
  public LevelId furthestReached() {
    return furthestReached;
  }
}
