package oogasalad.view.help.internal;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import oogasalad.model.help.api.ChatMessage;

/**
 * Visual transcript area for the help chatbot popup.
 *
 * @author Emilio Garcia
 */
public class HelpChatHistoryView {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.help.HelpChatWindow");

  private final VBox chatHistory;
  private final ScrollPane root;

  /**
   * Creates the transcript view used by the help chat popup.
   *
   * @author Emilio Garcia
   */
  public HelpChatHistoryView() {
    this.chatHistory = new VBox(d("messageSpacing"));
    this.root = buildRoot();
  }

  /**
   * Returns the root node for the transcript area.
   *
   * @return transcript view root
   * @author Emilio Garcia
   */
  public Node getRoot() {
    return root;
  }

  /**
   * Appends a chat message to the visible transcript.
   *
   * @param message chat message to render
   * @author Emilio Garcia
   */
  public void appendMessage(ChatMessage message) {
    removeWaitingMessageIfPresent();

    Label bubble = new Label(message.content());
    bubble.setWrapText(true);
    bubble.setMaxWidth(d("messageMaxWidth"));
    bubble.getStyleClass().add(s("chatBubbleStyleClass"));
    bubble.getStyleClass().add(resolveRoleStyleClass(message.role()));

    HBox messageBox = new HBox(bubble);
    messageBox.setAlignment(resolveAlignment(message.role()));
    chatHistory.getChildren().add(messageBox);
  }

  /**
   * Shows the transient assistant waiting message.
   *
   * @author Emilio Garcia
   */
  public void showWaitingMessage() {
    appendMessage(ChatMessage.assistantMessage(s("aiWaitingMessage")));
  }

  /**
   * Removes the transient assistant waiting message when present.
   *
   * @author Emilio Garcia
   */
  public void removeWaitingMessageIfPresent() {
    if (chatHistory.getChildren().isEmpty()) {
      return;
    }
    HBox lastMessageBox = (HBox) chatHistory.getChildren().getLast();
    Label lastLabel = (Label) lastMessageBox.getChildren().getFirst();
    if (lastLabel.getText().equals(s("aiWaitingMessage"))) {
      chatHistory.getChildren().removeLast();
    }
  }

  private ScrollPane buildRoot() {
    chatHistory.setPadding(new Insets(d("headerPadding")));
    chatHistory.getStyleClass().add(s("chatHistoryStyleClass"));
    chatHistory.setPrefHeight(d("chatHistoryPrefHeight"));

    ScrollPane scrollPane = new ScrollPane(chatHistory);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefHeight(d("chatHistoryPrefHeight"));
    scrollPane.getStyleClass().add(s("chatHistoryStyleClass"));
    return scrollPane;
  }

  private String resolveRoleStyleClass(ChatMessage.Role role) {
    return switch (role) {
      case USER -> s("userMessageStyleClass");
      case ASSISTANT -> s("assistantMessageStyleClass");
      case DEVELOPER -> s("systemMessageStyleClass");
    };
  }

  private Pos resolveAlignment(ChatMessage.Role role) {
    return switch (role) {
      case USER -> Pos.CENTER_RIGHT;
      case ASSISTANT, DEVELOPER -> Pos.CENTER_LEFT;
    };
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
