package oogasalad.view.authoring.internal.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.data.api.TerrainInfo;

/**
 * Validation rules for authored game catalogs before they are written to disk or used for a level.
 */
final class GameCatalogValidator {

  private static final Set<String> STATIC_ACTION_TYPES =
      Set.of("heal", "damage", "score", "finish");

  void validate(String gameId, String mainCharacterId, GameCatalogDraft draft) {
    validateGameId(gameId);
    if (draft.components().isEmpty()) {
      throw new IllegalArgumentException("At least one vehicle component is required.");
    }
    validateComponentIds(draft.components());
    validateStaticIds(draft.staticComponents());
    validateTerrainIds(draft.terrainTypes());
    validateMainCharacterId(mainCharacterId, draft.components());
    validateComponents(draft.components());
    validateStaticComponents(draft.staticComponents());
    validateTerrainTypes(draft.terrainTypes());
  }

  private void validateComponentIds(List<ComponentInfo> components) {
    Set<String> ids = new HashSet<>();
    for (ComponentInfo component : components) {
      String id = requireId(component.id(), "Component ID");
      if (!ids.add(id)) {
        throw new IllegalArgumentException("Duplicate component ID: " + id);
      }
    }
  }

  private void validateStaticIds(List<StaticComponentInfo> staticComponents) {
    Set<String> ids = new HashSet<>();
    for (StaticComponentInfo component : staticComponents) {
      String id = requireId(component.id(), "Static object ID");
      if (!ids.add(id)) {
        throw new IllegalArgumentException("Duplicate static object ID: " + id);
      }
    }
  }

  private void validateTerrainIds(List<TerrainInfo> terrainTypes) {
    Set<String> ids = new HashSet<>();
    for (TerrainInfo terrain : terrainTypes) {
      String id = requireId(terrain.id(), "Terrain ID");
      if (!ids.add(id)) {
        throw new IllegalArgumentException("Duplicate terrain ID: " + id);
      }
    }
  }

  private void validateMainCharacterId(String mainCharacterId, List<ComponentInfo> components) {
    String id = requireId(mainCharacterId, "Main Character");
    boolean exists = components.stream().anyMatch(component -> id.equals(component.id()));
    if (!exists) {
      throw new IllegalArgumentException("Main Character must be one of the vehicle components.");
    }
  }

  private void validateComponents(List<ComponentInfo> components) {
    for (ComponentInfo component : components) {
      requirePositiveFinite(component.mass(), "Component '" + component.id() + "' mass");
      component.buttons().forEach(button -> {
        requireId(button.name(), "Button name for component '" + component.id() + "'");
        requireFinite(button.directionX(), "Button '" + button.name() + "' direction X");
        requireFinite(button.directionY(), "Button '" + button.name() + "' direction Y");
      });
    }
  }

  private void validateStaticComponents(List<StaticComponentInfo> staticComponents) {
    for (StaticComponentInfo component : staticComponents) {
      requireNonNegativeFinite(component.mass(), "Static object '" + component.id() + "' mass");
      validateStaticAction(component.id(), component.onCollisionAction());
    }
  }

  private void validateStaticAction(String id, CollisionAction action) {
    if (action == null) {
      return;
    }
    if (!STATIC_ACTION_TYPES.contains(action.type())) {
      throw new IllegalArgumentException(
          "Static object '" + id + "' has unsupported action type: " + action.type());
    }
    requireNonNegativeFinite(action.amount(), "Static object '" + id + "' action amount");
    if (!"finish".equals(action.type()) && action.amount() <= 0) {
      throw new IllegalArgumentException(
          "Static object '" + id + "' action amount must be positive.");
    }
  }

  private void validateTerrainTypes(List<TerrainInfo> terrainTypes) {
    for (TerrainInfo terrain : terrainTypes) {
      requireNonNegativeFinite(terrain.friction(), "Terrain '" + terrain.id() + "' friction");
      if ((terrain.spriteUrl() == null || terrain.spriteUrl().isBlank())
          && (terrain.visualColor() == null || terrain.visualColor().isBlank())) {
        throw new IllegalArgumentException(
            "Terrain '" + terrain.id() + "' needs either a sprite or visual color.");
      }
    }
  }

  private String requireId(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required.");
    }
    return value.strip();
  }

  private void validateGameId(String gameId) {
    String id = requireId(gameId, "Game ID");
    if (id.contains("/") || id.contains("\\") || id.contains("..")) {
      throw new IllegalArgumentException(
          "Game ID cannot contain path separators or '..'.");
    }
  }

  private void requirePositiveFinite(double value, String label) {
    requireFinite(value, label);
    if (value <= 0) {
      throw new IllegalArgumentException(label + " must be greater than 0.");
    }
  }

  private void requireNonNegativeFinite(double value, String label) {
    requireFinite(value, label);
    if (value < 0) {
      throw new IllegalArgumentException(label + " must be non-negative.");
    }
  }

  private void requireFinite(double value, String label) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(label + " must be a finite number.");
    }
  }
}
