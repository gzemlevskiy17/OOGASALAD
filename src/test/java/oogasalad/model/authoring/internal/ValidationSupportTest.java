package oogasalad.model.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ResourceBundle;
import oogasalad.model.authoring.api.GeneratedLevelSpecValidationException;
import org.junit.jupiter.api.Test;

/**
 * Tests validation helper behavior for generated-level validators.
 *
 * @author Emilio Garcia
 */
class ValidationSupportTest {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.model.authoring.GeneratedLevelSpecValidator");

  @Test
  void RequirePresent_NonNullValue_DoesNotThrow() {
    ValidationSupport support = new ValidationSupport(PROPS);

    assertDoesNotThrow(() -> support.requirePresent("nullSpecError", new Object()));
  }

  @Test
  void ValidateRange_ValueOutsideRange_ThrowsGeneratedLevelSpecValidationException() {
    ValidationSupport support = new ValidationSupport(PROPS);

    assertThrows(GeneratedLevelSpecValidationException.class,
        () -> support.validateRange("screenWidthOutOfRangeError", 1.0, 800.0, 20000.0));
  }

  @Test
  void N_IntegerPropertyKey_ReturnsParsedIntegerValue() {
    ValidationSupport support = new ValidationSupport(PROPS);

    assertEquals(2, support.n("minKnotCount"));
  }
}
