package oogasalad.view.authoring.internal.level;

import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import oogasalad.controller.authoring.api.ToolSelector.ToolType;
import oogasalad.controller.authoring.internal.AuthoringControllerImpl;

/**
 * Coordinates authoring mode, palette visibility, and active tool button styling.
 *
 * @author Emilio Garcia
 * @author Beliaev Iurii
 */
final class AuthoringModeController {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringScreen");

  private enum AuthoringMode {
    LEVEL,
    VEHICLE
  }

  private enum LevelPaletteMode {
    TERRAIN,
    PIGS,
    LEVEL_ITEMS,
    SETTINGS
  }

  private final AuthoringScreenLayoutFactory.Parts parts;
  private final AuthoringPaletteCatalogAdapter palettes;
  private final LevelAttributesPanel levelAttributesPanel;
  private final List<Button> toolButtons;

  private AuthoringMode currentMode = AuthoringMode.LEVEL;
  private LevelPaletteMode currentLevelPaletteMode = LevelPaletteMode.TERRAIN;

  AuthoringModeController(AuthoringScreenLayoutFactory.Parts parts,
      AuthoringPaletteCatalogAdapter palettes, LevelAttributesPanel levelAttributesPanel) {
    this.parts = parts;
    this.palettes = palettes;
    this.levelAttributesPanel = levelAttributesPanel;
    toolButtons = List.of(parts.selectButton(), parts.splineButton());
  }

  void wireHandlers(AuthoringControllerImpl controller) {
    parts.levelModeButton().setOnAction(event -> {
      currentMode = AuthoringMode.LEVEL;
      refreshDisplayedMode();
    });
    parts.vehicleModeButton().setOnAction(event -> {
      currentMode = AuthoringMode.VEHICLE;
      refreshDisplayedMode();
    });
    parts.terrainPaletteButton().setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.TERRAIN;
      refreshDisplayedMode();
    });
    parts.pigsPaletteButton().setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.PIGS;
      refreshDisplayedMode();
    });
    parts.levelItemsPaletteButton().setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.LEVEL_ITEMS;
      refreshDisplayedMode();
    });
    parts.settingsPaletteButton().setOnAction(event -> {
      currentLevelPaletteMode = LevelPaletteMode.SETTINGS;
      refreshDisplayedMode();
    });
    parts.selectButton().getStyleClass().add("tool-active");
    parts.selectButton().setOnAction(event -> {
      controller.setActiveTool(ToolType.SELECT);
      updateToolButtonStyles(parts.selectButton());
    });
    parts.splineButton().setOnAction(event -> {
      controller.setActiveTool(ToolType.SPLINE);
      updateToolButtonStyles(parts.splineButton());
    });
  }

  void refreshDisplayedMode() {
    boolean isLevelMode = currentMode == AuthoringMode.LEVEL;
    parts.levelPaletteTabs().setVisible(isLevelMode);
    parts.levelPaletteTabs().setManaged(isLevelMode);
    parts.selectedItemsLabel().setVisible(!isLevelMode);
    parts.selectedItemsLabel().setManaged(!isLevelMode);

    parts.bottomContentHost().getChildren().clear();
    Node paletteNode;
    Pos alignment = Pos.TOP_LEFT;
    if (currentMode == AuthoringMode.VEHICLE) {
      paletteNode = palettes.vehiclePalette().getVisualRoot();
      alignment = Pos.CENTER;
    } else {
      paletteNode = switch (currentLevelPaletteMode) {
        case TERRAIN -> palettes.terrainPalette().getVisualRoot();
        case PIGS -> palettes.pigsPalette().getVisualRoot();
        case LEVEL_ITEMS -> palettes.levelItemsPalette().getVisualRoot();
        case SETTINGS -> levelAttributesPanel.getVisualRoot();
      };
    }
    StackPane.setAlignment(paletteNode, alignment);
    parts.bottomContentHost().getChildren().add(paletteNode);
  }

  void updateSelectedItemsSummary(String selectedItemsText) {
    parts.selectedItemsLabel().setText(String.format(
        s("selectedItemsSummaryFormat"),
        selectedItemsText));
  }

  private void updateToolButtonStyles(Button activeButton) {
    for (Button button : toolButtons) {
      button.getStyleClass().remove("tool-active");
    }
    activeButton.getStyleClass().add("tool-active");
  }

  private String s(String key) {
    return PROPS.getString(key);
  }
}
