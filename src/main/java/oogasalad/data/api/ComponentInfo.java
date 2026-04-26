package oogasalad.data.api;

import java.util.List;

/**
 * Catalog entry for a movable vehicle component. Buttons may be empty, in which case the component
 * is not player-controllable (it just moves passively as part of the vehicle).
 *
 * @param id        catalog id referenced from level files
 * @param spriteUrl path to the component's sprite
 * @param mass      physics mass
 * @param buttons   on-screen buttons this component contributes; empty for non-controllable parts
 * @author Paulina Vvedenskaya
 */
public record ComponentInfo(
    String id,
    String spriteUrl,
    double mass,
    List<Button> buttons
) {

}
