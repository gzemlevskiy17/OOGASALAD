package oogasalad.view.authoring.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * Loads suggested prompts and hover explanations for authoring numeric/string fields so designers
 * see reasonable ranges without hard-validating inputs.
 */
public final class AuthoringValueHints {
  private static final double DEFAULT_WIDTH = 220;

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AuthoringValueHints");

  private AuthoringValueHints() {
  }

  /**
   * Text field with catalog/default width, prompt text, and tooltip when bundle entries exist.
   */
  public static TextField newTextField(String baseKey) {
    return newTextField(baseKey, DEFAULT_WIDTH);
  }

  /**
   * Same as {@link #newTextField(String)} with explicit preferred width (e.g. sprite rows).
   */
  public static TextField newTextField(String baseKey, double prefWidth) {
    TextField field = new TextField();
    field.setPrefWidth(prefWidth);
    configure(field, baseKey);
    return field;
  }

  /**
   * Sets {@link TextField#setPromptText prompt} (optional) and {@link Control#setTooltip tooltip}
   * from keys {@code prompt.<baseKey>} and {@code tooltip.<baseKey>}.
   */
  public static void configure(TextField field, String baseKey) {
    try {
      field.setPromptText(PROPS.getString("prompt." + baseKey));
    } catch (MissingResourceException ignored) {
      // optional prompt
    }
    attach(field, baseKey);
  }

  /**
   * Tooltip only — for spinners or fields created elsewhere.
   */
  public static void attach(Control control, String baseKey) {
    try {
      String tip = PROPS.getString("tooltip." + baseKey);
      control.setTooltip(new Tooltip(tip));
    } catch (MissingResourceException ignored) {
      // optional tooltip
    }
  }
}
