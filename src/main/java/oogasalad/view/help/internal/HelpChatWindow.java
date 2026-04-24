package oogasalad.view.help.internal;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import oogasalad.controller.help.internal.HelpChatCoordinator;
import oogasalad.model.help.api.ChatMessage;
import oogasalad.model.help.internal.HelpChatModel;

/**
 * Draggable popup chat window used to provide in-game help through an LLM-backed assistant.
 *
 * @author Emilio Garcia
 */
public class HelpChatWindow {

  private static final ResourceBundle PROPS =
      ResourceBundle.getBundle("oogasalad.view.help.HelpChatWindow");

  private final StackPane root;
  private final HelpChatHistoryView historyView;
  private final HelpChatInputBar inputBar;
  private final Button closeButton;
  private final PopupDragSupport dragSupport;
  private final HelpChatCoordinator coordinator;

  /**
   * Creates a help chat popup window backed by the given model.
   *
   * @param model stateful chat model used to generate responses
   * @author Emilio Garcia
   */
  public HelpChatWindow(HelpChatModel model) {
    if (model == null) {
      throw new IllegalArgumentException(s("nullHelpChatModelError"));
    }
    this.historyView = new HelpChatHistoryView();
    this.inputBar = new HelpChatInputBar();
    this.closeButton = new Button(s("closeButtonText"));
    this.dragSupport = new PopupDragSupport();
    this.coordinator = new HelpChatCoordinator(model, historyView, inputBar);
    this.root = buildRoot();
    historyView.appendMessage(ChatMessage.developerMessage(s("emptyStateMessage")));
    hide();
  }

  /**
   * Returns the popup root node so callers can add it to an existing scene graph.
   *
   * @return chat popup root
   * @author Emilio Garcia
   */
  public Parent getRoot() {
    return root;
  }

  /**
   * Shows the popup window.
   *
   * @author Emilio Garcia
   */
  public void show() {
    root.setManaged(true);
    root.setVisible(true);
    inputBar.requestFocus();
  }

  /**
   * Hides the popup window.
   *
   * @author Emilio Garcia
   */
  public void hide() {
    root.setManaged(false);
    root.setVisible(false);
  }

  /**
   * Toggles popup visibility.
   *
   * @author Emilio Garcia
   */
  public void toggle() {
    if (root.isVisible()) {
      hide();
    } else {
      show();
    }
  }

  private StackPane buildRoot() {
    VBox container = new VBox(d("rootSpacing"));
    container.setPadding(new Insets(d("rootPadding")));
    container.getStyleClass().add(s("rootStyleClass"));
    container.setPrefWidth(d("windowWidth"));
    container.setMinWidth(d("windowWidth"));
    container.setMaxWidth(d("windowWidth"));
    container.setPrefHeight(d("windowHeight"));
    container.setMinHeight(d("windowHeight"));
    container.setMaxHeight(d("windowHeight"));
    container.getStylesheets().add(
        getClass().getResource("/oogasalad/view/help/help-chat.css").toExternalForm());

    HBox header = buildHeader();
    VBox body = new VBox(d("contentSpacing"), historyView.getRoot(), buildInputRow());
    body.getStyleClass().add(s("bodyStyleClass"));
    VBox.setVgrow(body, Priority.ALWAYS);

    container.getChildren().addAll(header, body);

    StackPane popup = new StackPane(container);
    popup.setPickOnBounds(false);
    StackPane.setAlignment(container, Pos.TOP_RIGHT);
    StackPane.setMargin(container, new Insets(d("windowOffsetTop"), d("windowOffsetRight"), 0, 0));
    return popup;
  }

  private HBox buildHeader() {
    Label title = new Label(s("headerTitle"));
    title.getStyleClass().add(s("titleStyleClass"));

    closeButton.setOnAction(event -> hide());

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox header = new HBox(d("headerSpacing"), title, spacer, closeButton);
    header.setPadding(new Insets(d("headerPadding")));
    header.setAlignment(Pos.CENTER_LEFT);
    header.getStyleClass().add(s("headerStyleClass"));
    dragSupport.enableDragging(header, root);
    return header;
  }

  private Node buildInputRow() {
    inputBar.setOnSend(coordinator::handleSend);
    return inputBar.getRoot();
  }

  private String s(String key) {
    return PROPS.getString(key);
  }

  private double d(String key) {
    return Double.parseDouble(PROPS.getString(key));
  }
}
