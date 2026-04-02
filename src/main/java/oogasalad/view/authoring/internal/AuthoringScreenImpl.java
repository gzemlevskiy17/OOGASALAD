package oogasalad.view.authoring.internal;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import oogasalad.view.core.api.AppScreen;

/**
 * Composite view that arranges the authoring canvas above the component palette bar.
 * Each section is wrapped in a gray bordered box with padding between them.
 */
public class AuthoringScreenImpl implements AppScreen {

  private static final double GAP = 10;
  private static final double SECTION_PADDING = 8;
  private static final Color SECTION_BORDER_COLOR = Color.GRAY;
  private static final Color SECTION_BACKGROUND = Color.web("#F5F5F5");

  private final AuthoringCanvasImpl canvas;
  private final ComponentPaletteImpl palette;
  private final VBox root;

  public AuthoringScreenImpl() {
    canvas = new AuthoringCanvasImpl();
    palette = new ComponentPaletteImpl();

    palette.subscribe(canvas::setPlacedObjectFactory);

    Node canvasSection = wrapInSection(canvas.getVisualRoot());
    Node paletteSection = wrapInSection(palette.getVisualRoot());

    root = new VBox(GAP, canvasSection, paletteSection);
    root.setPadding(new Insets(GAP));
  }

  @Override
  public Parent getRoot() {
    return root;
  }

  public AuthoringCanvasImpl getCanvas() {
    return canvas;
  }

  public ComponentPaletteImpl getPalette() {
    return palette;
  }

  private Node wrapInSection(Node content) {
    StackPane section = new StackPane(content);
    section.setPadding(new Insets(SECTION_PADDING));
    section.setBorder(new Border(new BorderStroke(
        SECTION_BORDER_COLOR, BorderStrokeStyle.SOLID, new CornerRadii(4),
        new BorderWidths(1))));
    section.setBackground(new Background(
        new BackgroundFill(SECTION_BACKGROUND, new CornerRadii(4), Insets.EMPTY)));
    return section;
  }
}
