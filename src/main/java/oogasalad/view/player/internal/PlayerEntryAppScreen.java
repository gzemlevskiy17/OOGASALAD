package oogasalad.view.player.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.view.core.internal.AbstractAppScreen;

/**
 * Screen on which the player chooses the world and the level they want to play
 * @author Iurii Beliaev
 */

public class PlayerEntryAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.PlayerEntryScreen");

  public PlayerEntryAppScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(
        getClass().getResource("/oogasalad/view/player/player-entry.css").toExternalForm()
    );
    root.getStyleClass().add("screen-root");

    VBox mainContent = new VBox(d("main.content.spacing"));
    mainContent.setAlignment(Pos.CENTER);

    // Assembling the regions
    mainContent.getChildren().addAll(
        createLevelChoiceContentRegion(),
        createStartBadPiggiesContentRegion()
    );

    root.setCenter(mainContent);
    root.setBottom(createBackButtonRegion());

    return root;
  }

  private VBox createLevelChoiceContentRegion() {
    VBox container = new VBox(d("level.choice.spacing"));
    container.getStyleClass().add("selection-region");
    container.setAlignment(Pos.TOP_CENTER);

    container.getChildren().addAll(
        createTitleRegion(),
        createWorldButtonRegion(),
        createLevelButtonRegion(),
        createCommentRegion()
    );
    return container;
  }

  private VBox createStartBadPiggiesContentRegion() {
    Button startButton = new Button(s("start.button.text"));
    startButton.getStyleClass().add("start-button");
    startButton.setOnAction(event -> {
      screenManager.switchScreen(ScreenFactory.VEHICLE_BUILDER_APP_SCREEN_KEY, null);
    });

    VBox container = new VBox(startButton);
    container.getStyleClass().add("selection-region");
    container.setAlignment(Pos.CENTER);
    return container;
  }

  private HBox createWorldButtonRegion() {
    HBox worldBox = new HBox(d("world.region.spacing"));
    worldBox.setAlignment(Pos.CENTER);

    for (String world : getList("worlds")) {
      Button btn = new Button(s("world.button." + world));
      btn.getStyleClass().add("world-button");
      btn.getStyleClass().add("world-" + world); // Hovering logic
      worldBox.getChildren().add(btn);
    }
    return worldBox;
  }

  private HBox createLevelButtonRegion() {
    HBox levelBox = new HBox(d("level.region.spacing"));
    levelBox.setAlignment(Pos.CENTER);

    for (String level : getList("levels")) {
      Button btn = new Button(s("level.button." + level));
      btn.getStyleClass().add("level-button");
      // high levels are not open
      if (level.contains("locked")) btn.getStyleClass().add("filled");
      levelBox.getChildren().add(btn);
    }
    return levelBox;
  }

  private Label createTitleRegion() {
    Label title = new Label(s("title.text"));
    title.getStyleClass().add("header-title");
    return title;
  }

  private Text createCommentRegion() {
    Text comment = new Text(s("comment.text"));
    comment.getStyleClass().add("comment-text");
    return comment;
  }

  private Node createBackButtonRegion() {
    Button backButton = new Button(s("back.button.text"));
    backButton.getStyleClass().add("back-button");
    backButton.setOnAction(e -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));

    StackPane container = new StackPane(backButton);
    StackPane.setAlignment(backButton, Pos.BOTTOM_LEFT);
    container.getStyleClass().add("nav-region");
    return container;
  }

  private List<String> getList(String key) { return Arrays.asList(s(key).split(",")); }
  private String s(String key) { return PROPS.getString(key); }
  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}
