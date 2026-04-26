package oogasalad.data.api;

import java.util.List;

/**
 * Movable part the player can place in the vehicle builder.
 *
 * @param id        unique key within its game catalog
 * @param spriteUrl path to the component's sprite asset
 * @param mass      physics mass (≥ 0)
 * @param buttons   on-screen controls this component contributes; empty = not player-controllable
 */
public record ComponentInfo(String id, String spriteUrl, double mass, List<ButtonInfo> buttons) {

  public ComponentInfo {
    buttons = List.copyOf(buttons);
  }
}
