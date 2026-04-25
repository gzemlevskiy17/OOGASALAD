package oogasalad.model.authoring.internal;

import oogasalad.model.authoring.api.GeneratedLevelSpec.FinishLineSpec;
import oogasalad.model.authoring.api.GeneratedLevelSpec.SpawnSpec;

/**
 * Normalized top-level generated-level metadata.
 *
 * @param style normalized style token
 * @param levelName normalized level name
 * @param gravity normalized gravity
 * @param backgroundImage normalized background image path
 * @param worldWidth normalized world width
 * @param spawn normalized spawn placement
 * @param finishLine normalized finish-line placement
 * @author Emilio Garcia
 */
record NormalizedLevelMetadata(
    String style,
    String levelName,
    double gravity,
    String backgroundImage,
    double worldWidth,
    SpawnSpec spawn,
    FinishLineSpec finishLine) {
}
