package oogasalad.controller.player.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests InputRouterImpl by creating a mock InputRouterImpl
 * Checks that key and button press executes the action, different key and button presses don't do anything.

 */
class InputRouterImplTest {

  private InputRouterImpl router;

  @BeforeEach
  void setUp() {
    router = new InputRouterImpl();
  }

  @Test
  void bindKey_handleKeyPress_executesAction() {
    int[] count = {0};
    router.bind(KeyCode.W, () -> count[0]++);
    router.handleKeyPress(KeyCode.W);

    assertEquals(1, count[0]);
  }

  @Test
  void bindKey_pressDifferentKey_doesNotExecute() {
    int[] count = {0};
    router.bind(KeyCode.W, () -> count[0]++);

    router.handleKeyPress(KeyCode.SPACE);

    assertEquals(0, count[0]);
  }

  @Test
  void bindButton_handleButtonPress_executesAction() {
    int[] count = {0};
    router.bind("SOME_TEST_BUTTON", () -> count[0]++);

    router.handleButtonPress("SOME_TEST_BUTTON");

    assertEquals(1, count[0]);
  }

  @Test
  void bindButton_pressDifferentButton_doesNotExecute() {
    int[] count = {0};
    router.bind("SOME_TEST_BUTTON", () -> count[0]++);

    router.handleButtonPress("OTHER_BUTTON");

    assertEquals(0, count[0]);
  }

  @Test
  void handleKeyPress_unboundKey_doesNotThrow() {
    assertDoesNotThrow(() -> router.handleKeyPress(KeyCode.SPACE));
  }

  @Test
  void handleButtonPress_unboundButton_doesNotThrow() {
    assertDoesNotThrow(() -> router.handleButtonPress("UNKNOWN_BUTTON"));
  }
}

