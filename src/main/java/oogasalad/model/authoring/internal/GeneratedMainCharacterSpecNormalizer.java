package oogasalad.model.authoring.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpec;
import oogasalad.model.authoring.api.GoodPiggiesCatalog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Normalizes the schema {@code mainCharacter} section for AI-generated Good Piggies levels.
 *
 * @author Emilio Garcia
 */
final class GeneratedMainCharacterSpecNormalizer {

  private static final Logger LOG = LogManager.getLogger(GeneratedMainCharacterSpecNormalizer.class);

  private final NormalizationSupport support;

  /**
   * Creates a main-character normalizer backed by the given resource bundle.
   *
   * @param props normalization properties bundle
   * @author Emilio Garcia
   */
  GeneratedMainCharacterSpecNormalizer(ResourceBundle props) {
    this.support = new NormalizationSupport(props);
  }

  /**
   * Returns a normalized schema {@code mainCharacter} section.
   *
   * @param mainCharacter generated main-character section
   * @param catalog fixed Good Piggies catalog
   * @return normalized main-character section
   * @author Emilio Garcia
   */
  GeneratedLevelSpec.MainCharacterSpec normalize(
      GeneratedLevelSpec.MainCharacterSpec mainCharacter, GoodPiggiesCatalog catalog) {
    GeneratedLevelSpec.MainCharacterSpec safeMainCharacter = mainCharacter == null
        ? new GeneratedLevelSpec.MainCharacterSpec(null, List.of(), null, null, null)
        : mainCharacter;
    return new GeneratedLevelSpec.MainCharacterSpec(
        support.clamp(support.defaultIfNull(safeMainCharacter.gridSize(),
                support.d("defaultGridSize")),
            support.d("minGridSize"),
            support.d("maxGridSize")),
        normalizeAllowedComponents(safeMainCharacter.elementsAllowed(), catalog),
        support.clampInt(safeMainCharacter.maxSelectable(), support.n("defaultMaxSelectable"),
            support.n("minMaxSelectable"), support.n("maxMaxSelectable")),
        support.clamp(support.defaultIfNull(safeMainCharacter.maxHP(),
                support.d("defaultMaxHp")),
            support.d("minMaxHp"),
            support.d("maxMaxHp")),
        normalizeCameraFollows());
  }

  private List<String> normalizeAllowedComponents(List<String> componentIds,
      GoodPiggiesCatalog catalog) {
    LinkedHashSet<String> normalized = new LinkedHashSet<>();
    for (String componentId : support.safeList(componentIds)) {
      String candidate = support.normalizeText(componentId, "");
      if (candidate.isBlank()) {
        continue;
      }
      if (!catalog.componentIds().contains(candidate)) {
        LOG.warn(support.s("invalidComponentDroppedLogMessage"), candidate);
        continue;
      }
      normalized.add(candidate);
    }
    return List.copyOf(normalized);
  }

  private String normalizeCameraFollows() {
    return support.s("defaultCameraFollows");
  }
}
