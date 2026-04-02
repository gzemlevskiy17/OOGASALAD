package oogasalad.view.player.internal;

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

  private static final List<String> WORLD_KEYS = List.of("forest", "desert", "moon", "glacier", "mars");
  private static final List<String> LEVEL_KEYS = List.of("first", "second", "third", "fourth", "fifth");

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

    Label title = new Label(s("title.text"));
    title.setFont(Font.font(d("title.font.size")));

    Text commentText = new Text(s("comment.text"));
    commentText.setFont(Font.font(d("comment.font.size")));

    container.getChildren().addAll(
        title,
        createButtonRow(WORLD_KEYS, "world.button.", d("world.region.spacing"), d("world.region.padding")),
        createButtonRow(LEVEL_KEYS, "level.button.", d("level.region.spacing"), d("level.region.padding")),
        commentText
    );

    return container;
  }

  private VBox createStartActionRegion() {
    Button startButton = new Button(s("start.button.text"));
    // Event handler for starting the game

    VBox container = new VBox(d("bad.piggies.spacing"), startButton);
    container.setAlignment(Pos.TOP_CENTER);
    return container;
  }

  /**
   * Dynamically creates an HBox of buttons based on property keys.
   */
  private HBox createButtonRow(List<String> subKeys, String prefix, double spacing, double padding) {
    HBox row = new HBox(spacing);
    row.setAlignment(Pos.CENTER);
    row.setPadding(new Insets(padding));

    for (String key : subKeys) {
      Button btn = new Button(s(prefix + key));
      configureActionButton(btn);
      // In the future: btn.setOnAction(e -> handleSelection(key));
      row.getChildren().add(btn);
    }

    return row;
  }

  private void configureActionButton(Button button) {
    button.setFont(Font.font(d("action.font.size")));
    button.setPrefSize(d("action.button.width"), d("action.button.height"));
    button.setWrapText(true);
  }

  private Node createNavigationRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.setOnAction(event -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));

    StackPane container = new StackPane(backButton);
    StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
    return container;
  }

  // Helper methods for property access
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}