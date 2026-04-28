package oogasalad.data.api;

import java.util.Map;

/**
 * Declarative primitive effect used by an author-defined cheat.
 *
 * <p>The {@code type} is resolved by the runtime cheat effect registry. Parameters remain generic
 * so new primitive effects can be added without changing this data contract. Initial primitive
 * examples include {@code repair}, {@code damage}, {@code teleport}, {@code finishRun},
 * {@code loseRun}, {@code addScore}, {@code destroy}, {@code launch}, and
 * {@code spawnPowerup}. A spawn-powerup effect can use parameters such as {@code componentId},
 * {@code x}, and {@code y}.
 *
 * @param type       primitive effect type interpreted by the runtime registry
 * @param target     target selector for effects that act on objects or the world
 * @param parameters string parameters consumed by the selected effect factory
 * @author Emilio Garcia
 */
public record CheatEffectInfo(
    String type,
    CheatTargetInfo target,
    Map<String, String> parameters) {

  public CheatEffectInfo {
    parameters = Map.copyOf(parameters);
  }
}
