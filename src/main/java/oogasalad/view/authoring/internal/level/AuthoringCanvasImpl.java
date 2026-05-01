package oogasalad.view.authoring.internal.level;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import oogasalad.model.engine.api.World;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * JavaFX grid canvas for the level editor.
 *
 * <p>All grid settings are read from the AuthoringCanvas.properties resource bundle.
 */
public class AuthoringCanvasImpl implements AuthoringCanvas {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringCanvas");

  private static final Color TERRAIN_STROKE_COLOR = Color.BLACK;
  private static final double ZOOM_MIN = 0.25;
  private static final double ZOOM_MAX = 4.0;
  private static final double HOVER_PREVIEW_OPACITY = 0.5;


  private final Scale zoomTransform = new Scale(1, 1, 0, 0);
  private final java.util.List<Node> transientOverlays = new java.util.ArrayList<>();
  private final double cellSize;
  private final Pane root;
  private final World world;
  private final AuthoringCanvasGridRenderer gridRenderer;
  private final SpawnGuideRenderer spawnGuideRenderer;

  private double zoomFactor = 1.0;
  private int gridCols;
  private int gridRows;
  private AuthoringPlaceable selectedPlaceable;
  private String backgroundImagePath = "";
  private Consumer<AuthoringPlaceable> onObjectSelected = obj -> {};
  private Consumer<AuthoringPlaceable> onTerrainHoverEnter = placeable -> {};
  private Runnable onTerrainHoverExit = () -> {};
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
    this.gridRenderer = new AuthoringCanvasGridRenderer(
        cellSize,
        Integer.parseInt(PROPS.getString("gridLineSpacing")),
        Color.web(PROPS.getString("gridStroke")),
        Double.parseDouble(PROPS.getString("gridStrokeWidth")));
    this.spawnGuideRenderer = new SpawnGuideRenderer(PROPS, cellSize);
    this.root = new Pane();
    root.getStyleClass().add("authoring-canvas");
    root.getTransforms().add(zoomTransform);
    resizeRoot();
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
      resizeRoot();
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
   * Clears and redraws authoring objects, grid lines, and spawn reference guides.
   */
  @Override
  public void refreshGrid() {
    clearHoverPreview();
    clearOverlays();
    root.getChildren().clear();
    drawBackgroundImage();
    objectRenderer().renderObjects(world, root, gridRows);
    gridRenderer.drawGridLines(root, gridCols, gridRows);
    spawnGuideRenderer.drawSpawnGuide(world, root, gridCols);
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
  public void setBackgroundImagePath(String imagePath) {
    backgroundImagePath = imagePath == null ? "" : imagePath.strip();
    refreshGrid();
  }

  @Override
  public void setSelectedPlaceable(AuthoringPlaceable placeable) {
    this.selectedPlaceable = placeable;
    if (placeable == null) {
      clearHoverPreview();
      return;
    }
    if (hoverOverlay != null) {
      int col = hoverCol;
      int row = hoverRow;
      clearHoverPreview();
      showHoverPreview(col, row);
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
    gridRenderer.drawGridLines(root, gridCols, gridRows);
  }

  @Override
  public void showHoverPreview(int col, int row) {
    if (selectedPlaceable == null) {
      clearHoverPreview();
      return;
    }
    if (col == hoverCol && row == hoverRow) {
      return;
    }
    clearHoverPreview();
    if (col < 0 || col >= gridCols || row < 0 || row >= gridRows) {
      return;
    }

    int spanCols = selectedPlaceable.colSpan(cellSize);
    int spanRows = selectedPlaceable.rowSpan(cellSize);
    Color previewColor = Color.web(selectedPlaceable.getColor(), HOVER_PREVIEW_OPACITY);

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
  public void setOnTerrainHoverEnter(Consumer<AuthoringPlaceable> handler) {
    this.onTerrainHoverEnter = handler != null ? handler : placeable -> {};
  }

  @Override
  public void setOnTerrainHoverExit(Runnable handler) {
    this.onTerrainHoverExit = handler != null ? handler : () -> {};
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

  private AuthoringCanvasObjectRenderer objectRenderer() {
    return new AuthoringCanvasObjectRenderer(cellSize, onObjectSelected, onTerrainHoverEnter,
        onTerrainHoverExit);
  }

  private void resizeRoot() {
    root.setPrefSize(gridCols * cellSize, gridRows * cellSize);
  }

  private void drawBackgroundImage() {
    if (backgroundImagePath.isBlank()) {
      return;
    }
    Image image = new Image(backgroundImagePath, false);
    if (image.isError()) {
      return;
    }
    ImageView background = new ImageView(image);
    background.setFitWidth(gridCols * cellSize);
    background.setFitHeight(gridRows * cellSize);
    background.setPreserveRatio(false);
    background.setMouseTransparent(true);
    root.getChildren().add(background);
  }
}
