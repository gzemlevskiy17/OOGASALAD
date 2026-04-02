package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.view.core.internal.AbstractAppScreen;

public class PlayerEntryScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.PlayerEntryScreen");

  // Property keys for the lists (properties file is in the resources/ folder)
  private static final String WORLDS_LIST_KEY = "worlds";
  private static final String LEVELS_LIST_KEY = "levels";

  public PlayerEntryScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(d("root.padding")));

    VBox mainContent = new VBox(
        d("main.content.spacing"),
        createLevelChoiceRegion(),
        createStartActionRegion()
    );
    mainContent.setAlignment(Pos.TOP_CENTER);

    root.setCenter(mainContent);
    root.setBottom(createNavigationRegion());

    return root;
  }

  private VBox createLevelChoiceRegion() {
    VBox container = new VBox(d("level.choice.spacing"));
    container.setAlignment(Pos.TOP_CENTER);

    List<String> worlds = getListFromProps(WORLDS_LIST_KEY);
    List<String> levels = getListFromProps(LEVELS_LIST_KEY);

    container.getChildren().addAll(
        createHeaderLabel(),
        createButtonRow(worlds, "world.button.", d("world.region.spacing"), d("world.region.padding")),
        createButtonRow(levels, "level.button.", d("level.region.spacing"), d("level.region.padding")),
        createCommentText()
    );

    return container;
  }

  private HBox createButtonRow(List<String> items, String prefix, double spacing, double padding) {
    HBox row = new HBox(spacing);
    row.setAlignment(Pos.CENTER);
    row.setPadding(new Insets(padding));

    for (String item : items) {
      Button btn = new Button(s(prefix + item));
      styleActionButton(btn);
      btn.setOnAction(e -> handleSelection(prefix, item));
      row.getChildren().add(btn);
    }
    return row;
  }

  private Label createHeaderLabel() {
    Label title = new Label(s("title.text"));
    title.setFont(Font.font(d("title.font.size")));
    return title;
  }

  private Text createCommentText() {
    Text comment = new Text(s("comment.text"));
    comment.setFont(Font.font(d("comment.font.size")));
    return comment;
  }

  private VBox createStartActionRegion() {
    Button startButton = new Button(s("start.button.text"));
    // Transition to the actual game controller

    VBox container = new VBox(d("bad.piggies.spacing"), startButton);
    container.setAlignment(Pos.TOP_CENTER);
    return container;
  }

  private Node createNavigationRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.setOnAction(e -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));

    StackPane wrapper = new StackPane(backButton);
    StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
    return wrapper;
  }

  private void styleActionButton(Button button) {
    button.setFont(Font.font(d("action.font.size")));
    button.setPrefSize(d("action.button.width"), d("action.button.height"));
    button.setWrapText(true);
  }

  private void handleSelection(String category, String id) {
    // Model/Controller will be updated here
    System.out.println("User selected " + category + " id: " + id);
  }

  private List<String> getListFromProps(String key) {
    return Arrays.stream(s(key).split(","))
        .map(String::trim)
        .toList();
  }

  private String s(String key) { return PROPS.getString(key); }

  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}