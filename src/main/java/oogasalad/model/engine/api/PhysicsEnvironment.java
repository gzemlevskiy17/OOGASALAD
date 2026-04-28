package oogasalad.model.engine.api;

/**
 * Per-level environmental physics constants applied every simulation step.
 *
 * <p>Bundles {@link #gravity()} as a screen-space vector (Y grows downward) and the linear
 * {@link #drag()} coefficient that opposes velocity. A {@link World} exposes one of these so the
 * engine can read both from a single source instead of receiving them through separate
 * constructor arguments.
 */
public record PhysicsEnvironment(Vector gravity, double drag) {

  public static final PhysicsEnvironment DEFAULT =
      new PhysicsEnvironment(new Vector(0, 980), 0.1);
}
