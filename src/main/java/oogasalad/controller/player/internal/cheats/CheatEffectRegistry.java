package oogasalad.controller.player.internal.cheats;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import oogasalad.controller.core.api.Outcome;
import oogasalad.data.api.CheatEffectInfo;
import oogasalad.data.api.CollisionAction;
import oogasalad.data.api.StaticComponentInfo;
import oogasalad.model.engine.ConvexPolygon;
import oogasalad.model.engine.api.CollisionEffect;
import oogasalad.model.engine.api.Damageable;
import oogasalad.model.engine.internal.components.GameComponent;
import oogasalad.model.engine.internal.components.PickupComponent;
import oogasalad.model.engine.internal.effects.HPChangerEffect;
import oogasalad.model.engine.internal.effects.ScoreEffect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registry of primitive cheat effects that game authors can compose in game data.
 *
 * @author Emilio Garcia
 */
public class CheatEffectRegistry {

  private static final Logger LOG = LogManager.getLogger(CheatEffectRegistry.class);
  private static final double DEFAULT_POWERUP_SIZE = 60.0;

  private final CheatTargetResolver targetResolver;
  private final Map<String, CheatEffectFactory> factories = new LinkedHashMap<>();
  private final Map<String, BiFunction<CollisionAction, CheatRuntimeContext, CollisionEffect>>
      collisionEffects = new LinkedHashMap<>();

  public CheatEffectRegistry(CheatTargetResolver targetResolver) {
    this.targetResolver = targetResolver;
    registerCollisionEffects();
  }

  /**
   * Creates the default primitive registry used by gameplay.
   */
  public static CheatEffectRegistry defaults() {
    CheatEffectRegistry registry = new CheatEffectRegistry(CheatTargetResolver.defaults());
    return registry
        .register("repair", registry::repairEffect)
        .register("damage", registry::damageEffect)
        .register("teleport", registry::teleportEffect)
        .register("finishRun", registry::finishRunEffect)
        .register("loseRun", registry::loseRunEffect)
        .register("setGravity", registry::setGravityEffect)
        .register("spawnPowerup", registry::spawnPowerupEffect);
  }

  /**
   * Adds or replaces a primitive cheat effect factory.
   */
  public CheatEffectRegistry register(String type, CheatEffectFactory factory) {
    factories.put(normalize(type), factory);
    LOG.debug("Registered cheat effect primitive '{}'", type);
    return this;
  }

  /**
   * Converts configured effect data into executable runtime behavior.
   */
  public CheatEffect create(CheatEffectInfo info) {
    String effectType = normalize(info.type());
    CheatEffectFactory factory = factories.get(effectType);
    if (factory == null) {
      throw new CheatConfigurationException(
          "No cheat effect primitive registered for: " + info.type());
    }
    return factory.create(info, targetResolver);
  }

  private CheatEffect repairEffect(CheatEffectInfo info, CheatTargetResolver resolver) {
    double amount = requiredDouble(info, "amount");
    return context -> resolver.resolve(info.target(), context).stream()
        .filter(Damageable.class::isInstance)
        .map(Damageable.class::cast)
        .forEach(target -> target.repair(amount));
  }

  private CheatEffect damageEffect(CheatEffectInfo info, CheatTargetResolver resolver) {
    double amount = requiredDouble(info, "amount");
    return context -> resolver.resolve(info.target(), context).stream()
        .filter(Damageable.class::isInstance)
        .map(Damageable.class::cast)
        .forEach(target -> target.damage(amount));
  }

  private CheatEffect teleportEffect(CheatEffectInfo info, CheatTargetResolver resolver) {
    double x = requiredDouble(info, "x");
    double y = requiredDouble(info, "y");
    return context -> resolver.resolve(info.target(), context).stream()
        .filter(GameComponent.class::isInstance)
        .map(GameComponent.class::cast)
        .forEach(component -> moveComponent(component, x, y));
  }

  private CheatEffect finishRunEffect(CheatEffectInfo info, CheatTargetResolver resolver) {
    return context -> context.terminalOutcome().accept(Outcome.GAME_WON);
  }

