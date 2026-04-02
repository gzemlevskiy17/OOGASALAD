package oogasalad.view.authoring.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import oogasalad.view.authoring.api.AuthoringCanvas;
import oogasalad.view.authoring.api.InputMonitor;
import oogasalad.view.authoring.api.PlacedObject;
import oogasalad.view.authoring.api.PlacedObjectFactory;
import oogasalad.view.authoring.internal.DefaultPlacedObjectFactory;

public class AuthoringCanvasImpl implements AuthoringCanvas {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringCanvas");

  private final int columns = Integer.parseInt(PROPS.getString("columns"));
  private final int rows = Integer.parseInt(PROPS.getString("rows"));
  private final double cellSize = Double.parseDouble(PROPS.getString("cellSize"));
  private final int gridLineSpacing = Integer.parseInt(PROPS.getString("gridLineSpacing"));
  private final Color defaultFill = Color.web(PROPS.getString("defaultFill"));
  private final Color hoverFill = Color.web(PROPS.getString("hoverFill"));
  private final Color gridStroke = Color.web(PROPS.getString("gridStroke"));
  private final double gridStrokeWidth = Double.parseDouble(PROPS.getString("gridStrokeWidth"));

  private final Canvas canvas;
  private final Map<CellKey, PlacedObject> placedObjects;

  private InputMonitor hoverMonitor;
  private InputMonitor clickMonitor;

  private PlacedObjectFactory objectFactory = new DefaultPlacedObjectFactory(cellSize, cellSize,
      "#000000", null);

  private int hoverCol = -1;
  private int hoverRow = -1;
  private int lastDragCol = -1;
  private int lastDragRow = -1;

  /**
   * Initializes the authoring canvas with a grid and mouse interaction for hover effects.
   */
  public AuthoringCanvasImpl() {
    this.placedObjects = new LinkedHashMap<>();
    this.canvas = new Canvas(columns * cellSize, rows * cellSize);
    drawGrid();
    attachMouseHandlers();
  }

  public void setOnHover(InputMonitor monitor) {
    this.hoverMonitor = monitor;
  }

  public void setOnClick(InputMonitor monitor) {
    this.clickMonitor = monitor;
  }

  private void attachMouseHandlers() {
    canvas.setOnMouseMoved(e -> {
      int col = (int) (e.getX() / cellSize);
      int row = (int) (e.getY() / cellSize);
      if (col == hoverCol && row == hoverRow) {
        return;
      }
      clearHover();
      if (col >= 0 && col < columns && row >= 0 && row < rows) {
        hoverCol = col;
        hoverRow = row;
        drawHoverPreview(col, row);
        if (hoverMonitor != null) {
          hoverMonitor.onCellInteraction(col, row);
        }
      }
    });

    canvas.setOnMousePressed(e -> {
      int col = (int) (e.getX() / cellSize);
      int row = (int) (e.getY() / cellSize);
      lastDragCol = col;
      lastDragRow = row;
      fillSingleCell(col, row);
    });

    canvas.setOnMouseDragged(e -> {
      int col = (int) (e.getX() / cellSize);
      int row = (int) (e.getY() / cellSize);
      fillLine(lastDragCol, lastDragRow, col, row);
      lastDragCol = col;
      lastDragRow = row;
    });

    canvas.setOnMouseExited(e -> clearHover());
  }

  private void fillSingleCell(int col, int row) {
    placeBlock(col, row);
  }

