package oogasalad.controller.authoring.internal;

import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;

/**
 * Locates and moves the spawn point in the authoring world. A default spawn is created on first
 * access if none exists, so callers can rely on one always being present.
 */
public class SpawnPointService {

  private static final double DEFAULT_COL = 5.0;
  private static final double DEFAULT_ROW = 5.0;

  private final World world;

  public SpawnPointService(World world) {
    this.world = world;
  }

  public SpawnPointComponent getSpawnPoint() {
    for (GameComponent component : world.getAllComponents()) {
      if (component instanceof SpawnPointComponent spawn) {
        return spawn;
      }
    }
    SpawnPointComponent created = new SpawnPointComponent(DEFAULT_COL, DEFAULT_ROW);
    world.addObject(created);
    return created;
  }

  public void moveSpawnTo(double col, double row) {
    getSpawnPoint().moveTo(col, row);
  }
}
