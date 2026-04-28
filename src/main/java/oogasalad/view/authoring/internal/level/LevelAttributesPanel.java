package oogasalad.view.authoring.internal.level;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import oogasalad.data.internal.LevelAttributes;
import oogasalad.view.authoring.internal.common.AuthoringValueHints;

/**
 * Side panel for editing global level attributes (name, gravity, drag, maxHP).
 * Binds directly to a {@link LevelAttributes} model so edits are picked up by save immediately.
 *
 * @author Paulina Vvedenskaya
 */
public class LevelAttributesPanel {

  private static final double PANEL_PADDING = 16;
  private static final double FIELD_SPACING = 12;
  private static final double LABEL_WIDTH = 120;
  private static final double FIELD_WIDTH = 180;
  private static final double GRAVITY_MIN = -50.0;
  private static final double GRAVITY_MAX = 0.0;
  private static final double GRAVITY_STEP = 0.1;
  private static final double DRAG_MIN = 0.0;
  private static final double DRAG_MAX = 1.0;
  private static final double DRAG_STEP = 0.001;
  private static final double HP_MIN = 1.0;
  private static final double HP_MAX = 9999.0;
  private static final double HP_STEP = 10.0;

  private final VBox root;
  private final LevelAttributes model;

  public LevelAttributesPanel(LevelAttributes model) {
    this.model = model;
    this.root = new VBox(FIELD_SPACING);
    root.setPadding(new Insets(PANEL_PADDING));
    root.getStyleClass().add("authoring-attributes-panel");
    root.getChildren().addAll(
        buildTitle(),
        buildNameRow(),
        buildGravityRow(),
        buildDragRow(),
        buildMaxHPRow()
    );
  }

  public Node getVisualRoot() {
    return root;
  }

  private Label buildTitle() {
    Label title = new Label("Level Settings");
    title.getStyleClass().add("authoring-panel-title");
    return title;
  }

  private HBox buildNameRow() {
    TextField field = new TextField(model.getLevelName());
    field.setPrefWidth(FIELD_WIDTH);
    AuthoringValueHints.configure(field, "level.name");
    field.textProperty().addListener((obs, oldVal, newVal) -> model.setLevelName(newVal));
    return labeledRow("Level Name", field);
  }

  private HBox buildGravityRow() {
    SpinnerValueFactory.DoubleSpinnerValueFactory factory =
        new SpinnerValueFactory.DoubleSpinnerValueFactory(GRAVITY_MIN, GRAVITY_MAX,
            model.getGravity(), GRAVITY_STEP);
    Spinner<Double> spinner = new Spinner<>(factory);
    spinner.setEditable(true);
    spinner.setPrefWidth(FIELD_WIDTH);
    AuthoringValueHints.attach(spinner, "level.gravity");
    spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        model.setGravity(newVal);
      }
    });
    return labeledRow("Gravity", spinner);
  }

  private HBox buildDragRow() {
    Spinner<Double> spinner = new Spinner<>(
        new SpinnerValueFactory.DoubleSpinnerValueFactory(DRAG_MIN, DRAG_MAX,
            model.getDrag(), DRAG_STEP));
    spinner.setEditable(true);
    spinner.setPrefWidth(FIELD_WIDTH);
    AuthoringValueHints.attach(spinner, "level.drag");
    spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        model.setDrag(newVal);
      }
    });
    return labeledRow("Drag", spinner);
  }

  private HBox buildMaxHPRow() {
    Spinner<Double> spinner = new Spinner<>(
        new SpinnerValueFactory.DoubleSpinnerValueFactory(HP_MIN, HP_MAX,
            model.getMaxHP(), HP_STEP));
    spinner.setEditable(true);
    spinner.setPrefWidth(FIELD_WIDTH);
    AuthoringValueHints.attach(spinner, "level.maxHp");
    spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        model.setMaxHP(newVal);
      }
    });
    return labeledRow("Max HP", spinner);
  }

  private HBox labeledRow(String labelText, Node field) {
    Label label = new Label(labelText + ":");
    label.setPrefWidth(LABEL_WIDTH);
    label.setAlignment(Pos.CENTER_LEFT);
    label.getStyleClass().add("authoring-field-label");
    HBox row = new HBox(FIELD_SPACING, label, field);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }
}
