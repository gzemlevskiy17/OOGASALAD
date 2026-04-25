package oogasalad.view.player.internal;

import java.util.Map;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.player.api.PlacedComponent;

/**
 * HUD overlay mirroring the vehicle builder grid. Each cell that holds a {@link Damageable} vehicle
 * component (pig, frame, wheel, fan, engine, etc.) is colored on a green → yellow → red gradient by
 * its current/max HP ratio. Cells with no component stay grey.
 *
 * <p>If two placements share the same {@code (row, col)} (typical for a pig sitting on a frame
 * cell), the entry inserted later in the supplied map wins — the {@code VehicleFactory} inserts
 * pigs after structurals, so pig HP is what's shown for shared cells.
 *
 * <p>Self-updating via {@link AnimationTimer}.
 *
 * @author Paulina Vvedenskaya
 */
public final class VehicleHpHud extends GridPane {

  private static final double CELL_SIZE = 24.0;
  private static final double CELL_GAP = 2.0;
  private static final double PADDING = 8.0;
  private static final Color EMPTY_COLOR = Color.web("#7a7a7a");
  private static final String BACKGROUND_STYLE =
      "-fx-background-color: rgba(0,0,0,0.55); -fx-background-radius: 6;";

  private final Map<PlacedComponent, ? extends Damageable> placementHp;
  private final Rectangle[][] cells;

  public VehicleHpHud(int rows, int cols,
      Map<PlacedComponent, ? extends Damageable> placementHp) {
    if (rows <= 0 || cols <= 0) {
      throw new IllegalArgumentException("rows and cols must be positive");
    }
    this.placementHp = placementHp;
    this.cells = new Rectangle[rows][cols];

    setHgap(CELL_GAP);
    setVgap(CELL_GAP);
    setPadding(new Insets(PADDING));
    setStyle(BACKGROUND_STYLE);
    setMouseTransparent(true);
    setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE, EMPTY_COLOR);
        rect.setArcWidth(4);
        rect.setArcHeight(4);
        cells[r][c] = rect;
        add(rect, c, r);
      }
    }

    new AnimationTimer() {
      @Override
      public void handle(long now) {
        update();
      }
    }.start();
  }

  private static Color hpColor(double frac) {
    double clamped = Math.max(0, Math.min(1, frac));
    if (clamped >= 0.5) {
      double t = (1.0 - clamped) * 2;
      return Color.color(t, 1, 0);
    }
    double t = (0.5 - clamped) * 2;
    return Color.color(1, 1 - t, 0);
  }

  public void update() {
    for (Map.Entry<PlacedComponent, ? extends Damageable> entry : placementHp.entrySet()) {
      PlacedComponent placement = entry.getKey();
      Damageable component = entry.getValue();
      int row = placement.row();
      int col = placement.col();
      if (row < 0 || row >= cells.length || col < 0 || col >= cells[0].length) {
        continue;
      }
      double frac = component.getMaxHp() <= 0 ? 0
          : component.getCurrentHp() / component.getMaxHp();
      cells[row][col].setFill(hpColor(frac));
    }
  }
}
