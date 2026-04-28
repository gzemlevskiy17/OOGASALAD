package oogasalad.controller.player.internal;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import oogasalad.controller.player.api.InputRouter;
import oogasalad.data.api.ButtonInfo;
import oogasalad.data.api.ComponentInfo;
import oogasalad.data.api.GameConfig;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.TypedComponent;
import oogasalad.model.engine.api.Vector;
import oogasalad.model.engine.api.World;
import oogasalad.model.engine.internal.components.DynamicComponent;

/**
 * Creates per-ButtonInfo toolbar entries and binds authored force vectors to world objects.
 *
 * @author Paulina Vvedenskaya
 */
final class ButtonForceController {

  Set<String> collectButtonIds(GameConfig config, World world) {
    Set<String> ids = new HashSet<>();
    if (config == null) {
      return ids;
    }
    Map<String, DynamicComponent> componentsByType = indexDynamicsByType(world);
    for (ComponentInfo ci : config.components()) {
      if (!componentsByType.containsKey(normalize(ci.id()))) {
        continue;
      }
      for (ButtonInfo button : ci.buttons()) {
        ids.add(buttonId(button));
      }
    }
    return ids;
  }

  void bind(InputRouter router, GameConfig config, World world) {
    if (config == null) {
      return;
    }
    Map<String, DynamicComponent> componentsByType = indexDynamicsByType(world);
    for (ComponentInfo ci : config.components()) {
      DynamicComponent target = componentsByType.get(normalize(ci.id()));
      if (target == null) {
        continue;
      }
      for (ButtonInfo button : ci.buttons()) {
        String id = buttonId(button);
        Vector force = new Vector(button.directionX(), button.directionY());
        String forceId = "button:" + id;
        System.out.println("[ButtonForce] BIND \"" + id + "\" -> component=\""
            + ci.id() + "\" force=(" + force.x() + ", " + force.y()
            + ") mass=" + target.getMass() + " sleeping=" + target.isSleeping());
        router.bind(id, () -> {
          System.out.println("[ButtonForce] PRESSED \"" + id + "\" -> addExternalForce(\""
              + forceId + "\", (" + force.x() + ", " + force.y()
              + ")) sleeping=" + target.isSleeping());
          target.addExternalForce(forceId, force);
          System.out.println("[ButtonForce]   after add: sleeping=" + target.isSleeping()
              + " externalTotal=(" + target.getExternalForceTotal().x()
              + ", " + target.getExternalForceTotal().y() + ")");
        });
        router.bindRelease(id, () -> {
          System.out.println("[ButtonForce] RELEASED \"" + id + "\"");
          target.removeExternalForce(forceId);
        });
      }
    }
  }

  private Map<String, DynamicComponent> indexDynamicsByType(World world) {
    Map<String, DynamicComponent> index = new LinkedHashMap<>();
    for (GameObject obj : world.getObjects()) {
      if (obj instanceof DynamicComponent dc && obj instanceof TypedComponent typed) {
        index.putIfAbsent(normalize(typed.getComponentType()), dc);
      }
    }
    return index;
  }

  private String buttonId(ButtonInfo button) {
    return button.name().trim().toLowerCase(Locale.ROOT);
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
