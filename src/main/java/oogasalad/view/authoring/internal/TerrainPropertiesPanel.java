package oogasalad.view.authoring.internal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import oogasalad.model.engine.internal.components.TerrainSpline;
import oogasalad.view.authoring.api.AuthoringPlaceable;

/**
 * Right sidebar: catalog-backed terrain name, texture, friction, and restitution for the brush
 * (palette hover), canvas hover, and click selection.
 */
public class TerrainPropertiesPanel {

  private static final double PANEL_PADDING = 16;
  private static final double FIELD_SPACING = 10;
  private static final double PANEL_WIDTH = 220;

  private final VBox root;
  private final Label nameLabel;
  private final Label textureValue;
  private final Label frictionValue;
  private final Label restitutionValue;
  private final Label sizeValue;

  private AuthoringPlaceable canvasSelection;
  private AuthoringPlaceable canvasHoverTarget;
  private CatalogTerrainInfo paletteCatalogHover;

  public TerrainPropertiesPanel() {
    nameLabel = new Label("—");
    nameLabel.getStyleClass().add("authoring-terrain-name");

    textureValue = fieldValue();
    frictionValue = fieldValue();
    restitutionValue = fieldValue();
    sizeValue = fieldValue();

    root = new VBox(FIELD_SPACING,
        sectionTitle("Terrain"),
        nameLabel,
        fieldRow("Texture", textureValue),
        fieldRow("Friction", frictionValue),
        fieldRow("Restitution", restitutionValue),
        fieldRow("Size (cells)", sizeValue)
    );
    root.setPadding(new Insets(PANEL_PADDING));
    root.setPrefWidth(PANEL_WIDTH);
    root.getStyleClass().add("authoring-attributes-panel");
    root.setVisible(true);
    root.setManaged(true);
    showPlaceholder();
  }

  public Node getVisualRoot() {
    return root;
  }

  public void show(AuthoringPlaceable placeable) {
    canvasSelection = placeable;
    refreshDisplay();
  }

  public void hide() {
    canvasSelection = null;
    canvasHoverTarget = null;
    paletteCatalogHover = null;
    showPlaceholder();
  }

  public void showCatalogHover(CatalogTerrainInfo info) {
    paletteCatalogHover = info;
    refreshDisplay();
  }

  public void endCatalogHover() {
    paletteCatalogHover = null;
    refreshDisplay();
  }

  public void onCanvasTerrainHover(AuthoringPlaceable placeable) {
    canvasHoverTarget = placeable;
    refreshDisplay();
  }

  public void onCanvasTerrainHoverEnd() {
    canvasHoverTarget = null;
    refreshDisplay();
  }

  private void refreshDisplay() {
    if (paletteCatalogHover != null) {
      applyCatalog(paletteCatalogHover);
      return;
    }
    if (canvasHoverTarget != null) {
      applyPlaceable(canvasHoverTarget);
      return;
    }
    if (canvasSelection != null) {
      applyPlaceable(canvasSelection);
      return;
    }
    showPlaceholder();
  }

  private void showPlaceholder() {
    nameLabel.setText("—");
    textureValue.setText("—");
    frictionValue.setText("—");
    restitutionValue.setText("—");
    sizeValue.setText("—");
  }

  private void applyCatalog(CatalogTerrainInfo c) {
    nameLabel.setText(c.catalogName());
    textureValue.setText(shortenSprite(c.sprite()));
    frictionValue.setText(String.format("%.2f", c.friction()));
    restitutionValue.setText(String.format("%.2f", c.restitution()));
    sizeValue.setText("—");
  }

  private void applyPlaceable(AuthoringPlaceable p) {
    nameLabel.setText(displayName(p));
    textureValue.setText(shortenSprite(p.getSprite()));
    frictionValue.setText(String.format("%.2f", p.getFriction()));
    restitutionValue.setText(String.format("%.2f", p.getRestitution()));
    if (p instanceof TerrainSpline) {
      sizeValue.setText("—");
    } else {
      sizeValue.setText(
          String.format("%.0f x %.0f", p.getWidth(), p.getHeight()));
    }
  }

  private static String displayName(AuthoringPlaceable p) {
    String n = p.getCatalogName();
    if (n != null && !n.isBlank()) {
      return n;
    }
    if (p instanceof TerrainSpline) {
      return "Spline";
    }
    return "Terrain";
  }

  private static String shortenSprite(String path) {
    if (path == null || path.isBlank()) {
      return "—";
    }
    int slash = path.lastIndexOf('/');
    return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
  }

  private Label sectionTitle(String text) {
    Label l = new Label(text);
    l.getStyleClass().add("authoring-panel-title");
    return l;
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
