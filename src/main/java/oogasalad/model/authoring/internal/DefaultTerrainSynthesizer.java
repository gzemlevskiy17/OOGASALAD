package oogasalad.model.authoring.internal;

import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainBlockSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Synthesizes fallback terrain when generated specs do not contain any usable terrain.
 *
 * @author Emilio Garcia
 */
final class DefaultTerrainSynthesizer {

  private static final Logger LOG = LogManager.getLogger(DefaultTerrainSynthesizer.class);

  private final NormalizationSupport support;

  /**
   * Creates a default-terrain synthesizer using the shared generation properties.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  DefaultTerrainSynthesizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns a single fallback terrain block spanning the generated world.
   *
   * @param worldWidth normalized world width
   * @return synthesized fallback terrain block
   * @author Emilio Garcia
   */
  TerrainBlockSpec synthesize(double worldWidth) {
    double width = Math.max(support.d("minimumTerrainWidth"),
        Math.min(worldWidth, support.d("defaultTerrainBlockWidth")));
    LOG.info(support.s("defaultTerrainSynthesizedLogMessage"));
    return new TerrainBlockSpec(
        0.0,
        support.d("defaultTerrainBlockY"),
        width,
        support.d("defaultTerrainBlockHeight"),
        support.s("defaultTerrainTexture"),
        support.b("defaultTerrainTileTexture"),
        support.d("defaultTerrainFriction"),
        support.d("defaultTerrainRestitution"));
  }
}
