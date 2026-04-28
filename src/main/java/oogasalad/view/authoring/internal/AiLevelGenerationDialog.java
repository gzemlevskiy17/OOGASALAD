package oogasalad.view.authoring.internal;

import java.util.Optional;
import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Dialog that collects a free-form prompt for AI level generation and exposes simple loading and
 * error state controls for later coordinator wiring.
 * @author Emilio Garcia
 */
public class AiLevelGenerationDialog {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.authoring.AiLevelGenerationDialog");

  private final Dialog<String> dialog;
  private final TextArea promptArea;
  private final Label statusLabel;
  private final ButtonType generateButtonType;
  private final ButtonType cancelButtonType;

  /**
   * Creates an AI level generation prompt dialog.
   */
  public AiLevelGenerationDialog() {
    this.dialog = new Dialog<>();
    this.promptArea = new TextArea();
    this.statusLabel = new Label();
    this.generateButtonType = new ButtonType(s("generateButtonText"), ButtonData.OK_DONE);
    this.cancelButtonType = new ButtonType(s("cancelButtonText"), ButtonData.CANCEL_CLOSE);
    configureDialog();
  }

  /**
   * Shows the dialog and returns the trimmed prompt when the generate action is confirmed.
   *
   * @param owner owning window for modality and positioning
   * @return optional trimmed prompt text
   */
  public Optional<String> showAndWait(Window owner) {
    if (owner != null) {
      dialog.initOwner(owner);
    }
    clearStatusMessage();
    setLoading(false);
    return dialog.showAndWait()
        .map(String::trim)
        .filter(prompt -> !prompt.isBlank());
  }

  /**
   * Returns the current prompt text.
   *
   * @return trimmed prompt text
   */
  public String getPromptText() {
    return promptArea.getText().trim();
  }

  /**
   * Replaces the current prompt text.
   *
   * @param promptText new prompt text
   */
  public void setPromptText(String promptText) {
    promptArea.setText(promptText == null ? "" : promptText);
  }

  /**
   * Updates the dialog to reflect whether generation is currently in progress.
   *
   * @param loading true when generation is in progress
   */
  public void setLoading(boolean loading) {
    promptArea.setDisable(loading);
    Button generateButton = (Button) dialog.getDialogPane().lookupButton(generateButtonType);
    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
    if (generateButton != null) {
      generateButton.setDisable(loading);
      generateButton.setText(loading ? s("loadingButtonText") : s("generateButtonText"));
    }
    if (cancelButton != null) {
      cancelButton.setDisable(loading);
    }
  }

  /**
   * Displays an inline error/status message.
   *
   * @param message message to display
   */
  public void showStatusMessage(String message) {
    String normalizedMessage = message == null ? "" : message.trim();
    statusLabel.setManaged(!normalizedMessage.isEmpty());
    statusLabel.setVisible(!normalizedMessage.isEmpty());
    statusLabel.setText(normalizedMessage);
  }

  /**
   * Clears any inline status/error message.
   */
  public void clearStatusMessage() {
    showStatusMessage("");
  }

  /**
   * Closes the dialog.
   */
  public void close() {
    dialog.close();
  }

  private void configureDialog() {
    dialog.setTitle(s("titleText"));
    dialog.setHeaderText(s("headerText"));
    dialog.setResultConverter(buttonType ->
        generateButtonType.equals(buttonType) ? getPromptText() : null);

    DialogPane pane = dialog.getDialogPane();
    pane.getButtonTypes().addAll(generateButtonType, cancelButtonType);
    pane.setContent(buildContent());
    pane.setPrefWidth(d("dialogWidth"));
    pane.setPrefHeight(d("dialogHeight"));
    pane.getStylesheets().add(
        getClass().getResource("/oogasalad/view/authoring/ai-level-generation-dialog.css")
            .toExternalForm());
    pane.getStyleClass().add(s("dialogPaneStyleClass"));
  }

  private VBox buildContent() {
    Label promptLabel = new Label(s("promptLabelText"));
    promptLabel.getStyleClass().add(s("promptLabelStyleClass"));

    promptArea.setPromptText(s("promptTextPrompt"));
    promptArea.setWrapText(true);
    promptArea.setPrefColumnCount(n("promptColumns"));
    promptArea.setPrefRowCount(n("promptRows"));
    promptArea.getStyleClass().add(s("promptAreaStyleClass"));

    statusLabel.getStyleClass().add(s("statusLabelStyleClass"));
    statusLabel.setManaged(false);
    statusLabel.setVisible(false);

    VBox content = new VBox(d("contentSpacing"), promptLabel, promptArea, statusLabel);
    content.setPadding(new Insets(d("contentPadding")));
    content.getStyleClass().add(s("contentRootStyleClass"));
    return content;
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }

  private int n(String key) {
    return Integer.parseInt(PROPS.getString(key));
  }
}
