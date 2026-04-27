package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.SpawnPointComponent;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.model.engine.internal.spline.ParabolaSpline;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JavaFX grid canvas for the level editor.
 *
 * Rendering rules:
 *   TerrainBlock with tiled=true  → repeat texture across the block's bounds (tiled rendering).
 *   TerrainBlock with tiled=false → single scaled sprite, or fallback color if not found.
 *   Grid lines are always drawn on top of placed objects.
 *
 * All grid settings (columns, rows, cellSize, colors, etc.) are read from
 * the AuthoringCanvas.properties resource bundle.
 *
 * Collaborators: AuthoringControllerImpl (calls refreshGrid after model changes,
 *                registers mouse handlers on getVisualRoot()),
 *                WorldImpl (source of objects to render).
 */
public class AuthoringCanvasImpl implements AuthoringCanvas {

  private static final Logger LOG = LogManager.getLogger(AuthoringCanvasImpl.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringCanvas");

  private static final Color TERRAIN_FALLBACK_COLOR = Color.SADDLEBROWN;
  private static final Color TERRAIN_STROKE_COLOR = Color.BLACK;
  private static final double ZOOM_MIN = 0.25;
  private static final double ZOOM_MAX = 4.0;

  private final Scale zoomTransform = new Scale(1, 1, 0, 0);
  private double zoomFactor = 1.0;

  private final double cellSize;
  private int gridCols;
  private int gridRows;
  private final int gridLineSpacing;
  private final Color defaultFill;
  private final Color gridStroke;
  private final double gridStrokeWidth;

  private final Pane root;
  private final World world;
  private AuthoringPlaceable selectedPlaceable;
  private Consumer<AuthoringPlaceable> onObjectSelected = obj -> {};

  private Rectangle hoverOverlay;
  private int hoverCol = -1;
  private int hoverRow = -1;
  private final java.util.List<Node> transientOverlays = new java.util.ArrayList<>();

  public AuthoringCanvasImpl() {
    this(null);
  }

  /**
   * @param world the model whose objects are rendered onto the grid
   */
  public AuthoringCanvasImpl(World world) {
    this.world = world;
    this.gridCols = Integer.parseInt(PROPS.getString("columns"));
    this.gridRows = Integer.parseInt(PROPS.getString("rows"));
    this.cellSize = Double.parseDouble(PROPS.getString("cellSize"));
    this.gridLineSpacing = Integer.parseInt(PROPS.getString("gridLineSpacing"));
    this.defaultFill = Color.web(PROPS.getString("defaultFill"));
    this.gridStroke = Color.web(PROPS.getString("gridStroke"));
    this.gridStrokeWidth = Double.parseDouble(PROPS.getString("gridStrokeWidth"));
    this.selectedPlaceable = new TerrainPlaceable(
        cellSize, cellSize, "#8B4513", "assets/terrain/grass.png", true, 0.6, 0.4);
    this.root = new Pane();
    root.getStyleClass().add("authoring-canvas");
    root.getTransforms().add(zoomTransform);
    double totalWidth = gridCols * cellSize;
    double totalHeight = gridRows * cellSize;
    root.setPrefSize(totalWidth, totalHeight);
    root.setStyle("-fx-background-color: " + PROPS.getString("defaultFill") + ";");
    refreshGrid();
  }

  @Override
  public boolean isWithinBounds(int col, int row) {
    if (col < 0 || row < 0) {
      return false;
    }
    if (col >= gridCols || row >= gridRows) {
      gridCols = Math.max(gridCols, col + 1);
      gridRows = Math.max(gridRows, row + 1);
      root.setPrefSize(gridCols * cellSize, gridRows * cellSize);
    }
    return true;
  }

  @Override
  public double getCellSize() {
    return cellSize;
  }

  @Override
  public int getGridCols() {
    return gridCols;
  }

  @Override
  public int getGridRows() {
    return gridRows;
  }

  @Override
  public Node getVisualRoot() {
    return root;
  }

  /**
   * Clears and redraws all authoring objects, grid lines, and spawn reference guides
   * from the current World state. The spawn guide is drawn LAST so the draggable marker
   * sits above both placed objects and the grid lines — otherwise grid lines intercept
   * clicks on the marker and the event bubbles up to the root's paint/draw handler,
   * making the marker effectively un-draggable while the active tool is DRAW or PAINT.
   */
  @Override
  public void refreshGrid() {
    clearHoverPreview();
    clearOverlays();
    root.getChildren().clear();
    if (world != null) {
      drawObjects();
    }
    drawGridLines();
    drawSpawnGuide();
  }

  @Override
  public void addOverlay(Node node) {
    if (node == null) {
      return;
    }
    transientOverlays.add(node);
    root.getChildren().add(node);
  }

  @Override
  public void clearOverlays() {
    if (transientOverlays.isEmpty()) {
      return;
    }
    root.getChildren().removeAll(transientOverlays);
    transientOverlays.clear();
  }

  @Override
  public void setSelectedPlaceable(AuthoringPlaceable placeable) {
    if (placeable != null) {
      this.selectedPlaceable = placeable;
      if (hoverOverlay != null) {
        int col = hoverCol;
        int row = hoverRow;
        clearHoverPreview();
        showHoverPreview(col, row);
      }
    }
  }

  @Override
  public void placeBlock(int col, int row) {
    if (selectedPlaceable == null || !isWithinBounds(col, row)) {
      return;
    }
    int spanCols = selectedPlaceable.colSpan(cellSize);
    int spanRows = selectedPlaceable.rowSpan(cellSize);
    Rectangle rect = new Rectangle(
        col * cellSize, row * cellSize,
        spanCols * cellSize, spanRows * cellSize);
    rect.setFill(Color.web(selectedPlaceable.getColor()));
    rect.setStroke(TERRAIN_STROKE_COLOR);
    root.getChildren().add(rect);
    drawGridLines();
  }

  @Override
  public void showHoverPreview(int col, int row) {
    if (col == hoverCol && row == hoverRow) {
      return;
    }
    clearHoverPreview();
    if (col < 0 || col >= gridCols || row < 0 || row >= gridRows) {
      return;
    }

    int spanCols = selectedPlaceable.colSpan(cellSize);
    int spanRows = selectedPlaceable.rowSpan(cellSize);
    Color previewColor = Color.web(selectedPlaceable.getColor(), 0.5);

    hoverOverlay = new Rectangle(
        col * cellSize, row * cellSize,
        spanCols * cellSize, spanRows * cellSize);
    hoverOverlay.setFill(previewColor);
    hoverOverlay.setMouseTransparent(true);
    root.getChildren().add(hoverOverlay);
    hoverCol = col;
    hoverRow = row;
  }

  @Override
  public void setOnObjectSelected(Consumer<AuthoringPlaceable> handler) {
    if (handler != null) {
      this.onObjectSelected = handler;
    }
  }

  @Override
  public void clearHoverPreview() {
    if (hoverOverlay != null) {
      root.getChildren().remove(hoverOverlay);
      hoverOverlay = null;
      hoverCol = -1;
      hoverRow = -1;
    }
  }

  @Override
  public void setZoom(double factor) {
    zoomFactor = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, factor));
    zoomTransform.setX(zoomFactor);
    zoomTransform.setY(zoomFactor);
  }

  @Override
  public double getZoom() {
    return zoomFactor;
  }

  private void drawObjects() {
    for (GameObject obj : world.getObjects()) {
      if (obj instanceof TerrainSpline spline) {
        root.getChildren().add(drawSplineObject(spline));
      } else if (obj instanceof AuthoringPlaceable placeable) {
        root.getChildren().add(drawObject(placeable));
      }
      // Non-visual markers (e.g. SpawnPointComponent) live in the world for
      // save/load and lookup but have no grid representation; drawSpawnGuide()
      // renders them separately. Blind-casting here previously crashed
      // refreshGrid(), which left the canvas frozen and un-clickable.
    }
  }

  /**
   * Draws a committed {@link TerrainSpline} as a quadratic-Bézier {@link Path} filled down to its
   * baseline. Coordinates are scaled from the authoring grid-cell space into pixel space by
   * {@link #cellSize}, so the committed curve sits in the canvas at the same location the
   * designer drafted it.
   */
  private Path drawSplineObject(TerrainSpline terrainSpline) {
    ParabolaSpline spline = terrainSpline.getSpline();
    Path path = new Path();

    Vector firstKnot = spline.knots().get(0);
    path.getElements().add(new MoveTo(firstKnot.x() * cellSize, firstKnot.y() * cellSize));
    for (int i = 0; i < spline.segmentCount(); i++) {
      Vector control = spline.controls().get(i);
      Vector next = spline.knots().get(i + 1);
      path.getElements().add(new QuadCurveTo(
          control.x() * cellSize, control.y() * cellSize,
          next.x() * cellSize, next.y() * cellSize));
    }
    Vector lastKnot = spline.knots().get(spline.knots().size() - 1);
    double baselinePx = spline.baselineY() * cellSize;
    path.getElements().add(new LineTo(lastKnot.x() * cellSize, baselinePx));
    path.getElements().add(new LineTo(firstKnot.x() * cellSize, baselinePx));
    path.getElements().add(new ClosePath());

    path.setFill(parseColor(terrainSpline.getColor()));
    path.setStroke(TERRAIN_STROKE_COLOR);
    path.setStrokeWidth(1.0);
    path.setOnMousePressed(e -> onObjectSelected.accept(terrainSpline));
    return path;
  }

  private Rectangle drawObject(AuthoringPlaceable placeable) {
    double px = placeable.getX() * cellSize;
    double py = placeable.getY() * cellSize;
    double pw = placeable.getWidth() * cellSize;
    double ph = placeable.getHeight() * cellSize;

    Rectangle rect = new Rectangle(px, py, pw, ph);
    rect.setStroke(TERRAIN_STROKE_COLOR);

    Color fallback = parseColor(placeable.getColor());
    if (placeable.isTiled()) {
      applyTiledTexture(rect, placeable.getSprite(), fallback);
    } else {
      applySprite(rect, placeable.getSprite(), fallback);
    }

    rect.setOnMousePressed(e -> onObjectSelected.accept(placeable));

    return rect;
  }

  private Color parseColor(String css) {
    if (css == null || css.isBlank()) {
      return TERRAIN_FALLBACK_COLOR;
    }
    try {
      return Color.web(css);
    } catch (IllegalArgumentException e) {
      return TERRAIN_FALLBACK_COLOR;
    }
  }

  /**
   * Repeats the texture image in a tile pattern across the rectangle.
   * Falls back to the block's color if the image file cannot be loaded.
   */
  private void applyTiledTexture(Rectangle rect, String spritePath, Color fallback) {
    Image img = tryLoadImage(spritePath);
    if (img != null) {
      rect.setFill(new ImagePattern(img, 0, 0, cellSize, cellSize, false));
    } else {
      rect.setFill(fallback);
    }
  }

  /**
   * Scales a single image to fill the rectangle.
   * Falls back to the block's color if the image file cannot be loaded.
   */
  private void applySprite(Rectangle rect, String spritePath, Color fallback) {
    Image img = tryLoadImage(spritePath);
    if (img != null) {
      double w = rect.getWidth();
      double h = rect.getHeight();
      rect.setFill(new ImagePattern(img, 0, 0, w, h, false));
    } else {
      rect.setFill(fallback);
    }
  }

  private Image tryLoadImage(String spritePath) {
    if (spritePath == null || spritePath.isBlank()) {
      return null;
    }
    try {
      java.io.File file = new java.io.File(spritePath);
      if (file.isFile()) {
        return new Image(file.toURI().toString());
      }
      String normalized = spritePath.startsWith("/") ? spritePath.substring(1) : spritePath;
      java.net.URL resource = getClass().getClassLoader().getResource(normalized);
      if (resource != null) {
        return new Image(resource.toExternalForm());
      }
    } catch (Exception e) {
      LOG.warn("Could not load sprite: {}", spritePath);
    }
    return null;
  }

  /**
   * Draws a draggable spawn marker. Position is read from the world's SpawnPointComponent so
   * it persists across save/load. Dragging the marker updates the component directly and
   * redraws the canvas.
   */
  private void drawSpawnGuide() {
    SpawnPointComponent spawnComp = world == null ? null :
        world.getAllComponents().stream()
            .filter(SpawnPointComponent.class::isInstance)
            .map(SpawnPointComponent.class::cast)
            .findFirst().orElse(null);

    // SpawnPointComponent stores grid-cell units (same as TerrainBlock); multiply by cellSize
    // to get canvas pixel coordinates for rendering.
    double spawnX = spawnComp != null ? spawnComp.x() * cellSize : d("spawnX");
    double spawnY = spawnComp != null ? spawnComp.y() * cellSize : d("spawnY");
    double markerRadius = d("spawnMarkerRadius");
    double totalWidth = gridCols * cellSize;

    Line ground = new Line(0, spawnY, totalWidth, spawnY);
    ground.setStroke(Color.web(PROPS.getString("groundGuideColor")));
    ground.setStrokeWidth(d("groundGuideStrokeWidth"));
    ground.getStrokeDashArray().addAll(d("groundGuideDashOn"), d("groundGuideDashOff"));
    ground.setMouseTransparent(true);
    root.getChildren().add(ground);

    Color markerColor = Color.web(PROPS.getString("spawnMarkerColor"));
    Circle marker = new Circle(spawnX, spawnY, markerRadius, Color.TRANSPARENT);
    marker.setStroke(markerColor);
    marker.setStrokeWidth(2.0);
    marker.setMouseTransparent(true);
    root.getChildren().add(marker);

    Circle markerDot = new Circle(spawnX, spawnY, markerRadius / 3.0, markerColor);
    markerDot.setMouseTransparent(true);
    root.getChildren().add(markerDot);

    Text label = new Text(
        spawnX + d("spawnLabelOffsetX"),
        spawnY + d("spawnLabelOffsetY"),
        "vehicle spawn");
    label.setFill(markerColor);
    label.setMouseTransparent(true);
    root.getChildren().add(label);

  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }

  private void drawGridLines() {
    if (gridLineSpacing <= 0) {
      return;
    }
    double totalWidth = gridCols * cellSize;
    double totalHeight = gridRows * cellSize;

    for (int col = 0; col <= gridCols; col += gridLineSpacing) {
      double x = col * cellSize;
      Line line = new Line(x, 0, x, totalHeight);
      line.setStroke(gridStroke);
      line.setStrokeWidth(gridStrokeWidth);
      root.getChildren().add(line);
    }

    for (int row = 0; row <= gridRows; row += gridLineSpacing) {
      double y = row * cellSize;
      Line line = new Line(0, y, totalWidth, y);
      line.setStroke(gridStroke);
      line.setStrokeWidth(gridStrokeWidth);
      root.getChildren().add(line);
    }
  }
}