  private CheatEffect loseRunEffect(CheatEffectInfo info, CheatTargetResolver resolver) {
    return context -> context.terminalOutcome().accept(Outcome.GAME_LOST);
  }

  private CheatEffect setGravityEffect(CheatEffectInfo info, CheatTargetResolver resolver) {
    double gravity = requiredDouble(info, "value");
    return context -> context.gravitySetter().accept(gravity);
  }

  private CheatEffect spawnPowerupEffect(CheatEffectInfo info, CheatTargetResolver resolver) {
    String componentId = requiredParameter(info, "componentId");
    double x = requiredDouble(info, "x");
    double y = requiredDouble(info, "y");
    return context -> {
      StaticComponentInfo componentInfo = lookupStaticComponent(context, componentId);
      PickupComponent pickup = buildPickup(componentInfo, x, y, context);
      pickup.setOnDestroyHook(() -> context.world().destroyObject(pickup));
      context.world().addObject(pickup);
      LOG.info("Spawned cheat powerup '{}' at ({}, {})", componentInfo.id(), x, y);
    };
  }

  private void registerCollisionEffects() {
    collisionEffects.put("heal",
        (action, context) -> new HPChangerEffect(action.amount()));
    collisionEffects.put("damage",
        (action, context) -> new HPChangerEffect(-action.amount()));
    collisionEffects.put("score",
        (action, context) -> new ScoreEffect(context.scoreService(), (int) action.amount()));
  }

  private CollisionEffect collisionEffectFor(StaticComponentInfo info,
      CheatRuntimeContext context) {
    CollisionAction action = info.onCollisionAction();
    if (action == null) {
      return target -> {
      };
    }
    BiFunction<CollisionAction, CheatRuntimeContext, CollisionEffect> factory =
        collisionEffects.get(normalize(action.type()));
    if (factory == null) {
      throw new CheatConfigurationException(
          "Unsupported spawnPowerup collision action: " + action.type());
    }
    return factory.apply(action, context);
  }

  private PickupComponent buildPickup(StaticComponentInfo componentInfo, double x, double y,
      CheatRuntimeContext context) {
    ConvexPolygon polygon = ConvexPolygon.axisAlignedBox(
        x - DEFAULT_POWERUP_SIZE / 2.0,
        y - DEFAULT_POWERUP_SIZE / 2.0,
        DEFAULT_POWERUP_SIZE,
        DEFAULT_POWERUP_SIZE);
    return new PickupComponent(x, y, 0, polygon, "", componentInfo.id(),
        collisionEffectFor(componentInfo, context));
  }

  private StaticComponentInfo lookupStaticComponent(CheatRuntimeContext context,
      String componentId) {
    return context.gameConfig().staticComponents().stream()
        .filter(info -> info.id().equals(componentId) || info.id().equalsIgnoreCase(componentId))
        .findFirst()
        .orElseThrow(() -> new CheatConfigurationException(
            "No static component registered for spawnPowerup: " + componentId));
  }

  private void moveComponent(GameComponent component, double x, double y) {
    component.setPosition(x, y, component.rotation());
    component.updatePolygon();
  }

  private double requiredDouble(CheatEffectInfo info, String parameterName) {
    String rawValue = requiredParameter(info, parameterName);
    try {
      return Double.parseDouble(rawValue);
    } catch (NumberFormatException e) {
      throw new CheatConfigurationException(
          "Cheat effect '" + info.type() + "' parameter '" + parameterName
              + "' must be numeric: " + rawValue, e);
    }
  }

  private String requiredParameter(CheatEffectInfo info, String parameterName) {
    return Optional.ofNullable(info.parameters().get(parameterName))
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new CheatConfigurationException(
            "Cheat effect '" + info.type() + "' missing parameter: " + parameterName));
  }

  private String normalize(String value) {
    return Optional.ofNullable(value)
        .orElse("")
        .trim()
        .toLowerCase(Locale.ROOT);
  }
}
