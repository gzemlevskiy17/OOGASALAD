package oogasalad.controller.authoring.internal;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import oogasalad.model.authoring.api.GeneratedLevelResult;
import oogasalad.model.authoring.internal.GeneratedLevelModel;
import oogasalad.view.authoring.internal.AiLevelGenerationDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Coordinates the AI level generation prompt flow from dialog submission through asynchronous model
 * execution and success/error handoff back to the authoring UI.
 * @author Emilio Garcia
 */
public class AiLevelGenerationCoordinator {

  private static final Logger LOG = LogManager.getLogger(AiLevelGenerationCoordinator.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.controller.authoring.AiLevelGenerationCoordinator");

  private final GeneratedLevelModel model;
  private final AiLevelGenerationDialog dialog;
  private final Consumer<GeneratedLevelResult> successHandler;

  /**
   * Creates an AI level generation coordinator with the default dialog implementation.
   *
   * @param model generated-level orchestration model
   * @param successHandler callback invoked when generation completes successfully
   */
  public AiLevelGenerationCoordinator(GeneratedLevelModel model,
      Consumer<GeneratedLevelResult> successHandler) {
    this(model, new AiLevelGenerationDialog(), successHandler);
  }

  /**
   * Creates an AI level generation coordinator with injected collaborators.
   *
   * @param model generated-level orchestration model
   * @param dialog prompt dialog used to collect user input
   * @param successHandler callback invoked when generation completes successfully
   */
  public AiLevelGenerationCoordinator(GeneratedLevelModel model, AiLevelGenerationDialog dialog,
      Consumer<GeneratedLevelResult> successHandler) {
    this.model = Objects.requireNonNull(model, PROPS.getString("nullModelError"));
    this.dialog = Objects.requireNonNull(dialog, PROPS.getString("nullDialogError"));
    this.successHandler = Objects.requireNonNull(successHandler,
        PROPS.getString("nullSuccessHandlerError"));
  }

  /**
   * Opens the AI generation dialog and, when the user submits a prompt, runs the generation
   * pipeline asynchronously.
   *
   * @param owner owner window for modal dialog display and error alerts
   */
  public void open(Window owner) {
    dialog.showAndWait(owner).ifPresent(prompt -> generateAsync(prompt, owner));
  }

  private void generateAsync(String prompt, Window owner) {
    CompletableFuture.runAsync(() -> {
      try {
        GeneratedLevelResult result = model.generateLevel(prompt);
        Platform.runLater(() -> successHandler.accept(result));
      } catch (RuntimeException exception) {
        LOG.error(PROPS.getString("generationFailureLogMessage"), exception);
        Platform.runLater(() -> showFailureAlert(owner, exception.getMessage()));
      }
    });
  }

  private void showFailureAlert(Window owner, String message) {
    Alert alert = new Alert(AlertType.ERROR, message, ButtonType.OK);
    if (owner != null) {
      alert.initOwner(owner);
    }
    alert.setTitle(PROPS.getString("failureAlertTitle"));
    alert.setHeaderText(PROPS.getString("failureAlertHeader"));
    alert.showAndWait();
  }
}
