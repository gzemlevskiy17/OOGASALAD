package oogasalad.view.authoring.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests vehicle-selection-limit exception construction.
 *
 * @author Emilio Garcia
 */
class VehicleSelectionLimitExceptionTest {

  @Test
  void Constructor_MessageProvided_StoresMessage() {
    VehicleSelectionLimitException exception =
        new VehicleSelectionLimitException("too many parts");

    assertEquals("too many parts", exception.getMessage());
  }
}
