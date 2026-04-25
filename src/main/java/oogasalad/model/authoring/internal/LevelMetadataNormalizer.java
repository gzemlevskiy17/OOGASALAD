package oogasalad.model.authoring.internal;

import java.util.ResourceBundle;
import java.util.Set;
import oogasalad.model.authoring.api.GeneratedLevelSpec.FinishLineSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.SpawnSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Normalizes top-level generated-level metadata such as style, gravity, spawn, and finish line.
 *
 * @author Emilio Garcia
 */
final class LevelMetadataNormalizer {

  private static final Logger LOG = LogManager.getLogger(LevelMetadataNormalizer.class);

  private final NormalizationSupport support;
  private final Set<String> allowedStyles;

  /**
   * Creates a metadata normalizer using the shared generation properties.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  LevelMetadataNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
    this.allowedStyles = support.csvSet("allowedStyles");
  }

  /**
   * Returns normalized top-level metadata.
   *
   * @param style requested style
   * @param levelName requested level name
   * @param gravity requested gravity
   * @param backgroundImage requested background image path
   * @param worldWidth requested world width
   * @param spawn requested spawn placement
   * @param finishLine requested finish-line placement
   * @return normalized metadata bundle
   * @author Emilio Garcia
   */
  NormalizedLevelMetadata normalize(String style, String levelName, Double gravity,
      String backgroundImage, Double worldWidth, SpawnSpec spawn, FinishLineSpec finishLine) {
    double normalizedWorldWidth = support.clamp(
        support.defaultIfNull(worldWidth, support.d("defaultWorldWidth")),
        support.d("minWorldWidth"),
        support.d("maxWorldWidth"));
    return new NormalizedLevelMetadata(
        normalizeStyle(style),
        support.normalizeText(levelName, support.s("defaultLevelName")),
        support.clamp(support.defaultIfNull(gravity, support.d("defaultGravity")),
            support.d("minGravity"), support.d("maxGravity")),
        support.normalizeText(backgroundImage, support.s("defaultBackgroundImage")),
        normalizedWorldWidth,
        normalizeSpawn(spawn),
        normalizeFinishLine(finishLine, normalizedWorldWidth));
  }

  private String normalizeStyle(String style) {
    String normalizedStyle = support.normalizeText(style, support.s("defaultStyle")).toLowerCase();
    if (!allowedStyles.contains(normalizedStyle)) {
      LOG.warn(support.s("styleFallbackLogMessage"), style, support.s("defaultStyle"));
      return support.s("defaultStyle");
    }
    return normalizedStyle;
  }

  private SpawnSpec normalizeSpawn(SpawnSpec spawn) {
    double x = spawn == null
        ? support.d("defaultSpawnX")
        : support.defaultIfNull(spawn.x(), support.d("defaultSpawnX"));
    double y = spawn == null
        ? support.d("defaultSpawnY")
        : support.defaultIfNull(spawn.y(), support.d("defaultSpawnY"));
    return new SpawnSpec(x, y);
  }

  private FinishLineSpec normalizeFinishLine(FinishLineSpec finishLine, double worldWidth) {
    double defaultX = worldWidth - support.d("defaultFinishLineRightMargin");
    double x = finishLine == null ? defaultX : support.defaultIfNull(finishLine.x(), defaultX);
    double y = finishLine == null
        ? support.d("defaultFinishLineY")
        : support.defaultIfNull(finishLine.y(), support.d("defaultFinishLineY"));
    double width = support.clampPositive(
        finishLine == null ? support.d("defaultFinishLineWidth")
            : support.defaultIfNull(finishLine.width(), support.d("defaultFinishLineWidth")),
        support.d("defaultFinishLineWidth"));
    double height = support.clampPositive(
        finishLine == null ? support.d("defaultFinishLineHeight")
            : support.defaultIfNull(finishLine.height(), support.d("defaultFinishLineHeight")),
        support.d("defaultFinishLineHeight"));
    return new FinishLineSpec(x, y, width, height);
  }
}
