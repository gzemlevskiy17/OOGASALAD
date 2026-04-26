package oogasalad.controller.authoring.internal;

import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.SpawnPointComponent;

/**
 * Locates and moves the spawn point in the authoring world. A default spawn is created on
 * first access if none exists, so callers can rely on one always being present.
 *
 * @author Paulina Vvedenskaya
 */
public class SpawnPointService {

  private static final double DEFAULT_COL = 5.0;
  private static final double DEFAULT_ROW = 5.0;

  private final World world;

  /**
   * @param world the authoring world this service reads and writes to
   */
  public SpawnPointService(World world) {
    this.world = world;
  }

  /**
   * Returns the current spawn-point component, creating one at the default location if the world
   * doesn't already contain one.
   *
   * @return the spawn point (never null)
   */
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

  /**
   * Moves the spawn point to the given grid cell.
   *
   * @param col target column in cell units
   * @param row target row in cell units
   */
  public void moveSpawnTo(double col, double row) {
    getSpawnPoint().moveTo(col, row);
  }
}
