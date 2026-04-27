package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

/**
 * Tests resource-backed normalization helper behavior.
 *
 * @author Emilio Garcia
 */
class NormalizationSupportTest {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecNormalizer");

  @Test
  void NormalizeText_BlankValue_ReturnsFallbackText() {
    NormalizationSupport support = new NormalizationSupport(PROPS);

    assertEquals("fallback", support.normalizeText(" ", "fallback"));
  }

  @Test
  void CsvStrings_CommaSeparatedProperty_ReturnsTrimmedTokens() {
    NormalizationSupport support = new NormalizationSupport(PROPS);

    assertEquals(List.of("Coin", "Rock", "XPBoost", "Coin", "Bomb", "Heart"),
        support.csvStrings("defaultGeneratedObjectTypes"));
  }

  @Test
  void Clamp_ValueOutsideRange_ReturnsNearestBound() {
    NormalizationSupport support = new NormalizationSupport(PROPS);

    assertEquals(10.0, support.clamp(20.0, 0.0, 10.0));
  }
}
