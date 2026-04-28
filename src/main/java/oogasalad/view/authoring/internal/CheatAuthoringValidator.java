package oogasalad.view.authoring.internal;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.input.KeyCode;
import oogasalad.controller.player.internal.cheats.CheatEffectRegistry;
import oogasalad.controller.player.internal.cheats.CheatTargetResolver;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CheatInfo;
import oogasalad.data.api.CheatTargetInfo;
import oogasalad.data.api.StaticComponentInfo;

/**
 * Validates author-entered cheat data against the runtime cheat registries and current catalog.
 *
 * @author Emilio Garcia
 */
public class CheatAuthoringValidator {

  private static final String TARGET_WORLD = "world";
  private static final Set<String> SUPPORTED_EFFECT_TYPES =
      normalizedSet(CheatEffectRegistry.defaults().registeredTypes());
  private static final Set<String> SUPPORTED_TARGET_KINDS =
      normalizedSet(CheatTargetResolver.defaults().registeredKinds());

  public void validateCheats(List<CheatInfo> cheats, List<StaticComponentInfo> staticComponents) {
    cheats.forEach(cheat -> validateCheat(cheat, staticComponents));
  }

  public void validateCheat(CheatInfo cheat, List<StaticComponentInfo> staticComponents) {
    if (cheat.trigger() == null || cheat.trigger().keys().isEmpty()) {
      fail("Cheat '" + cheat.id() + "' must include at least one trigger key.");
    }
    cheat.trigger().keys().forEach(this::validateKey);
    if (cheat.effects().isEmpty()) {
      fail("Cheat '" + cheat.id() + "' must include at least one effect.");
    }
    cheat.effects().forEach(effect -> validateEffect(effect, staticComponents));
  }

  public void validateEffect(CheatEffectInfo effect, List<StaticComponentInfo> staticComponents) {
    String effectType = normalize(effect.type());
    if (!SUPPORTED_EFFECT_TYPES.contains(effectType)) {
      fail("Unsupported cheat effect type: " + effect.type());
    }
    String targetKind = normalize(effect.target() == null ? TARGET_WORLD : effect.target().kind());
    if (!SUPPORTED_TARGET_KINDS.contains(targetKind)) {
      fail("Unsupported cheat target kind: " + (effect.target() == null ? TARGET_WORLD
          : effect.target().kind()));
    }
    switch (effectType) {
      case "repair", "damage" -> requireNumber(effect, "amount");
      case "teleport" -> requireNumbers(effect, "x", "y");
      case "addscore" -> requireInteger(effect, "amount");
      case "destroy" -> {
      }
      case "launch" -> requireNumbers(effect, "xVelocity", "yVelocity");
      case "spawnpowerup" -> validateSpawnPowerup(effect, staticComponents);
      case "finishrun", "loserun" -> {
      }
      default -> fail("No authoring validation rule for cheat effect type: " + effect.type());
    }
  }

  private void validateKey(String rawKey) {
    String normalizedKey = normalizeKey(rawKey);
    if (normalizedKey.isBlank()) {
      fail("Cheat key names cannot be blank.");
    }
    try {
      KeyCode.valueOf(normalizedKey);
    } catch (IllegalArgumentException e) {
      fail("Invalid JavaFX cheat key: " + rawKey);
    }
  }

  private void validateSpawnPowerup(CheatEffectInfo effect,
      List<StaticComponentInfo> staticComponents) {
    String componentId = requireText(effect, "componentId");
    if (staticComponents.stream().noneMatch(component -> idsMatch(component.id(), componentId))) {
      fail("spawnPowerup componentId is not a static component id: " + componentId);
    }
    boolean hasX = hasParameter(effect, "x");
    boolean hasY = hasParameter(effect, "y");
    if (hasX != hasY) {
      fail("spawnPowerup must define both x and y or neither.");
    }
    if (hasX) {
      requireNumbers(effect, "x", "y");
      return;
    }
    CheatTargetInfo target = effect.target() == null ? new CheatTargetInfo(TARGET_WORLD, "") : effect.target();
    if (TARGET_WORLD.equals(normalize(target.kind()))) {
      fail("spawnPowerup without x/y needs an object target such as mainCharacter.");
    }
  }

  private void requireNumber(CheatEffectInfo effect, String parameterName) {
    String rawValue = requireText(effect, parameterName);
    try {
      Double.parseDouble(rawValue);
    } catch (NumberFormatException e) {
      fail("Cheat effect '" + effect.type() + "' parameter '" + parameterName
          + "' must be numeric: " + rawValue);
    }
  }

  private void requireNumbers(CheatEffectInfo effect, String... parameterNames) {
    for (String parameterName : parameterNames) {
      requireNumber(effect, parameterName);
    }
  }

  private void requireInteger(CheatEffectInfo effect, String parameterName) {
    String rawValue = requireText(effect, parameterName);
    try {
      double value = Double.parseDouble(rawValue);
      if (value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
        throw new NumberFormatException(rawValue);
      }
    } catch (NumberFormatException e) {
      fail("Cheat effect '" + effect.type() + "' parameter '" + parameterName
          + "' must be an integer: " + rawValue);
    }
  }

  private String requireText(CheatEffectInfo effect, String parameterName) {
    String rawValue = effect.parameters().get(parameterName);
    if (blank(rawValue)) {
      fail("Cheat effect '" + effect.type() + "' missing parameter: " + parameterName);
    }
    return rawValue.strip();
  }

  private boolean hasParameter(CheatEffectInfo effect, String parameterName) {
    return !blank(effect.parameters().get(parameterName));
  }

  private boolean idsMatch(String first, String second) {
    return normalize(first).equals(normalize(second));
  }

  private String normalizeKey(String rawKey) {
    return rawKey == null ? "" : rawKey.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
  }

  private static Set<String> normalizedSet(List<String> values) {
    return values.stream()
        .map(CheatAuthoringValidator::normalize)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private void fail(String message) {
    throw new CheatAuthoringException(message);
  }
}
