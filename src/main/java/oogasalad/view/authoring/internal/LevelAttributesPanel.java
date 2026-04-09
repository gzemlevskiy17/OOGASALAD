package oogasalad.view.authoring.internal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import oogasalad.data.api.LevelAttributes;

/**
 * Side panel for editing global level attributes: name, gravity, background image.
 *
 * Binds directly to a {@link LevelAttributes} model — any edit immediately
 * updates the model so save picks up the latest values without a confirm step.
 */
public class LevelAttributesPanel {

  private static final double PANEL_PADDING = 16;
  private static final double FIELD_SPACING = 12;
  private static final double LABEL_WIDTH = 120;
  private static final double FIELD_WIDTH = 200;
  private static final double GRAVITY_MIN = -50.0;
  private static final double GRAVITY_MAX = 0.0;
  private static final double GRAVITY_STEP = 0.1;
  private static final int SPINNER_COLS = 6;

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
        buildBackgroundRow()
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
    spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) model.setGravity(newVal);
    });
    return labeledRow("Gravity", spinner);
  }

  private HBox buildBackgroundRow() {
    TextField field = new TextField(model.getBackgroundImage());
    field.setPrefWidth(FIELD_WIDTH);
    field.setPromptText("assets/backgrounds/sky.png");
    field.textProperty().addListener((obs, oldVal, newVal) -> model.setBackgroundImage(newVal));
    return labeledRow("Background", field);
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
