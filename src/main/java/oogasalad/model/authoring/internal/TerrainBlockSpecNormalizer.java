package oogasalad.model.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainBlockSpec;

/**
 * Normalizes generated terrain block entries.
 *
 * @author Emilio Garcia
 */
final class TerrainBlockSpecNormalizer {

  private final NormalizationSupport support;

  /**
   * Creates a terrain-block normalizer using the shared generation properties.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  TerrainBlockSpecNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns normalized terrain blocks constrained to the normalized world width.
   *
   * @param terrainBlocks requested terrain blocks
   * @param worldWidth normalized world width
   * @return normalized terrain blocks
   * @author Emilio Garcia
   */
  List<TerrainBlockSpec> normalize(List<TerrainBlockSpec> terrainBlocks, double worldWidth) {
    List<TerrainBlockSpec> normalized = new ArrayList<>();
    for (TerrainBlockSpec block : support.safeList(terrainBlocks)) {
      if (block == null) {
        continue;
      }
      double width = support.clampPositive(block.width(), support.d("defaultTerrainBlockWidth"));
      double height = support.clampPositive(block.height(), support.d("defaultTerrainBlockHeight"));
      double x = support.defaultIfNull(block.x(), 0.0);
      double y = support.defaultIfNull(block.y(), support.d("defaultTerrainBlockY"));
      String texture = support.normalizeText(block.texture(), support.s("defaultTerrainTexture"));
      boolean tileTexture = block.tileTexture() == null
          ? support.b("defaultTerrainTileTexture")
          : block.tileTexture();
      double friction = support.clamp(
          support.defaultIfNull(block.friction(), support.d("defaultTerrainFriction")),
          support.d("minTerrainFriction"),
          support.d("maxTerrainFriction"));
      double restitution = support.clamp(
          support.defaultIfNull(block.restitution(), support.d("defaultTerrainRestitution")),
          support.d("minTerrainRestitution"),
          support.d("maxTerrainRestitution"));
      normalized.add(new TerrainBlockSpec(x, y, width, height, texture, tileTexture, friction,
          restitution));
    }
    return normalized.stream()
        .map(block -> trimToWorldWidth(block, worldWidth))
        .filter(Objects::nonNull)
        .toList();
  }

  private TerrainBlockSpec trimToWorldWidth(TerrainBlockSpec block, double worldWidth) {
    if (block.x() >= worldWidth) {
      return null;
    }
    double adjustedWidth = Math.min(block.width(), Math.max(support.d("minimumTerrainWidth"),
        worldWidth - block.x()));
    return new TerrainBlockSpec(block.x(), block.y(), adjustedWidth, block.height(),
        block.texture(), block.tileTexture(), block.friction(), block.restitution());
  }
}
