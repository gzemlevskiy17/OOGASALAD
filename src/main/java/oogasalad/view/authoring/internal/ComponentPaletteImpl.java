package oogasalad.view.authoring.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import oogasalad.view.authoring.api.ComponentPallete;

/**
 * Palette implementation that reads object definitions from properties and notifies subscribers
 * when the user selects a different object type.
 */
public class ComponentPaletteImpl implements ComponentPallete {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.ComponentPalette");
  private static final ResourceBundle TERRAIN_PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.TerrainBlock");

  private final double sampleSize = d("sampleSize");
  private final Color selectedStroke = Color.web(s("selectedStroke"));
  private final double selectedStrokeWidth = d("selectedStrokeWidth");

  private final Map<String, PaletteEntry> entries = new LinkedHashMap<>();
  private final List<Consumer<AuthoringPlaceable>> subscribers = new ArrayList<>();
  private final HBox root = new HBox(d("rootSpacing"));

  private String selectedName;

  public ComponentPaletteImpl() {
    root.setPadding(new Insets(d("rootPadding")));
    loadEntries();
    buildSamples();
  }

  /**
   * Registers a subscriber that is notified whenever the selected palette item changes.
   */
  public void subscribe(Consumer<AuthoringPlaceable> listener) {
    subscribers.add(listener);
  }

  @Override
  public Node getVisualRoot() {
    return root;
  }

  private void loadEntries() {
    String[] names = PROPS.getString("objects").split(",");
    for (String name : names) {
      String color = PROPS.getString(name + ".color");
      int cols = Integer.parseInt(PROPS.getString(name + ".cols"));
      int rows = Integer.parseInt(PROPS.getString(name + ".rows"));
      String sprite = TERRAIN_PROPS.getString(name + ".sprite");
      boolean tiled = Boolean.parseBoolean(TERRAIN_PROPS.getString(name + ".tiled"));
      double friction = Double.parseDouble(TERRAIN_PROPS.getString(name + ".friction"));
      int blockCols = Integer.parseInt(TERRAIN_PROPS.getString(name + ".blockCols"));
      int blockRows = Integer.parseInt(TERRAIN_PROPS.getString(name + ".blockRows"));
      entries.put(name, new PaletteEntry(name, color, cols, rows,
          sprite, tiled, friction, blockCols, blockRows));
    }
  }

  private void buildSamples() {
    for (PaletteEntry entry : entries.values()) {
      Canvas sample = new Canvas(sampleSize, sampleSize);
      drawSample(sample, entry, false);
      sample.setOnMouseClicked(e -> select(entry.name(), sample));
      root.getChildren().add(sample);
    }
  }

  private void select(String name, Canvas clicked) {
    selectedName = name;
    PaletteEntry entry = entries.get(name);
    double cellSize = Double.parseDouble(
        ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringCanvas")
            .getString("cellSize"));
    AuthoringPlaceable placeable = new TerrainPlaceable(
        entry.blockCols() * cellSize, entry.blockRows() * cellSize,
        entry.color(), entry.sprite(), entry.tiled(), entry.friction());

    // Redraw all samplees to update selection highlight
    int i = 0;
    for (PaletteEntry e : entries.values()) {
      Canvas sample = (Canvas) root.getChildren().get(i++);
      drawSample(sample, e, e.name().equals(name));
    }

    for (Consumer<AuthoringPlaceable> sub : subscribers) {
      sub.accept(placeable);
    }
  }

  private void drawSample(Canvas sample, PaletteEntry entry, boolean selected) {
    GraphicsContext gc = sample.getGraphicsContext2D();
    gc.clearRect(0, 0, sampleSize, sampleSize);
    gc.setFill(Color.web(entry.color()));
    gc.fillRect(
        d("sampleFillInset"),
        d("sampleFillInset"),
        sampleSize - d("sampleFillInsetDouble"),
        sampleSize - d("sampleFillInsetDouble"));
    if (selected) {
      gc.setStroke(selectedStroke);
      gc.setLineWidth(selectedStrokeWidth);
      gc.strokeRect(
          d("sampleBorderInset"),
          d("sampleBorderInset"),
          sampleSize - d("sampleBorderInsetDouble"),
          sampleSize - d("sampleBorderInsetDouble"));
    }
  }

  private record PaletteEntry(String name, String color, int cols, int rows,
      String sprite, boolean tiled, double friction, int blockCols, int blockRows) {
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
