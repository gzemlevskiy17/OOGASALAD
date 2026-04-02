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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import oogasalad.view.authoring.api.ComponentPallete;
import oogasalad.view.authoring.api.PlacedObjectFactory;

/**
 * Palette implementation that reads object definitions from properties and notifies subscribers
 * when the user selects a different object type.
 */
public class ComponentPaletteImpl implements ComponentPallete {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.ComponentPalette");

  private final double sampleSize = Double.parseDouble(PROPS.getString("sampleSize"));
  private final Color selectedStroke = Color.web(PROPS.getString("selectedStroke"));
  private final double selectedStrokeWidth =
      Double.parseDouble(PROPS.getString("selectedStrokeWidth"));

  private final Map<String, PaletteEntry> entries = new LinkedHashMap<>();
  private final List<Consumer<PlacedObjectFactory>> subscribers = new ArrayList<>();
  private final VBox root = new VBox(8);

  private String selectedName;

  public ComponentPaletteImpl() {
    root.setPadding(new Insets(8));
    loadEntries();
    buildSamples();
  }

  /**
   * Registers a subscriber that is notified whenever the selected palette item changes.
   */
  public void subscribe(Consumer<PlacedObjectFactory> listener) {
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
      entries.put(name, new PaletteEntry(name, color, cols, rows));
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
    PlacedObjectFactory factory = new DefaultPlacedObjectFactory(
        entry.cols() * cellSize, entry.rows() * cellSize, entry.color(), null);

    // Redraw all samplees to update selection highlight
    int i = 0;
    for (PaletteEntry e : entries.values()) {
      Canvas sample = (Canvas) root.getChildren().get(i++);
      drawSample(sample, e, e.name().equals(name));
    }

    for (Consumer<PlacedObjectFactory> sub : subscribers) {
      sub.accept(factory);
    }
  }

  private void drawSample(Canvas sample, PaletteEntry entry, boolean selected) {
    GraphicsContext gc = sample.getGraphicsContext2D();
    gc.clearRect(0, 0, sampleSize, sampleSize);
    gc.setFill(Color.web(entry.color()));
    gc.fillRect(2, 2, sampleSize - 4, sampleSize - 4);
    if (selected) {
      gc.setStroke(selectedStroke);
      gc.setLineWidth(selectedStrokeWidth);
      gc.strokeRect(1, 1, sampleSize - 2, sampleSize - 2);
    }
  }

  private record PaletteEntry(String name, String color, int cols, int rows) {
  }
}
