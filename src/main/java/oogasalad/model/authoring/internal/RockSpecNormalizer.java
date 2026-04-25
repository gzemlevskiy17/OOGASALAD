package oogasalad.model.authoring.internal;

import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec.RockSpec;

/**
 * Normalizes generated rock entries.
 *
 * @author Emilio Garcia
 */
final class RockSpecNormalizer {

  private final NormalizationSupport support;

  /**
   * Creates a rock normalizer using the shared generation properties.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  RockSpecNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns normalized rock entries.
   *
   * @param rocks requested rock entries
   * @return normalized rock entries
   * @author Emilio Garcia
   */
  List<RockSpec> normalize(List<RockSpec> rocks) {
    return support.safeList(rocks).stream()
        .filter(Objects::nonNull)
        .map(this::normalizeRock)
        .toList();
  }

  private RockSpec normalizeRock(RockSpec rock) {
    double width = support.clampPositive(rock.width(), support.d("defaultRockWidth"));
    double height = support.clampPositive(rock.height(), support.d("defaultRockHeight"));
    double x = support.defaultIfNull(rock.x(), 0.0);
    double y = support.defaultIfNull(rock.y(), support.d("defaultRockY"));
    double friction = support.clamp(
        support.defaultIfNull(rock.friction(), support.d("defaultRockFriction")),
        support.d("minRockFriction"),
        support.d("maxRockFriction"));
    double restitution = support.clamp(
        support.defaultIfNull(rock.restitution(), support.d("defaultRockRestitution")),
        support.d("minRockRestitution"),
        support.d("maxRockRestitution"));
    double damage = support.clamp(
        support.defaultIfNull(rock.damage(), support.d("defaultRockDamage")),
        support.d("minRockDamage"),
        support.d("maxRockDamage"));
    return new RockSpec(x, y, width, height, friction, restitution, damage);
  }
}
