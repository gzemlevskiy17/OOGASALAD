package oogasalad.model.authoring.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PointSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainSplineSpec;

/**
 * Normalizes generated spline-terrain entries.
 *
 * @author Emilio Garcia
 */
final class TerrainSplineSpecNormalizer {

  private final NormalizationSupport support;

  /**
   * Creates a spline normalizer using the shared generation properties.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  TerrainSplineSpecNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns normalized spline terrain entries.
   *
   * @param terrainSplines requested spline terrain entries
   * @return normalized spline terrain entries
   * @author Emilio Garcia
   */
  List<TerrainSplineSpec> normalize(List<TerrainSplineSpec> terrainSplines) {
    List<TerrainSplineSpec> normalized = new ArrayList<>();
    for (TerrainSplineSpec spline : support.safeList(terrainSplines)) {
      if (spline == null) {
        continue;
      }
      List<PointSpec> knots = spline.knots().stream()
          .filter(Objects::nonNull)
          .filter(point -> point.x() != null && point.y() != null)
          .map(point -> new PointSpec(point.x(), point.y()))
          .toList();
      if (knots.size() < support.n("minKnotsPerSpline")) {
        continue;
      }

      List<PointSpec> controls = normalizeControls(spline.controls(), knots);
      double highestKnotY = knots.stream().mapToDouble(PointSpec::y).max()
          .orElse(support.d("defaultSplineKnotY"));
      double baselineY = support.defaultIfNull(spline.baselineY(),
          highestKnotY + support.d("defaultSplineBaselinePadding"));
      if (baselineY <= highestKnotY) {
        baselineY = highestKnotY + support.d("defaultSplineBaselinePadding");
      }

      int resolution = (int) support.clamp(
          spline.resolution() == null ? support.n("defaultSplineResolution") : spline.resolution(),
          support.d("minSplineResolution"),
          support.d("maxSplineResolution"));
      String texture = support.normalizeText(spline.texture(), support.s("defaultSplineTexture"));
      boolean tileTexture = spline.tileTexture() == null
          ? support.b("defaultSplineTileTexture")
          : spline.tileTexture();
      double friction = support.clamp(
          support.defaultIfNull(spline.friction(), support.d("defaultSplineFriction")),
          support.d("minTerrainFriction"),
          support.d("maxTerrainFriction"));
      String color = support.normalizeText(spline.color(), support.s("defaultSplineColor"));

      normalized.add(new TerrainSplineSpec(knots, controls, baselineY, resolution, texture,
          tileTexture, friction, color));
    }
    return normalized;
  }

  private List<PointSpec> normalizeControls(List<PointSpec> controls, List<PointSpec> knots) {
    List<PointSpec> validControls = support.safeList(controls).stream()
        .filter(Objects::nonNull)
        .filter(point -> point.x() != null && point.y() != null)
        .map(point -> new PointSpec(point.x(), point.y()))
        .toList();
    if (validControls.size() == knots.size() - 1) {
      return validControls;
    }
    return midpointControls(knots);
  }

  private List<PointSpec> midpointControls(List<PointSpec> knots) {
    List<PointSpec> generated = new ArrayList<>();
    for (int i = 0; i < knots.size() - 1; i++) {
      PointSpec start = knots.get(i);
      PointSpec end = knots.get(i + 1);
      generated.add(new PointSpec((start.x() + end.x()) / 2.0, (start.y() + end.y()) / 2.0));
    }
    return generated;
  }
}
