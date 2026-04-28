package oogasalad.view.authoring.internal;

import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.TerrainBlock;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Side panel displaying and editing properties of the currently selected terrain object.
 * Edits are applied by replacing the immutable block in the world.
 */
public class TerrainPropertiesPanel {

  private static final double PANEL_PADDING = 16;
  private static final double FIELD_SPACING = 10;
  private static final double PANEL_WIDTH = 220;
  private static final String PLACEHOLDER = "\u2014";

  private final VBox root;
  private final Label nameValue;
  private final Label typeValue;
  private final Label positionValue;
  private final Label sizeValue;
  private final Label textureValue;
  private final TextField frictionField;
  private final TextField restitutionField;

  private AuthoringPlaceable selected;
  private World world;
  private Runnable refreshCallback;

  public TerrainPropertiesPanel() {
    Label titleLabel = new Label("Selected Object");
    titleLabel.getStyleClass().add("authoring-panel-title");

    nameValue = fieldLabel();
    typeValue = fieldLabel();
    positionValue = fieldLabel();
    sizeValue = fieldLabel();
    textureValue = fieldLabel();
    frictionField = editableField(this::onFrictionCommit);
    restitutionField = editableField(this::onRestitutionCommit);
    AuthoringValueHints.attach(frictionField, "inspector.terrain.friction");
    AuthoringValueHints.attach(restitutionField, "inspector.terrain.restitution");

    root = new VBox(FIELD_SPACING,
        titleLabel,
        fieldRow("Name", nameValue),
        fieldRow("Type", typeValue),
        fieldRow("Position", positionValue),
        fieldRow("Size", sizeValue),
        fieldRow("Texture", textureValue),
        editableRow("Friction", frictionField),
        editableRow("Restitution", restitutionField)
    );
    root.setPadding(new Insets(PANEL_PADDING));
    root.setPrefWidth(PANEL_WIDTH);
    root.getStyleClass().add("authoring-attributes-panel");
    root.setVisible(true);
    root.setManaged(true);
    showPlaceholder();
  }

  /**
   * Provides the world and a refresh callback so edits can replace objects in place.
   */
  public void setEditContext(World world, Runnable refreshCallback) {
    this.world = world;
    this.refreshCallback = refreshCallback;
  }

  public Node getVisualRoot() {
    return root;
  }

  public void show(AuthoringPlaceable placeable) {
    this.selected = placeable;
    nameValue.setText(placeable.getDisplayName());
    typeValue.setText(placeable.getClass().getSimpleName());
    positionValue.setText(String.format("(%d, %d)", (int) placeable.x(), (int) placeable.y()));
    sizeValue.setText(String.format("%.0f \u00d7 %.0f", placeable.getWidth(), placeable.getHeight()));
    String sprite = placeable.getSprite();
    textureValue.setText(sprite == null || sprite.isBlank() ? "none" : sprite);
    frictionField.setText(String.format("%.2f", placeable.getFriction()));
    restitutionField.setText(String.format("%.2f", placeable.getRestitution()));
    frictionField.setDisable(false);
    restitutionField.setDisable(false);
  }

  public void hide() {
    this.selected = null;
    showPlaceholder();
  }

  private void showPlaceholder() {
    nameValue.setText(PLACEHOLDER);
    typeValue.setText(PLACEHOLDER);
    positionValue.setText(PLACEHOLDER);
    sizeValue.setText(PLACEHOLDER);
    textureValue.setText(PLACEHOLDER);
    frictionField.setText("");
    restitutionField.setText("");
    frictionField.setDisable(true);
    restitutionField.setDisable(true);
  }

  private void onFrictionCommit(String raw) {
    if (selected == null || world == null) {
      return;
    }
    double val = parseOrReset(raw, selected.getFriction(), frictionField);
    if (val == selected.getFriction()) {
      return;
    }
    replaceBlock(selected, val, selected.getRestitution());
  }

  private void onRestitutionCommit(String raw) {
    if (selected == null || world == null) {
      return;
    }
    double val = parseOrReset(raw, selected.getRestitution(), restitutionField);
    if (val == selected.getRestitution()) {
      return;
    }
    replaceBlock(selected, selected.getFriction(), val);
  }

  private void replaceBlock(AuthoringPlaceable old, double friction, double restitution) {
    world.removeObject((GameObject) old);
    TerrainBlock replacement = new TerrainBlock(
        old.getDisplayName(), old.x(), old.y(),
        old.getWidth(), old.getHeight(),
        old.getSprite(), old.isTiled(),
        friction, old.getColor(), restitution);
    world.addObject(replacement);
    selected = replacement;
    if (refreshCallback != null) {
      refreshCallback.run();
    }
    show(replacement);
  }

  private double parseOrReset(String raw, double fallback, TextField field) {
    try {
      return Double.parseDouble(raw.trim());
    } catch (NumberFormatException e) {
      field.setText(String.format("%.2f", fallback));
      return fallback;
    }
  }

  private VBox fieldRow(String label, Label value) {
    Label key = new Label(label + ":");
    key.getStyleClass().add("authoring-field-label");
    VBox row = new VBox(2, key, value);
    row.setAlignment(Pos.TOP_LEFT);
    return row;
  }

  private VBox editableRow(String label, TextField field) {
    Label key = new Label(label + ":");
    key.getStyleClass().add("authoring-field-label");
    VBox row = new VBox(2, key, field);
    row.setAlignment(Pos.TOP_LEFT);
    return row;
  }

  private Label fieldLabel() {
    Label label = new Label();
    label.setWrapText(true);
    label.getStyleClass().add("authoring-field-value");
    return label;
  }

  private TextField editableField(Consumer<String> onCommit) {
    TextField field = new TextField();
    field.setPrefWidth(PANEL_WIDTH - PANEL_PADDING * 2);
    field.getStyleClass().add("authoring-field-input");
    field.setOnAction(e -> onCommit.accept(field.getText()));
    field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
      if (!isFocused) {
        onCommit.accept(field.getText());
      }
    });
    return field;
  }
}
