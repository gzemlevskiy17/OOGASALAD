package oogasalad.view.authoring.internal.level;

import java.util.ResourceBundle;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.SpawnPointComponent;

/**
 * Renders the vehicle spawn marker and guide line.
 */
class SpawnGuideRenderer {

  private static final double MARKER_STROKE_WIDTH = 2.0;
  private static final double MARKER_DOT_RADIUS_DIVISOR = 3.0;

  private final ResourceBundle properties;
  private final double cellSize;

  SpawnGuideRenderer(ResourceBundle properties, double cellSize) {
    this.properties = properties;
    this.cellSize = cellSize;
  }

  void drawSpawnGuide(World world, Pane root, int gridCols) {
    SpawnPointComponent spawnComponent = findSpawnComponent(world);
    double spawnX = spawnComponent != null ? spawnComponent.x() * cellSize : d("spawnX");
    double spawnY = spawnComponent != null ? spawnComponent.y() * cellSize : d("spawnY");
    double markerRadius = d("spawnMarkerRadius");
    Color markerColor = Color.web(properties.getString("spawnMarkerColor"));

    root.getChildren().add(buildGroundGuide(spawnY, gridCols));
    root.getChildren().add(buildMarkerRing(spawnX, spawnY, markerRadius, markerColor));
    root.getChildren().add(buildMarkerDot(spawnX, spawnY, markerRadius, markerColor));
    root.getChildren().add(buildLabel(spawnX, spawnY, markerColor));
  }

  private SpawnPointComponent findSpawnComponent(World world) {
    if (world == null) {
      return null;
    }
    return world.getAllComponents().stream()
        .filter(SpawnPointComponent.class::isInstance)
        .map(SpawnPointComponent.class::cast)
        .findFirst()
        .orElse(null);
  }

  private Line buildGroundGuide(double spawnY, int gridCols) {
    Line ground = new Line(0, spawnY, gridCols * cellSize, spawnY);
    ground.setStroke(Color.web(properties.getString("groundGuideColor")));
    ground.setStrokeWidth(d("groundGuideStrokeWidth"));
    ground.getStrokeDashArray().addAll(d("groundGuideDashOn"), d("groundGuideDashOff"));
    ground.setMouseTransparent(true);
    return ground;
  }

  private Circle buildMarkerRing(double spawnX, double spawnY, double markerRadius,
      Color markerColor) {
    Circle marker = new Circle(spawnX, spawnY, markerRadius, Color.TRANSPARENT);
    marker.setStroke(markerColor);
    marker.setStrokeWidth(MARKER_STROKE_WIDTH);
    marker.setMouseTransparent(true);
    return marker;
  }

  private Circle buildMarkerDot(double spawnX, double spawnY, double markerRadius,
      Color markerColor) {
    Circle markerDot = new Circle(spawnX, spawnY, markerRadius / MARKER_DOT_RADIUS_DIVISOR,
        markerColor);
    markerDot.setMouseTransparent(true);
    return markerDot;
  }

  private Text buildLabel(double spawnX, double spawnY, Color markerColor) {
    Text label = new Text(
        spawnX + d("spawnLabelOffsetX"),
        spawnY + d("spawnLabelOffsetY"),
        "vehicle spawn");
    label.setFill(markerColor);
    label.setMouseTransparent(true);
    return label;
  }

  private double d(String key) {
    return Double.parseDouble(properties.getString(key));
  }
}
