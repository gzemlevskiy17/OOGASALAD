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
 * Palette implementation that reads object definitions from properties and notifies subscribers
 * when the user selects a different object type.
 */
public class ComponentPaletteImpl implements ComponentPallete {

  private static final ResourceBundle TERRAIN_PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.TerrainBlock");

  private final ResourceBundle props;
  private final double sampleSize;
  private final double cellSize;
  private final Color selectedStroke;
  private final double selectedStrokeWidth;

  private final Map<String, PaletteEntry> entries = new LinkedHashMap<>();
  private final List<Consumer<AuthoringPlaceable>> subscribers = new ArrayList<>();
  private final HBox paletteContent = new HBox();
  private final ScrollPane root = new ScrollPane();

  private String selectedName;

  public ComponentPaletteImpl() {
    this("oogasalad.view.authoring.ComponentPalette");
  }

  public ComponentPaletteImpl(String bundleName) {
    this.props = ResourceBundle.getBundle(bundleName);
    this.sampleSize = d("sampleSize");
    this.cellSize = Double.parseDouble(
        ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringCanvas")
            .getString("cellSize"));
    this.selectedStroke = Color.web(s("selectedStroke"));
    this.selectedStrokeWidth = d("selectedStrokeWidth");
    paletteContent.setPadding(new Insets(d("rootPadding")));
    paletteContent.setAlignment(Pos.TOP_LEFT);
    paletteContent.getStyleClass().add("authoring-level-palette");
    loadEntries();
    buildSamples();
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
    String[] names = props.getString("objects").split(",");
    for (String name : names) {
      if (name.isBlank()) {
        continue;
      }
      String color = props.getString(name + ".color");
      int cols = Integer.parseInt(props.getString(name + ".cols"));
      int rows = Integer.parseInt(props.getString(name + ".rows"));
      String sprite = TERRAIN_PROPS.getString(name + ".sprite");
      boolean tiled = Boolean.parseBoolean(TERRAIN_PROPS.getString(name + ".tiled"));
      double friction = Double.parseDouble(TERRAIN_PROPS.getString(name + ".friction"));
      double restitution = Double.parseDouble(TERRAIN_PROPS.getString(name + ".restitution"));
      int blockCols = Integer.parseInt(TERRAIN_PROPS.getString(name + ".blockCols"));
      int blockRows = Integer.parseInt(TERRAIN_PROPS.getString(name + ".blockRows"));
      entries.put(name, new PaletteEntry(name, color, cols, rows,
          sprite, tiled, friction, restitution, blockCols, blockRows));
    }
  }

  private void buildSamples() {
    for (PaletteEntry entry : entries.values()) {
      Canvas sample = new Canvas(sampleSize, sampleSize);
      sample.getStyleClass().add("authoring-palette-sample");
      drawSample(sample, entry, false);
      sample.setOnMouseClicked(e -> select(entry.name(), sample));
      // Drag-from-palette: select on drag start so subscribers (canvas + controller)
      // pick up the new placeable before the drop fires, then start a real
      // Dragboard gesture so the cursor shows a preview and the canvas's
      // setOnDragDropped handler can place a single block on release.
      sample.setOnDragDetected(e -> {
        select(entry.name(), sample);
        Dragboard db = sample.startDragAndDrop(TransferMode.COPY);
        ClipboardContent cc = new ClipboardContent();
        cc.putString(entry.name());
        // Scale the sample snapshot down to the actual block pixel size on the
        // canvas (blockCols * cellSize x blockRows * cellSize) so the cursor
        // preview matches the placed block instead of the larger palette sample.
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

    // Redraw all samplees to update selection highlight
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
                              String sprite, boolean tiled, double friction, double restitution,
                              int blockCols, int blockRows) {
  }

  private String s(String key) {
    return props.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(props.getString(key));
  }
}
