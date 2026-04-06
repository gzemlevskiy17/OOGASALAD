package oogasalad.view.authoring.internal;

import java.util.Collection;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.TerrainBlock;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.PlacedObject;
import oogasalad.view.authoring.api.PlacedObjectFactory;
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
 * CELL_SIZE must match AuthoringControllerImpl.CELL_SIZE so mouse-to-grid
 * coordinate math stays consistent between controller and view.
 *
 * Collaborators: AuthoringControllerImpl (calls refreshGrid after model changes,
 *                registers mouse handlers on getVisualRoot()),
 *                WorldImpl (source of objects to render).
 */
public class MichaelCanvas implements AuthoringCanvas {

  private static final Logger LOG = LogManager.getLogger(MichaelCanvas.class);

  /** Pixels per grid unit — must match AuthoringControllerImpl.CELL_SIZE. */
  public static final double CELL_SIZE = 40.0;

  private static final Color GRID_LINE_COLOR = Color.LIGHTGRAY;
  private static final Color TERRAIN_FALLBACK_COLOR = Color.SADDLEBROWN;
  private static final Color TERRAIN_STROKE_COLOR = Color.BLACK;

  private final Pane root;
  private final World world;
  private final int gridCols;
  private final int gridRows;
  private PlacedObjectFactory placedObjectFactory;

  private Rectangle hoverOverlay;
  private int hoverCol = -1;
  private int hoverRow = -1;

  public MichaelCanvas() {
    this(null, 30, 18);
  }

  /**
   * @param world     the model whose objects are rendered onto the grid
   * @param gridCols  number of columns (horizontal grid units)
   * @param gridRows  number of rows (vertical grid units)
   */
  public MichaelCanvas(World world, int gridCols, int gridRows) {
    this.world = world;
    this.gridCols = gridCols;
    this.gridRows = gridRows;
    this.placedObjectFactory = new DefaultPlacedObjectFactory(CELL_SIZE, CELL_SIZE, "#8B4513", null);
    this.root = new Pane();
    root.setPrefSize(gridCols * CELL_SIZE, gridRows * CELL_SIZE);
    refreshGrid();
  }

  @Override
  public Node getVisualRoot() {
    return root;
  }

  /** Clears and redraws all terrain blocks and grid lines from the current World state. */
  @Override
  public void refreshGrid() {
    clearHoverPreview();
    root.getChildren().clear();
    if (world != null) {
      drawTerrainObjects();
    }
    drawGridLines();
  }

  @Override
  public Collection<PlacedObject> getPlacedObjects() {
    return List.of();
  }

  @Override
  public void setPlacedObjectFactory(PlacedObjectFactory factory) {
    if (factory != null) {
      this.placedObjectFactory = factory;
    }
  }

  @Override
  public void placeBlock(int col, int row) {
    if (placedObjectFactory == null) {
      return;
    }
    PlacedObject placedObject = placedObjectFactory.create(col, row, CELL_SIZE);
    Rectangle preview = new Rectangle(
        placedObject.getDrawX(),
        placedObject.getDrawY(),
        placedObject.getWidth(),
        placedObject.getHeight()
    );
    preview.setFill(Color.web(placedObject.getColor()));
    preview.setStroke(TERRAIN_STROKE_COLOR);
    root.getChildren().add(preview);
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

    int spanCols = placedObjectFactory.colSpan(CELL_SIZE);
    int spanRows = placedObjectFactory.rowSpan(CELL_SIZE);
    PlacedObject sample = placedObjectFactory.create(col, row, CELL_SIZE);
    Color previewColor = Color.web(sample.getColor(), 0.5);

    hoverOverlay = new Rectangle(
        col * CELL_SIZE, row * CELL_SIZE,
        spanCols * CELL_SIZE, spanRows * CELL_SIZE);
    hoverOverlay.setFill(previewColor);
    hoverOverlay.setMouseTransparent(true);
    root.getChildren().add(hoverOverlay);
    hoverCol = col;
    hoverRow = row;
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

  private void drawTerrainObjects() {
    for (GameObject obj : world.getObjects()) {
      if (obj instanceof TerrainBlock tb) {
        root.getChildren().add(buildTerrainRect(tb));
      }
    }
  }

  private Rectangle buildTerrainRect(TerrainBlock tb) {
    double px = tb.getX() * CELL_SIZE;
    double py = tb.getY() * CELL_SIZE;
    double pw = tb.getWidth() * CELL_SIZE;
    double ph = tb.getHeight() * CELL_SIZE;

    Rectangle rect = new Rectangle(px, py, pw, ph);
    rect.setStroke(TERRAIN_STROKE_COLOR);

    if (tb.isTiled()) {
      applyTiledTexture(rect, tb.getSprite());
    } else {
      applySprite(rect, tb.getSprite());
    }

    return rect;
  }

  /**
   * Repeats the texture image in a tile pattern across the rectangle.
   * Falls back to a solid color if the image file cannot be loaded.
   */
  private void applyTiledTexture(Rectangle rect, String spritePath) {
    Image img = tryLoadImage(spritePath);
    if (img != null) {
      rect.setFill(new ImagePattern(img, 0, 0, CELL_SIZE, CELL_SIZE, false));
    } else {
      rect.setFill(TERRAIN_FALLBACK_COLOR);
    }
  }

  /**
   * Scales a single image to fill the rectangle.
   * Falls back to a solid color if the image file cannot be loaded.
   */
  private void applySprite(Rectangle rect, String spritePath) {
    Image img = tryLoadImage(spritePath);
    if (img != null) {
      double w = rect.getWidth();
      double h = rect.getHeight();
      rect.setFill(new ImagePattern(img, 0, 0, w, h, false));
    } else {
      rect.setFill(TERRAIN_FALLBACK_COLOR);
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
    double totalWidth = gridCols * CELL_SIZE;
    double totalHeight = gridRows * CELL_SIZE;

    for (int col = 0; col <= gridCols; col++) {
      double x = col * CELL_SIZE;
      Line line = new Line(x, 0, x, totalHeight);
      line.setStroke(GRID_LINE_COLOR);
      root.getChildren().add(line);
    }

    for (int row = 0; row <= gridRows; row++) {
      double y = row * CELL_SIZE;
      Line line = new Line(0, y, totalWidth, y);
      line.setStroke(GRID_LINE_COLOR);
      root.getChildren().add(line);
    }
  }
}
