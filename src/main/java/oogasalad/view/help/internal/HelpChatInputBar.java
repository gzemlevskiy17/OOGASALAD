package oogasalad.view.help.internal;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Input row for the help chatbot popup.
 * @author Emilio Garcia
 */
public class HelpChatInputBar {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.help.HelpChatWindow");

  private final TextField inputField;
  private final Button sendButton;
  private Consumer<String> onSend = text -> {
  };

  /**
   * Creates the help chat input row.
   */
  public HelpChatInputBar() {
    this.inputField = new TextField();
    this.sendButton = new Button(s("sendButtonText"));
  }

  /**
   * Returns the input row root node.
   *
   * @return input row root
   */
  public Node getRoot() {
    inputField.setPromptText(s("inputPromptText"));
    inputField.setOnAction(event -> onSend.accept(inputField.getText()));

    sendButton.setPrefWidth(d("sendButtonWidth"));
    sendButton.setOnAction(event -> onSend.accept(inputField.getText()));

    HBox row = new HBox(d("inputSpacing"), inputField, sendButton);
    row.setAlignment(Pos.CENTER_LEFT);
    row.getStyleClass().add(s("inputRowStyleClass"));
    HBox.setHgrow(inputField, Priority.ALWAYS);
    return row;
  }

  /**
   * Registers the send handler invoked when the user submits text.
   *
   * @param onSend callback for submitted text
   */
  public void setOnSend(Consumer<String> onSend) {
    this.onSend = onSend == null ? text -> {
    } : onSend;
  }

  /**
   * Clears the current input text.
   */
  public void clearInput() {
    inputField.clear();
  }

  /**
   * Requests input focus.
   */
  public void requestFocus() {
    inputField.requestFocus();
  }

  /**
   * Enables or disables user input controls.
   *
   * @param enabled true when input should be interactive
   */
  public void setInputEnabled(boolean enabled) {
    sendButton.setDisable(!enabled);
    inputField.setDisable(!enabled);
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
