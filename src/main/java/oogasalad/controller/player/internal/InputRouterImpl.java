package oogasalad.controller.player.internal;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.input.KeyCode;
import oogasalad.controller.player.api.Action;
import oogasalad.controller.player.api.InputRouter;

/**
 * Maps KeyCodes and button IDs to Actions and dispatches them on demand.
 *
 * @author Paulina Vvedenskaya
 */
public class InputRouterImpl implements InputRouter {

  private final Map<KeyCode, Action> keyBindings = new HashMap<>();
  private final Map<String, Action> buttonBindings = new HashMap<>();
  private final Map<String, Action> buttonReleaseBindings = new HashMap<>();

  @Override
  public void bind(KeyCode key, Action action) {
    keyBindings.put(key, action);
  }

  @Override
  public void bind(String buttonId, Action action) {
    buttonBindings.put(buttonId, action);
  }

  @Override
  public void bindRelease(String buttonId, Action action) {
    buttonReleaseBindings.put(buttonId, action);
  }

  @Override
  public void handleKeyPress(KeyCode key) {
    Action action = keyBindings.get(key);
    if (action != null) {
      action.execute();
    }
  }

  @Override
  public void handleButtonPress(String buttonId) {
    Action action = buttonBindings.get(buttonId);
    if (action != null) {
      action.execute();
    }
  }

  @Override
  public void handleButtonRelease(String buttonId) {
    Action action = buttonReleaseBindings.get(buttonId);
    if (action != null) {
      action.execute();
    }
  }
}
