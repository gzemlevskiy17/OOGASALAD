package oogasalad.view.authoring.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import oogasalad.view.authoring.api.AuthoringPlaceable;
import oogasalad.view.authoring.api.ComponentPallete;

/**
 * Palette that displays placeable items and notifies subscribers when the user selects one.
 * Items are set dynamically via {@link #setItems(List)} — typically from a {@code GameConfig}.
 *
 * @author Paulina Vvedenskaya
 */
public class ComponentPaletteImpl implements ComponentPallete {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.ComponentPalette");

  private final double sampleSize;
  private final double cellSize;
  private final Color selectedStroke;
  private final double selectedStrokeWidth;
  private final double sampleFillInset;
  private final double sampleFillInsetDouble;
  private final double sampleBorderInset;
  private final double sampleBorderInsetDouble;

  private final Map<String, PaletteEntry> entries = new LinkedHashMap<>();
  private final List<Consumer<AuthoringPlaceable>> subscribers = new ArrayList<>();
  private final HBox paletteContent = new HBox();
  private final ScrollPane root = new ScrollPane();

  private String selectedName;

  public ComponentPaletteImpl() {
    this.sampleSize = d("sampleSize");
    this.cellSize = Double.parseDouble(
        ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringCanvas")
            .getString("cellSize"));
    this.selectedStroke = Color.web(PROPS.getString("selectedStroke"));
    this.selectedStrokeWidth = d("selectedStrokeWidth");
    this.sampleFillInset = d("sampleFillInset");
    this.sampleFillInsetDouble = d("sampleFillInsetDouble");
    this.sampleBorderInset = d("sampleBorderInset");
    this.sampleBorderInsetDouble = d("sampleBorderInsetDouble");
    paletteContent.setPadding(new Insets(d("rootPadding")));
    paletteContent.setAlignment(Pos.TOP_LEFT);
    paletteContent.getStyleClass().add("authoring-level-palette");
    root.setContent(paletteContent);
    root.setFitToHeight(true);
    root.setFitToWidth(false);
    root.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    root.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    root.setPannable(true);
    root.setPrefViewportHeight(d("viewportHeight"));
    root.setMinHeight(d("viewportHeight"));
    root.getStyleClass().add("authoring-level-palette-scroll");
  }

  /**
   * Replaces the current palette contents with the given entries and rebuilds the visual samples.
   */
  public void setItems(List<PaletteEntry> items) {
    entries.clear();
    paletteContent.getChildren().clear();
    selectedName = null;
    for (PaletteEntry entry : items) {
      entries.put(entry.name(), entry);
    }
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

  private void buildSamples() {
    for (PaletteEntry entry : entries.values()) {
      Canvas sample = new Canvas(sampleSize, sampleSize);
      sample.getStyleClass().add("authoring-palette-sample");
      drawSample(sample, entry, false);
      sample.setOnMouseClicked(e -> select(entry.name(), sample));
      sample.setOnDragDetected(e -> {
        select(entry.name(), sample);
        Dragboard db = sample.startDragAndDrop(TransferMode.COPY);
        ClipboardContent cc = new ClipboardContent();
        cc.putString(entry.name());
        double targetWidth = entry.blockCols() * cellSize;
        double targetHeight = entry.blockRows() * cellSize;
        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(new Scale(targetWidth / sampleSize, targetHeight / sampleSize));
        WritableImage scaledImage = new WritableImage(
            Math.max(1, (int) Math.round(targetWidth)),
            Math.max(1, (int) Math.round(targetHeight)));
        sample.snapshot(params, scaledImage);
        db.setDragView(scaledImage, targetWidth / 2, targetHeight / 2);
        db.setContent(cc);
        e.consume();
      });
      paletteContent.getChildren().add(sample);
    }
  }

  private void select(String name, Canvas clicked) {
    selectedName = name;
    PaletteEntry entry = entries.get(name);
    AuthoringPlaceable placeable = new TerrainPlaceable(
        entry.blockCols() * cellSize, entry.blockRows() * cellSize,
        entry.color(), entry.sprite(), entry.tiled(), entry.friction(), entry.restitution());

    int i = 0;
    for (PaletteEntry e : entries.values()) {
      Canvas sample = (Canvas) paletteContent.getChildren().get(i++);
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
    gc.fillRect(sampleFillInset, sampleFillInset,
        sampleSize - sampleFillInsetDouble, sampleSize - sampleFillInsetDouble);
    if (selected) {
      gc.setStroke(selectedStroke);
      gc.setLineWidth(selectedStrokeWidth);
      gc.strokeRect(sampleBorderInset, sampleBorderInset,
          sampleSize - sampleBorderInsetDouble, sampleSize - sampleBorderInsetDouble);
    }
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }

  /**
   * Data for a single palette item.
   */
  public record PaletteEntry(String name, String color, int blockCols, int blockRows,
                             String sprite, boolean tiled, double friction, double restitution) {
  }
}
