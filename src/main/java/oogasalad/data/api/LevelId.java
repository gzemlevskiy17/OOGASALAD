package oogasalad.data.api;

/**
 * Identifies one level by the (world, level) pair used in the UI and the level JSON tree.
 *
 * <p>For example, {@code new LevelId("forest", "first")} refers to the level whose JSON lives
 * at {@code data/levels/forest/first.json} and whose buttons on the entry screen are labeled
 * with the "forest" world header and the "1" tile.
 *
 * <p>Plain value type. Two LevelIds with the same keys are equal (records give this for free),
 * which makes it safe as a map key or set member.
 */
public record LevelId(String worldKey, String levelKey) {
}
