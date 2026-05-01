package oogasalad.view.authoring.internal.level;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * Draws the authoring canvas grid.
 */
class AuthoringCanvasGridRenderer {

  private final double cellSize;
  private final int gridLineSpacing;
  private final Color gridStroke;
  private final double gridStrokeWidth;

  AuthoringCanvasGridRenderer(double cellSize, int gridLineSpacing, Color gridStroke,
      double gridStrokeWidth) {
    this.cellSize = cellSize;
    this.gridLineSpacing = gridLineSpacing;
    this.gridStroke = gridStroke;
    this.gridStrokeWidth = gridStrokeWidth;
  }

  void drawGridLines(Pane root, int gridCols, int gridRows) {
    if (gridLineSpacing <= 0) {
      return;
    }
    double totalWidth = gridCols * cellSize;
    double totalHeight = gridRows * cellSize;
    drawVerticalLines(root, gridCols, totalHeight);
    drawHorizontalLines(root, gridRows, totalWidth);
  }

  private void drawVerticalLines(Pane root, int gridCols, double totalHeight) {
    for (int col = 0; col <= gridCols; col += gridLineSpacing) {
      Line line = new Line(col * cellSize, 0, col * cellSize, totalHeight);
      styleLine(line);
      root.getChildren().add(line);
    }
  }

  private void drawHorizontalLines(Pane root, int gridRows, double totalWidth) {
    for (int row = 0; row <= gridRows; row += gridLineSpacing) {
      Line line = new Line(0, row * cellSize, totalWidth, row * cellSize);
      styleLine(line);
      root.getChildren().add(line);
    }
  }

  private void styleLine(Line line) {
    line.setStroke(gridStroke);
    line.setStrokeWidth(gridStrokeWidth);
  }
}
