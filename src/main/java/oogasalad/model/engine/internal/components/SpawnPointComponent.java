package oogasalad.model.engine.internal.components;

import oogasalad.model.engine.api.DefaultTag;

/**
 * Marks where the vehicle spawns when gameplay begins. Non-physical — no polygon,
 * no mass, not affected by gravity. Stored in the World so save/load can round-trip
 * the spawn position without a separate data channel.
 */
public class SpawnPointComponent extends StaticComponent {

  public SpawnPointComponent(double x, double y) {
    super(x, y, 0, 0, 0);
    setTag(DefaultTag.STATIC.value());
  }

  public void moveTo(double x, double y) {
    setPosition(x, y, 0);
  }
}