  private void fillLine(int x0, int y0, int x1, int y1) {
    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    while (true) {
      fillSingleCell(x0, y0);
      if (x0 == x1 && y0 == y1) {
        break;
      }
      // Compute
      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        x0 += sx;
      }
      if (e2 < dx) {
        err += dx;
        y0 += sy;
      }
    }
  }

  private void drawHoverPreview(int col, int row) {
    int spanCols = objectFactory.colSpan(cellSize);
    int spanRows = objectFactory.rowSpan(cellSize);
    PlacedObject sample = objectFactory.create(col, row, cellSize);
    Color previewColor = Color.web(sample.getColor(), 0.5);

    for (int r = row; r < row + spanRows; r++) {
      for (int c = col; c < col + spanCols; c++) {
        if (isWithinBounds(c, r) && !isOccupied(c, r)) {
          drawCell(c, r, previewColor);
        }
      }
    }
  }

  private void clearHover() {
    if (hoverCol >= 0 && hoverRow >= 0) {
      int spanCols = objectFactory.colSpan(cellSize);
      int spanRows = objectFactory.rowSpan(cellSize);

      for (int r = hoverRow; r < hoverRow + spanRows; r++) {
        for (int c = hoverCol; c < hoverCol + spanCols; c++) {
          if (isWithinBounds(c, r)) {
            PlacedObject existing = placedObjects.get(new CellKey(c, r));
            Color restore = existing != null ? Color.web(existing.getColor()) : defaultFill;
            drawCell(c, r, restore);
            redrawGridLinesAround(c, r);
          }
        }
      }

      hoverCol = -1;
      hoverRow = -1;
    }
  }

  private boolean isWithinBounds(int col, int row) {
    return col >= 0 && col < columns && row >= 0 && row < rows;
  }

  private boolean isOccupied(int col, int row) {
    return placedObjects.containsKey(new CellKey(col, row));
  }

  private void drawCell(int col, int row, Color color) {
    GraphicsContext gc = canvas.getGraphicsContext2D();
    gc.setFill(color);
    gc.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
  }

  private void redrawGridLinesAround(int col, int row) {
    if (gridLineSpacing <= 0) {
      return;
    }
    GraphicsContext gc = canvas.getGraphicsContext2D();
    gc.setStroke(gridStroke);
    gc.setLineWidth(gridStrokeWidth);
    // Draw left line if it's a grid line
    if (col % gridLineSpacing == 0) {
      gc.strokeLine(col * cellSize, row * cellSize, col * cellSize, (row + 1) * cellSize);
    }
    // Draw right line if it's a grid line or the last column
    if ((col + 1) % gridLineSpacing == 0 || col + 1 == columns) {
      gc.strokeLine((col + 1) * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
    }
    // Draw top line if it's a grid line
    if (row % gridLineSpacing == 0) {
      gc.strokeLine(col * cellSize, row * cellSize, (col + 1) * cellSize, row * cellSize);
    }
    // Draw bottom line if it's a grid line or the last row
    if ((row + 1) % gridLineSpacing == 0 || row + 1 == rows) {
      gc.strokeLine(col * cellSize, (row + 1) * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
    }
  }

  private void drawGrid() {
    GraphicsContext gc = canvas.getGraphicsContext2D();
    gc.setFill(defaultFill);
    gc.fillRect(0, 0, columns * cellSize, rows * cellSize);
    if (gridLineSpacing <= 0) {
      return;
    }
    gc.setStroke(gridStroke);
    gc.setLineWidth(gridStrokeWidth);
    for (int col = 0; col <= columns; col += gridLineSpacing) {
      gc.strokeLine(col * cellSize, 0, col * cellSize, rows * cellSize);
    }
    for (int row = 0; row <= rows; row += gridLineSpacing) {
      gc.strokeLine(0, row * cellSize, columns * cellSize, row * cellSize);
    }
  }

  @Override
  public Node getVisualRoot() {
    return canvas;
  }

  @Override
  public Collection<PlacedObject> getPlacedObjects() {
    return List.copyOf(placedObjects.values());
  }

  @Override
  public void refreshGrid() {
    placedObjects.clear();
    hoverCol = -1;
    hoverRow = -1;
    lastDragCol = -1;
    lastDragRow = -1;
    drawGrid();
  }

  @Override
  public void setPlacedObjectFactory(PlacedObjectFactory factory) {
    this.objectFactory = factory;
  }

  @Override
  public void placeBlock(int col, int row) {
    int spanCols = objectFactory.colSpan(cellSize);
    int spanRows = objectFactory.rowSpan(cellSize);

    for (int r = row; r < row + spanRows; r++) {
      for (int c = col; c < col + spanCols; c++) {
        if (!isWithinBounds(c, r) || isOccupied(c, r)) {
          return;
        }
      }
    }

    PlacedObject block = objectFactory.create(col, row, cellSize);
    Color blockColor = Color.web(block.getColor());

    for (int r = row; r < row + spanRows; r++) {
      for (int c = col; c < col + spanCols; c++) {
        placedObjects.put(new CellKey(c, r), block);
        drawCell(c, r, blockColor);
      }
    }

    if (clickMonitor != null) {
      clickMonitor.onCellInteraction(col, row);
    }
  }

  private record CellKey(int col, int row) {
  }
}
