package oogasalad.view.authoring.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import oogasalad.controller.authoring.internal.AuthoringControllerImpl;
import oogasalad.data.api.GameConfig;
import oogasalad.data.internal.LevelAttributes;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;
import oogasalad.data.internal.GameAttributes;

/**
 * Adapts the active game catalog and level attributes into authoring palette state.
 *
 * @author Emilio Garcia
 * @author Beliaev Iurii
 */
final class AuthoringPaletteCatalogAdapter {

  private static final String DEFAULT_TERRAIN_COLOR = "#8B4513";
  private static final String DEFAULT_STATIC_COLOR = "#F5C542";

  private final LevelAttributes levelAttributes;
  private final VehicleComponentScrollPalette vehiclePalette;
  private final ComponentPaletteImpl terrainPalette;
  private final ComponentPaletteImpl pigsPalette;
  private final ComponentPaletteImpl levelItemsPalette;

  private GameConfig gameConfig;
  private Consumer<String> selectionSummaryHandler = selectedItemsText -> {
  };

  AuthoringPaletteCatalogAdapter(LevelAttributes levelAttributes, AuthoringCanvasImpl canvas,
      AuthoringControllerImpl controller) {
    this.levelAttributes = Objects.requireNonNull(levelAttributes);

    vehiclePalette = new VehicleComponentScrollPalette();
    vehiclePalette.setSelectedComponentsHandler(this::applyVehicleAllowedSelection);
    vehiclePalette.setSelectionSummaryHandler(this::updateSelectedItemsSummary);
    levelAttributes.setMaxSelectable(vehiclePalette.getMaxSelectableItems());

    terrainPalette = new ComponentPaletteImpl();
    terrainPalette.setDragEnabled(false);
    pigsPalette = new ComponentPaletteImpl();
    levelItemsPalette = new ComponentPaletteImpl();
    terrainPalette.subscribe(controller::setSplineTerrainStyle);
    subscribePalette(pigsPalette, canvas, controller);
    subscribePalette(levelItemsPalette, canvas, controller);
    refreshCatalogPalettes();
  }

  void setSelectionSummaryHandler(Consumer<String> selectionSummaryHandler) {
    this.selectionSummaryHandler = selectionSummaryHandler == null
        ? selectedItemsText -> {
        } : selectionSummaryHandler;
  }

  void setGameConfig(GameConfig config) {
    gameConfig = config;
    if (config != null) {
      levelAttributes.setGameType(config.gameId());
    }
    refreshCatalogPalettes();
  }

  void syncVehiclePaletteSelection() {
    vehiclePalette.setSelectedComponentIds(levelAttributes.getMainCharacterElementsAllowed());
  }

  VehicleComponentScrollPalette vehiclePalette() {
    return vehiclePalette;
  }

  ComponentPaletteImpl terrainPalette() {
    return terrainPalette;
  }

  ComponentPaletteImpl pigsPalette() {
    return pigsPalette;
  }

  ComponentPaletteImpl levelItemsPalette() {
    return levelItemsPalette;
  }

  private void refreshCatalogPalettes() {
    if (gameConfig == null) {
      vehiclePalette.setAvailableComponentIds(List.of());
      terrainPalette.setItems(List.of());
      levelItemsPalette.setItems(List.of());
      return;
    }
    vehiclePalette.setAvailableComponentIds(gameConfig.components().stream()
        .map(component -> component.id())
        .toList());
    terrainPalette.setItems(gameConfig.terrainTypes().stream()
        .map(this::terrainEntry)
        .toList());
    levelItemsPalette.setItems(gameConfig.staticComponents().stream()
        .map(this::staticEntry)
        .toList());
  }

  private void applyVehicleAllowedSelection(List<String> selectedIds) {
    if (selectedIds == null) {
      levelAttributes.setMainCharacterElementsAllowed(List.of());
      return;
    }
    if (gameConfig == null) {
      levelAttributes.setMainCharacterElementsAllowed(selectedIds);
      return;
    }
    HashSet<String> movable = new HashSet<>(new GameAttributes(gameConfig).movableComponentIds());
    List<String> filtered = selectedIds.stream().filter(movable::contains).toList();
    levelAttributes.setMainCharacterElementsAllowed(filtered);
  }

  private void subscribePalette(ComponentPaletteImpl palette, AuthoringCanvasImpl canvas,
      AuthoringControllerImpl controller) {
    palette.subscribe(canvas::setSelectedPlaceable);
    palette.subscribe(controller::setSelectedPlaceable);
  }

  private ComponentPaletteImpl.PaletteEntry terrainEntry(TerrainInfo info) {
    String color = info.visualColor() == null || info.visualColor().isBlank()
        ? DEFAULT_TERRAIN_COLOR : info.visualColor();
    String sprite = info.spriteUrl() == null ? "" : info.spriteUrl();
    return new ComponentPaletteImpl.PaletteEntry(info.id(), color, 1, 1, sprite, true,
        info.friction(), 0.2, false);
  }

  private ComponentPaletteImpl.PaletteEntry staticEntry(StaticComponentInfo info) {
    String sprite = info.spriteUrl() == null ? "" : info.spriteUrl();
    return new ComponentPaletteImpl.PaletteEntry(info.id(), DEFAULT_STATIC_COLOR, 1, 1, sprite,
        false, 0, 0, true);
  }

  private void updateSelectedItemsSummary(String selectedItemsText) {
    selectionSummaryHandler.accept(selectedItemsText);
  }
}
