package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

  public PlayerEntryScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(d("root.padding")));

    Label title = new Label(s("title.text"));
    title.setFont(Font.font(d("title.font.size")));

    Button forestWorldButton = new Button(s("world.button.1"));
    configureWorldButton(forestWorldButton);

    Button desertWorldButton = new Button(s("world.button.2"));
    configureWorldButton(desertWorldButton);

    Button moonWorldButton = new Button(s("world.button.3"));
    configureWorldButton(moonWorldButton);

    Button glacierWorldButton = new Button(s("world.button.4"));
    configureWorldButton(glacierWorldButton);

    Button marsWorldButton = new Button(s("world.button.5"));
    configureWorldButton(marsWorldButton);

    HBox worldButtonRegion = new HBox(d("world.region.spacing"), forestWorldButton, desertWorldButton, moonWorldButton, glacierWorldButton, marsWorldButton);
    worldButtonRegion.setAlignment(Pos.CENTER);
    worldButtonRegion.setPadding(new Insets(d("world.region.padding")));

    Button firstLevelButton = new Button(s("level.button.1"));
    configureLevelButton(firstLevelButton);

    Button secondLevelButton = new Button(s("level.button.2"));
    configureLevelButton(secondLevelButton);

    Button thirdLevelButton = new Button(s("level.button.3"));
    configureLevelButton(thirdLevelButton);

    Button fourthLevelButton = new Button(s("level.button.4"));
    configureLevelButton(fourthLevelButton);

    Button fifthLevelButton = new Button(s("level.button.5"));
    configureLevelButton(fifthLevelButton);

    HBox levelButtonRegion = new HBox(d("level.region.spacing"), firstLevelButton, secondLevelButton, thirdLevelButton, fourthLevelButton, fifthLevelButton);
    levelButtonRegion.setAlignment(Pos.CENTER);
    levelButtonRegion.setPadding(new Insets(d("level.region.padding")));

    Text commentText = new Text(s("comment.text"));
    configureText(commentText);

    VBox levelChoiceContent = new VBox(d("level.choice.spacing"), title, worldButtonRegion, levelButtonRegion, commentText);
    levelChoiceContent.setAlignment(Pos.TOP_CENTER);

    Button startYourBadPiggiesButton = new Button(s("start.button.text"));

    VBox startBadPiggiesContent = new VBox(d("bad.piggies.spacing"), startYourBadPiggiesButton);
    startBadPiggiesContent.setAlignment(Pos.TOP_CENTER);

    VBox mainContent = new VBox(d("main.content.spacing"), levelChoiceContent, startBadPiggiesContent);
    mainContent.setAlignment(Pos.TOP_CENTER);
    root.setCenter(mainContent);

    Button backButton = new Button(s("back.button.text"));
    backButton.setOnAction(event -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));
    root.setBottom(wrapBottomLeft(backButton));

    return root;
  }

  private void configureText(Text commentText) {
    commentText.setFont(Font.font(d("comment.font.size")));
  }

  private void configureWorldButton(Button button) {
    button.setFont(Font.font(d("action.font.size")));
    button.setPrefSize(d("action.button.width"), d("action.button.height"));
    button.setWrapText(true);
  }

  private void configureLevelButton(Button button) {
    button.setFont(Font.font(d("action.font.size")));
    button.setPrefSize(d("action.button.width"), d("action.button.height"));
    button.setWrapText(true);
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }

  private String s(String key) { return PROPS.getString(key); }

  private double d(String key) { return Double.parseDouble(PROPS.getString(key)); }
}