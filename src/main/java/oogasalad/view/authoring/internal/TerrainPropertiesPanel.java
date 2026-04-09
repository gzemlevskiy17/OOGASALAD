package oogasalad.view.authoring.internal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Side panel displaying properties of the currently selected terrain object.
 *
 * Read-only for now — editing requires World.removeObject() which is pending
 * Xuye's World interface update. Call {@link #show(AuthoringPlaceable)} when
 * the user clicks a placed block; call {@link #hide()} to clear it.
 */
public class TerrainPropertiesPanel {

  private static final double PANEL_PADDING = 16;
  private static final double FIELD_SPACING = 10;
  private static final double PANEL_WIDTH = 200;

  private final VBox root;
  private final Label titleLabel;
  private final Label textureValue;
  private final Label frictionValue;
  private final Label sizeValue;

  public TerrainPropertiesPanel() {
    titleLabel = new Label("Selected Object");
    titleLabel.getStyleClass().add("authoring-panel-title");

    textureValue = fieldValue();
    frictionValue = fieldValue();
    sizeValue = fieldValue();

    root = new VBox(FIELD_SPACING,
        titleLabel,
        fieldRow("Texture", textureValue),
        fieldRow("Friction", frictionValue),
        fieldRow("Size", sizeValue)
    );
    root.setPadding(new Insets(PANEL_PADDING));
    root.setPrefWidth(PANEL_WIDTH);
    root.getStyleClass().add("authoring-attributes-panel");
    root.setVisible(false);
    root.setManaged(false);
  }

  public Node getVisualRoot() {
    return root;
  }

  /** Populates and shows the panel with the selected object's properties. */
  public void show(AuthoringPlaceable placeable) {
    textureValue.setText(placeable.getSprite());
    frictionValue.setText(String.format("%.2f", placeable.getFriction()));
    sizeValue.setText(String.format("%.0f x %.0f", placeable.getWidth(), placeable.getHeight()));
    root.setVisible(true);
    root.setManaged(true);
  }

  /** Hides the panel (no object selected). */
  public void hide() {
    root.setVisible(false);
    root.setManaged(false);
  }

  private VBox fieldRow(String label, Label value) {
    Label key = new Label(label + ":");
    key.getStyleClass().add("authoring-field-label");
    VBox row = new VBox(2, key, value);
    row.setAlignment(Pos.TOP_LEFT);
    return row;
  }

  private Label fieldValue() {
    Label label = new Label();
    label.setWrapText(true);
    label.getStyleClass().add("authoring-field-value");
    return label;
  }
}
