package oogasalad.model.authoring.internal;

import java.util.Arrays;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.FinishLineSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PickupSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.PointSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.RockSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.SpawnSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainBlockSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.TerrainSplineSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.VehiclePaletteSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpecValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Validates AI-generated level specifications before normalization and conversion into runtime
 * level JSON.
 *
 * <p>The validator accepts partially specified drafts, but rejects structurally unsafe values such
 * as unsupported pickup types, negative dimensions, or out-of-range physics parameters.
 *
 * @author Emilio Garcia
 */
public class GeneratedLevelSpecValidator {

  private static final Logger LOG = LogManager.getLogger(GeneratedLevelSpecValidator.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecValidator");

  private final Set<String> allowedStyles;
  private final Set<String> allowedPickupTypes;

  /**
   * Creates a generated-level validator using resource-backed validation rules.
   *
   * @author Emilio Garcia
   */
  public GeneratedLevelSpecValidator() {
    this.allowedStyles = csvSet("allowedStyles");
    this.allowedPickupTypes = csvSet("allowedPickupTypes");
  }

  /**
   * Validates the provided generated-level spec and throws on the first unsafe value.
   *
   * @param spec generated level specification to validate
   * @author Emilio Garcia
   */
  public void validate(GeneratedLevelSpec spec) {
    if (spec == null) {
      fail("nullSpecError");
    }

    validateStyle(spec.style());
    validateGravity(spec.gravity());
    validateWorldWidth(spec.worldWidth());
    validateSpawn(spec.spawn());
    validateFinishLine(spec.finishLine());
    validateVehiclePalette(spec.vehiclePalette());
    validateTerrainBlocks(spec.terrainBlocks());
    validateTerrainSplines(spec.terrainSplines());
    validateRocks(spec.rocks());
    validatePickups(spec.pickups());
  }

  private void validateStyle(String style) {
    if (style == null || style.isBlank()) {
      return;
    }
    if (!allowedStyles.contains(style.trim().toLowerCase())) {
      fail("invalidStyleError", style);
    }
  }

  private void validateGravity(Double gravity) {
    if (gravity == null) {
      return;
    }
    validateRange("gravityOutOfRangeError", gravity, d("minGravity"), d("maxGravity"));
  }

  private void validateWorldWidth(Double worldWidth) {
    if (worldWidth == null) {
      return;
    }
    validateRange("worldWidthOutOfRangeError", worldWidth, d("minWorldWidth"), d("maxWorldWidth"));
  }

  private void validateSpawn(SpawnSpec spawn) {
    if (spawn == null) {
      return;
    }
    requirePresent("spawnXMissingError", spawn.x());
    requirePresent("spawnYMissingError", spawn.y());
  }

  private void validateFinishLine(FinishLineSpec finishLine) {
    if (finishLine == null) {
      return;
    }
    requirePresent("finishXMissingError", finishLine.x());
    requirePresent("finishYMissingError", finishLine.y());
    validatePositive("finishWidthInvalidError", finishLine.width());
    validatePositive("finishHeightInvalidError", finishLine.height());
  }

  private void validateVehiclePalette(VehiclePaletteSpec palette) {
    if (palette == null) {
      return;
    }
    Integer maxSelectable = palette.maxSelectable();
    if (maxSelectable != null) {
      validateRange("maxSelectableOutOfRangeError", maxSelectable.doubleValue(),
          d("minMaxSelectable"), d("maxMaxSelectable"));
    }
    for (String componentId : palette.allowedComponents()) {
      if (componentId == null || componentId.isBlank()) {
        fail("blankVehicleComponentIdError");
      }
    }
  }

  private void validateTerrainBlocks(java.util.List<TerrainBlockSpec> blocks) {
    for (int i = 0; i < blocks.size(); i++) {
      TerrainBlockSpec block = blocks.get(i);
      requirePresent("terrainBlockXMissingError", block.x(), i);
      requirePresent("terrainBlockYMissingError", block.y(), i);
      validatePositive("terrainBlockWidthInvalidError", block.width(), i);
      validatePositive("terrainBlockHeightInvalidError", block.height(), i);
      validateOptionalRange("terrainFrictionOutOfRangeError", block.friction(),
          d("minTerrainFriction"), d("maxTerrainFriction"), i);
      validateOptionalRange("terrainRestitutionOutOfRangeError", block.restitution(),
          d("minTerrainRestitution"), d("maxTerrainRestitution"), i);
    }
  }

  private void validateTerrainSplines(java.util.List<TerrainSplineSpec> splines) {
    for (int i = 0; i < splines.size(); i++) {
      TerrainSplineSpec spline = splines.get(i);
      if (spline.knots().size() < n("minKnotsPerSpline")) {
        fail("splineKnotsTooShortError", i);
      }
      for (int j = 0; j < spline.knots().size(); j++) {
        validatePoint(spline.knots().get(j), "splineKnotMissingCoordinateError", i, j);
      }
      if (!spline.controls().isEmpty() && spline.controls().size() != spline.knots().size() - 1) {
        fail("splineControlsSizeMismatchError", i);
      }
      for (int j = 0; j < spline.controls().size(); j++) {
        validatePoint(spline.controls().get(j), "splineControlMissingCoordinateError", i, j);
      }
      requirePresent("splineBaselineMissingError", spline.baselineY(), i);
      if (spline.baselineY() != null) {
        double highestKnotY = spline.knots().stream()
            .map(PointSpec::y)
            .filter(java.util.Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(Double.NEGATIVE_INFINITY);
        if (spline.baselineY() <= highestKnotY) {
          fail("splineBaselineInvalidError", i);
        }
      }
      validateOptionalRange("splineResolutionOutOfRangeError",
          spline.resolution() == null ? null : spline.resolution().doubleValue(),
          d("minSplineResolution"), d("maxSplineResolution"), i);
      validateOptionalRange("terrainFrictionOutOfRangeError", spline.friction(),
          d("minTerrainFriction"), d("maxTerrainFriction"), i);
    }
  }

  private void validateRocks(java.util.List<RockSpec> rocks) {
    for (int i = 0; i < rocks.size(); i++) {
      RockSpec rock = rocks.get(i);
      requirePresent("rockXMissingError", rock.x(), i);
      requirePresent("rockYMissingError", rock.y(), i);
      validatePositive("rockWidthInvalidError", rock.width(), i);
      validatePositive("rockHeightInvalidError", rock.height(), i);
      validateOptionalRange("rockFrictionOutOfRangeError", rock.friction(),
          d("minRockFriction"), d("maxRockFriction"), i);
      validateOptionalRange("rockRestitutionOutOfRangeError", rock.restitution(),
          d("minRockRestitution"), d("maxRockRestitution"), i);
      validateOptionalRange("rockDamageOutOfRangeError", rock.damage(),
          d("minRockDamage"), d("maxRockDamage"), i);
    }
  }

  private void validatePickups(java.util.List<PickupSpec> pickups) {
    for (int i = 0; i < pickups.size(); i++) {
      PickupSpec pickup = pickups.get(i);
      if (pickup.type() == null || pickup.type().isBlank()) {
        fail("pickupTypeMissingError", i);
      }
      String normalizedType = pickup.type().trim().toLowerCase();
      if (!allowedPickupTypes.contains(normalizedType)) {
        fail("pickupTypeInvalidError", pickup.type(), i);
      }
      requirePresent("pickupXMissingError", pickup.x(), i);
      requirePresent("pickupYMissingError", pickup.y(), i);
      validatePositive("pickupWidthInvalidError", pickup.width(), i);
      validatePositive("pickupHeightInvalidError", pickup.height(), i);
      validateOptionalRange("pickupAmountOutOfRangeError", pickup.amount(),
          d("minPickupAmount"), d("maxPickupAmount"), i);
    }
  }

  private void validatePoint(PointSpec point, String messageKey, int splineIndex, int pointIndex) {
    if (point == null || point.x() == null || point.y() == null) {
      fail(messageKey, splineIndex, pointIndex);
    }
  }

  private void requirePresent(String key, Object value, Object... args) {
    if (value == null) {
      fail(key, args);
    }
  }

  private void validatePositive(String key, Double value, Object... args) {
    if (value == null || value <= 0) {
      fail(key, args);
    }
  }

  private void validateOptionalRange(String key, Double value, double min, double max,
      Object... args) {
    if (value == null) {
      return;
    }
    validateRange(key, value, min, max, args);
  }

  private void validateRange(String key, double value, double min, double max, Object... args) {
    if (value < min || value > max) {
      Object[] expandedArgs = Arrays.copyOf(args, args.length + 2);
      expandedArgs[args.length] = min;
      expandedArgs[args.length + 1] = max;
      fail(key, expandedArgs);
    }
  }

  private void fail(String key, Object... args) {
    String message = String.format(PROPS.getString(key), args);
    LOG.warn(message);
    throw new GeneratedLevelSpecValidationException(message);
  }

  private Set<String> csvSet(String key) {
    return Arrays.stream(PROPS.getString(key).split(","))
        .map(String::trim)
        .filter(token -> !token.isEmpty())
        .map(String::toLowerCase)
        .collect(Collectors.toUnmodifiableSet());
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }

  private int n(String key) {
    return Integer.parseInt(PROPS.getString(key));
  }
}
