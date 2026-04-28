package oogasalad.controller.player.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.internal.components.RigidBody;
import oogasalad.model.player.api.PlacedComponent;
import oogasalad.view.player.internal.GamePlayAppScreen;
import oogasalad.view.player.internal.VehicleHpHud;

/**
 * Attaches runtime vehicle health UI to the gameplay screen.
 *
 * @author Cody
 */
final class VehicleHudAttacher {

  private static final String BUILDER_PROPS = "oogasalad.view.player.VehicleBuilderAppScreen";
  private static final String GRID_ROWS_KEY = "grid.rows";
  private static final String GRID_COLUMNS_KEY = "grid.columns";
  private static final double HUD_BOTTOM_MARGIN = 16.0;
  private static final double HUD_LEFT_MARGIN = 16.0;
  private static final double NO_MARGIN = 0.0;

  void attach(GamePlayAppScreen screen, Map<PlacedComponent, RigidBody> placements) {
    Map<PlacedComponent, Damageable> placementHp = damageablePlacements(placements);
    ResourceBundle builderProps = ResourceBundle.getBundle(BUILDER_PROPS);
    int rows = readGridSize(builderProps, GRID_ROWS_KEY);
    int cols = readGridSize(builderProps, GRID_COLUMNS_KEY);
    VehicleHpHud hud = new VehicleHpHud(rows, cols, placementHp);
    StackPane.setMargin(hud, new Insets(NO_MARGIN, NO_MARGIN, HUD_BOTTOM_MARGIN,
        HUD_LEFT_MARGIN));
    screen.attachOverlay(hud, Pos.BOTTOM_LEFT);
  }

  private Map<PlacedComponent, Damageable> damageablePlacements(
      Map<PlacedComponent, RigidBody> placements) {
    Map<PlacedComponent, Damageable> placementHp = new LinkedHashMap<>();
    for (Map.Entry<PlacedComponent, RigidBody> entry : placements.entrySet()) {
      if (entry.getValue() instanceof Damageable damageable) {
        placementHp.put(entry.getKey(), damageable);
      }
    }
    return placementHp;
  }

  private int readGridSize(ResourceBundle builderProps, String key) {
    double configuredValue = Double.parseDouble(builderProps.getString(key));
    return (int) configuredValue;
  }
}
