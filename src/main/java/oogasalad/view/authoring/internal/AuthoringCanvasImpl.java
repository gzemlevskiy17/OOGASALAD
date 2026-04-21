package oogasalad.view.authoring.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
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
        cellSize, cellSize, "#8B4513", "assets/terrain/grass.png", true, 0.6);
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

  /** Clears and redraws all authoring objects and grid lines from the current World state. */
  @Override
  public void refreshGrid() {
    clearHoverPreview();
    root.getChildren().clear();
    if (world != null) {
      drawObjects();
    }
    drawGridLines();
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
      root.getChildren().add(drawObject((AuthoringPlaceable) obj));
    }
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
    try {
      java.io.File file = new java.io.File(spritePath);
      if (file.exists()) {
        return new Image(file.toURI().toString());
      }
    } catch (Exception e) {
      LOG.warn("Could not load sprite: {}", spritePath);
    }
    return null;
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
