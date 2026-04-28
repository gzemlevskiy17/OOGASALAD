package oogasalad.controller.player.internal.cheats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.api.DefaultTag;
import oogasalad.model.engine.api.GameObject;
import oogasalad.model.engine.api.TypedComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registry of target-selector primitives supported by author-defined cheats.
 *
 * @author Emilio Garcia
 */
public class CheatTargetResolver {

  private static final Logger LOG = LogManager.getLogger(CheatTargetResolver.class);
  private static final CheatTargetInfo WORLD_TARGET = new CheatTargetInfo("world", "");

  private final Map<String, CheatTargetSelector> selectors = new LinkedHashMap<>();
  private final Map<String, String> registeredKindNames = new LinkedHashMap<>();

  /**
   * Creates the default selector registry for gameplay cheats.
   */
  public static CheatTargetResolver defaults() {
    return new CheatTargetResolver()
        .register("world", (target, context) -> List.of())
        .register("tag", CheatTargetResolver::tagTargets)
        .register("vehicle", CheatTargetResolver::vehicleTargets)
        .register("mainCharacter", CheatTargetResolver::mainCharacterTargets)
        .register("allDamageable", CheatTargetResolver::damageableTargets)
        .register("componentType", CheatTargetResolver::componentTypeTargets);
  }

  /**
   * Adds or replaces a target selector.
   */
  public CheatTargetResolver register(String kind, CheatTargetSelector selector) {
    String selectorKind = normalize(kind);
    selectors.put(selectorKind, selector);
    registeredKindNames.put(selectorKind, kind);
    LOG.debug("Registered cheat target selector '{}'", kind);
    return this;
  }

  /**
   * Returns the registered target selector kind names in registration order.
   */
  public List<String> registeredKinds() {
    return List.copyOf(registeredKindNames.values());
  }

  /**
   * Resolves configured target data using the registered selector primitive.
   */
  public List<GameObject> resolve(CheatTargetInfo target, CheatRuntimeContext context) {
    CheatTargetInfo effectiveTarget = Optional.ofNullable(target).orElse(WORLD_TARGET);
    String selectorKey = normalize(effectiveTarget.kind());
    CheatTargetSelector selector = selectors.get(selectorKey);
    if (selector == null) {
      throw new CheatConfigurationException(
          "No cheat target selector registered for: " + effectiveTarget.kind());
    }
    return selector.resolve(effectiveTarget, context);
  }

  private static List<GameObject> tagTargets(CheatTargetInfo target, CheatRuntimeContext context) {
    return context.world().getObjectsByTag(target.id());
  }

  private static List<GameObject> vehicleTargets(CheatTargetInfo target,
      CheatRuntimeContext context) {
    return context.world().getObjectsByTag(DefaultTag.VEHICLE.value());
  }

  private static List<GameObject> mainCharacterTargets(CheatTargetInfo target,
      CheatRuntimeContext context) {
    String selectorId = selectorValue(target, context.mainCharacterId());
    List<GameObject> taggedTargets = context.world().getObjectsByTag(selectorId);
    if (!taggedTargets.isEmpty()) {
      return taggedTargets;
    }
    List<GameObject> typedTargets = typedTargets(context, selectorId);
    if (!typedTargets.isEmpty()) {
      return typedTargets;
    }
    return context.world().getObjectsByTag(DefaultTag.PIGS.value());
  }

  private static List<GameObject> damageableTargets(CheatTargetInfo target,
      CheatRuntimeContext context) {
    return context.world().getObjects().stream()
        .filter(Damageable.class::isInstance)
        .toList();
  }

  private static List<GameObject> componentTypeTargets(CheatTargetInfo target,
      CheatRuntimeContext context) {
    return typedTargets(context, target.id());
  }

  private static List<GameObject> typedTargets(CheatRuntimeContext context, String componentType) {
    return context.world().getObjects().stream()
        .filter(TypedComponent.class::isInstance)
        .filter(object -> ((TypedComponent) object).getComponentType().equals(componentType))
        .toList();
  }

  private static String selectorValue(CheatTargetInfo target, String fallback) {
    return Optional.ofNullable(target.id())
        .filter(value -> !value.isBlank())
        .orElse(fallback);
  }

  private static String normalize(String value) {
    return Optional.ofNullable(value)
        .orElse("")
        .trim()
        .toLowerCase(Locale.ROOT);
  }
}
