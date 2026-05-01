package oogasalad.controller.player.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.model.engine.api.Driveable;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.VehicleComponent;

/**
 * Binds player-controllable drive components to gameplay input tool IDs.
 *
 * <p>Multiple components with the same display name share one tool ID. For example, a vehicle with
 * two fans should turn both fans on when the player presses the fan control, rather than replacing
 * the first binding with the second one.
 *
 * @author Paulina Vvedenskaya
 */
final class DriveActionBinder {

  Set<String> collectToolIds(World world) {
    return collectToolIds(world, Set.of());
  }
  /**
   * @param skipNormalizedIds catalog component ids (normalized) that have authored buttons;
   * drives for those ids are omitted so ButtonForceController owns the UI.
   */
  Set<String> collectToolIds(World world, Set<String> skipNormalizedIds) {
    Set<String> skip = skipNormalizedIds == null ? Set.of() : skipNormalizedIds;
    Set<String> ids = new HashSet<>();
    for (GameObject obj : world.getObjects()) {
      if (!isControllableDrive(obj)) {
        continue;
      }
      if (skip.contains(normalizeKey(driveButtonId(obj)))) {
        continue;
      }
      ids.add(driveButtonId(obj));
    }
    return ids;
  }

  /**
   * Registers drive actions on the input router for every controllable drive component.
   *
   * @param inputRouter router receiving tool button bindings
   * @param world gameplay world after the vehicle has been built
   */
  void bind(InputRouter inputRouter, World world) {
    bind(inputRouter, world, Set.of());
  }
  void bind(InputRouter inputRouter, World world, Set<String> skipNormalizedIds) {
    Set<String> skip = skipNormalizedIds == null ? Set.of() : skipNormalizedIds;
    Map<String, List<Driveable>> drivesById = new LinkedHashMap<>();
    for (GameObject obj : world.getObjects()) {
      if (!(obj instanceof Driveable driveable) || !driveable.isPlayerControllable()) {
        continue;
      }
      if (skip.contains(normalizeKey(driveButtonId(obj)))) {
        continue;
      }
      drivesById.computeIfAbsent(driveButtonId(obj), ignored -> new ArrayList<>()).add(driveable);
    }
    for (Map.Entry<String, List<Driveable>> entry : drivesById.entrySet()) {
      inputRouter.bind(entry.getKey(), activateAction(entry.getValue()));
      inputRouter.bindRelease(entry.getKey(), deactivateAction(entry.getValue()));
    }
  }

  private String normalizeKey(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private boolean isControllableDrive(GameObject obj) {
    return obj instanceof Driveable driveable && driveable.isPlayerControllable();
  }

  private Action activateAction(List<Driveable> driveables) {
    return () -> {
      for (Driveable driveable : driveables) {
        driveable.activateDrive();
      }
    };
  }

  private Action deactivateAction(List<Driveable> driveables) {
    return () -> {
      for (Driveable driveable : driveables) {
        driveable.deactivateDrive();
      }
    };
  }

  private String driveButtonId(GameObject obj) {
    if (obj == null) {
      return "";
    }
    if (obj instanceof VehicleComponent vc) {
      String displayName = vc.getDisplayName();
      if (displayName != null && !displayName.isBlank()) {
        return displayName.toLowerCase();
      }
    }
    String tag = obj.getTag();
    return tag == null ? "" : tag;
  }
}
