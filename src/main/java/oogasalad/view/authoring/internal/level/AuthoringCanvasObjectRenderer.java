package oogasalad.view.authoring.internal.level;

import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.AuthoringStaticComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import oogasalad.view.core.internal.TerrainPaints;

/**
 * Renders world-backed objects onto the authoring canvas.
 */
class AuthoringCanvasObjectRenderer {

  private static final String GOAL_LINE_TYPE = "GoalLine";
  private static final Color GOAL_LINE_COLOR = Color.web("#FFD700", 0.55);
  private static final Color GOAL_LINE_STROKE = Color.web("#FFD700");
  private static final double GOAL_LINE_WIDTH_PIXELS = 8.0;
  private static final double GOAL_LINE_STROKE_WIDTH = 1.5;
  private static final double GOAL_LINE_LABEL_FONT_SIZE = 10.0;
  private static final double GOAL_LINE_LABEL_ROTATION_DEGREES = -90.0;
  private static final double GOAL_LINE_LABEL_OFFSET_X = 20.0;
  private static final double GOAL_LINE_LABEL_OFFSET_Y = 6.0;
  private static final double TERRAIN_STROKE_WIDTH = 1.0;
  private static final int COLOR_CHANNEL_MAX = 255;
  private static final Color TERRAIN_STROKE_COLOR = Color.BLACK;

  private final double cellSize;
  private final Consumer<AuthoringPlaceable> onObjectSelected;
  private final Consumer<AuthoringPlaceable> onTerrainHoverEnter;
  private final Runnable onTerrainHoverExit;

  AuthoringCanvasObjectRenderer(double cellSize,
      Consumer<AuthoringPlaceable> onObjectSelected,
      Consumer<AuthoringPlaceable> onTerrainHoverEnter,
      Runnable onTerrainHoverExit) {
    this.cellSize = cellSize;
    this.onObjectSelected = onObjectSelected;
    this.onTerrainHoverEnter = onTerrainHoverEnter;
    this.onTerrainHoverExit = onTerrainHoverExit;
  }

  void renderObjects(World world, Pane root, int gridRows) {
    if (world == null) {
      return;
    }
    for (GameObject object : world.getObjects()) {
      addVisualObject(root, object, gridRows);
    }
  }

  private void addVisualObject(Pane root, GameObject object, int gridRows) {
    if (object instanceof TerrainSpline terrainSpline) {
      root.getChildren().add(drawSplineObject(terrainSpline));
    } else if (object instanceof AuthoringStaticComponent staticComponent
        && GOAL_LINE_TYPE.equals(staticComponent.getComponentType())) {
      root.getChildren().add(drawGoalLineStripe(staticComponent, gridRows));
    } else if (object instanceof AuthoringPlaceable placeable) {
      root.getChildren().add(drawObject(placeable));
    }
  }

  private Path drawSplineObject(TerrainSpline terrainSpline) {
    ParabolaSpline spline = terrainSpline.getSpline();
    Path path = new Path();

    Vector firstKnot = spline.knots().get(0);
    path.getElements().add(new MoveTo(firstKnot.x() * cellSize, firstKnot.y() * cellSize));
    for (int segmentIndex = 0; segmentIndex < spline.segmentCount(); segmentIndex++) {
      Vector control = spline.controls().get(segmentIndex);
      Vector next = spline.knots().get(segmentIndex + 1);
      path.getElements().add(new QuadCurveTo(
          control.x() * cellSize, control.y() * cellSize,
          next.x() * cellSize, next.y() * cellSize));
    }
    Vector lastKnot = spline.knots().get(spline.knots().size() - 1);
    double baselinePixels = spline.baselineY() * cellSize;
    path.getElements().add(new LineTo(lastKnot.x() * cellSize, baselinePixels));
    path.getElements().add(new LineTo(firstKnot.x() * cellSize, baselinePixels));
    path.getElements().add(new ClosePath());

    TerrainPaints.applyTerrainFill(path, terrainSpline.getSprite(), terrainSpline.getColor(),
        terrainSpline.getWidth() * cellSize, terrainSpline.getHeight() * cellSize,
        terrainSpline.isTiled());
    path.setStroke(TERRAIN_STROKE_COLOR);
    path.setStrokeWidth(TERRAIN_STROKE_WIDTH);
    wireSelectionHandlers(path, terrainSpline);
    return path;
  }

  private Node drawGoalLineStripe(AuthoringStaticComponent staticComponent, int gridRows) {
    double xPixels = staticComponent.x() * cellSize;
    double totalHeight = gridRows * cellSize;

    Rectangle stripe = new Rectangle(xPixels - GOAL_LINE_WIDTH_PIXELS / 2, 0,
        GOAL_LINE_WIDTH_PIXELS, totalHeight);
    stripe.setFill(GOAL_LINE_COLOR);
    stripe.setStroke(GOAL_LINE_STROKE);
    stripe.setStrokeWidth(GOAL_LINE_STROKE_WIDTH);

    Label label = new Label("FINISH");
    label.setFont(Font.font(GOAL_LINE_LABEL_FONT_SIZE));
    label.setTextFill(GOAL_LINE_STROKE);
    label.setRotate(GOAL_LINE_LABEL_ROTATION_DEGREES);
    label.setMouseTransparent(true);
    label.setLayoutX(xPixels - GOAL_LINE_LABEL_OFFSET_X);
    label.setLayoutY(totalHeight / 2 - GOAL_LINE_LABEL_OFFSET_Y);

    wireSelectionHandlers(stripe, staticComponent);
    Pane group = new Pane(stripe, label);
    group.setMouseTransparent(false);
    return group;
  }

  private Rectangle drawObject(AuthoringPlaceable placeable) {
    Rectangle rectangle = new Rectangle(
        placeable.x() * cellSize,
        placeable.y() * cellSize,
        placeable.getWidth() * cellSize,
        placeable.getHeight() * cellSize);
    rectangle.setStroke(TERRAIN_STROKE_COLOR);

    Color fallback = TerrainPaints.parseColor(placeable.getColor());
    double paintWidth = placeable.isTiled() ? cellSize : rectangle.getWidth();
    double paintHeight = placeable.isTiled() ? cellSize : rectangle.getHeight();
    TerrainPaints.applyTerrainFill(rectangle, placeable.getSprite(), toCss(fallback),
        paintWidth, paintHeight, placeable.isTiled());
    wireSelectionHandlers(rectangle, placeable);
    return rectangle;
  }

  private void wireSelectionHandlers(Node node, AuthoringPlaceable placeable) {
    node.setOnMousePressed(event -> onObjectSelected.accept(placeable));
    node.setOnMouseEntered(event -> onTerrainHoverEnter.accept(placeable));
    node.setOnMouseExited(event -> onTerrainHoverExit.run());
  }

  private String toCss(Color color) {
    return String.format("#%02X%02X%02X",
        (int) Math.round(color.getRed() * COLOR_CHANNEL_MAX),
        (int) Math.round(color.getGreen() * COLOR_CHANNEL_MAX),
        (int) Math.round(color.getBlue() * COLOR_CHANNEL_MAX));
  }
}
