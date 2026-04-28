package oogasalad.controller.help.internal;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import oogasalad.model.help.api.ChatMessage;
import oogasalad.model.help.internal.HelpChatModel;
import oogasalad.view.help.internal.HelpChatHistoryView;
import oogasalad.view.help.internal.HelpChatInputBar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Coordinates the help chat interaction between the popup UI and the chat model.
 *
 * @author Emilio Garcia
 */
public class HelpChatCoordinator {

  private static final Logger LOG = LogManager.getLogger(HelpChatCoordinator.class);
  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.controller.help.HelpChatCoordinator");
  private static final ResourceBundle VIEW_PROPS =
      ResourceBundle.getBundle("oogasalad.view.help.HelpChatWindow");
  private static final ResourceBundle MODEL_PROPS =
      ResourceBundle.getBundle("oogasalad.model.help.HelpChat");

  private final HelpChatModel model;
  private final HelpChatHistoryView historyView;
  private final HelpChatInputBar inputBar;

  /**
   * Creates a coordinator for the help chat popup.
   *
   * @param model backing chat model
   * @param historyView transcript view
   * @param inputBar input controls
   * @author Emilio Garcia
   */
  public HelpChatCoordinator(HelpChatModel model, HelpChatHistoryView historyView,
      HelpChatInputBar inputBar) {
    this.model = Objects.requireNonNull(model, PROPS.getString("nullModelError"));
    this.historyView = Objects.requireNonNull(historyView, PROPS.getString("nullHistoryViewError"));
    this.inputBar = Objects.requireNonNull(inputBar, PROPS.getString("nullInputBarError"));
  }

  /**
   * Handles a submitted user message, updates the view immediately, and requests a response in the
   * background.
   *
   * @param submittedText raw submitted user text
   * @author Emilio Garcia
   */
  public void handleSend(String submittedText) {
    String userText = submittedText.trim();
    if (userText.isEmpty()) {
      LOG.warn(MODEL_PROPS.getString("emptyUserMessageWarning"));
      return;
    }

    LOG.info("User asked help chat question (length={})", userText.length());
    historyView.appendMessage(ChatMessage.userMessage(userText));
    inputBar.clearInput();
    inputBar.setInputEnabled(false);
    historyView.showWaitingMessage();

    CompletableFuture.runAsync(() -> {
      long startMillis = System.currentTimeMillis();
      try {
        ChatMessage response = model.getResponse(userText);
        LOG.info("Help chat response received in {}ms", System.currentTimeMillis() - startMillis);
        Platform.runLater(() -> historyView.appendMessage(response));
      } catch (RuntimeException exception) {
        LOG.error(PROPS.getString("requestFailureLogMessage"), exception);
        Platform.runLater(() -> historyView.appendMessage(
            ChatMessage.developerMessage(
                VIEW_PROPS.getString("responseErrorPrefix") + " " + exception.getMessage())));
      } finally {
        Platform.runLater(() -> {
          inputBar.setInputEnabled(true);
          inputBar.requestFocus();
        });
      }
    });
  }
}
