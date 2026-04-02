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

    VBox levelChoiceContent = createLevelChoiceContentRegion();

    VBox startBadPiggiesContent = createStartBadPiggiesContentRegion();

    displayMainContent(levelChoiceContent, startBadPiggiesContent, root);

    createBackButton(root);

    return root;
  }

  private void displayMainContent(VBox levelChoiceContent, VBox startBadPiggiesContent, BorderPane root) {
    VBox mainContent = new VBox(d("main.content.spacing"), levelChoiceContent,
        startBadPiggiesContent);
    mainContent.setAlignment(Pos.TOP_CENTER);
    root.setCenter(mainContent);
  }

  private void createBackButton(BorderPane root) {
    Button backButton = new Button(s("back.button.text"));
    backButton.setOnAction(event -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));
    root.setBottom(wrapBottomLeft(backButton));
  }

  private VBox createStartBadPiggiesContentRegion() {
    Button startYourBadPiggiesButton = createYourBadPiggiesButtonRegion();

    VBox startBadPiggiesContent = new VBox(d("bad.piggies.spacing"), startYourBadPiggiesButton);
    startBadPiggiesContent.setAlignment(Pos.TOP_CENTER);
    return startBadPiggiesContent;
  }

  private VBox createLevelChoiceContentRegion() {
    Label title = createTitleRegion();

    HBox worldButtonRegion = createWorldButtonRegion();

    HBox levelButtonRegion = createLevelButtonRegion();

    Text commentText = createCommentRegion();

    VBox levelChoiceContent = new VBox(d("level.choice.spacing"), title, worldButtonRegion, levelButtonRegion, commentText);
    levelChoiceContent.setAlignment(Pos.TOP_CENTER);
    return levelChoiceContent;
  }

  private Button createYourBadPiggiesButtonRegion() {
    Button startYourBadPiggiesButton = new Button(s("start.button.text"));
    return startYourBadPiggiesButton;
  }

  private Label createTitleRegion() {
    Label title = new Label(s("title.text"));
    title.setFont(Font.font(d("title.font.size")));
    return title;
  }

  private Text createCommentRegion() {
    Text commentText = new Text(s("comment.text"));
    configureText(commentText);
    return commentText;
  }

  private HBox createLevelButtonRegion() {
    Button firstLevelButton = new Button(configureFirstLevelButtonText());
    configureLevelButton(firstLevelButton);

    Button secondLevelButton = new Button(configureSecondLevelButtonText());
    configureLevelButton(secondLevelButton);

    Button thirdLevelButton = new Button(configureThirdLevelButtonText());
    configureLevelButton(thirdLevelButton);

    Button fourthLevelButton = new Button(configureFourthLevelButtonText());
    configureLevelButton(fourthLevelButton);

    Button fifthLevelButton = new Button(configureFifthLevelButtonText());
    configureLevelButton(fifthLevelButton);

    HBox levelButtonRegion = new HBox(d("level.region.spacing"), firstLevelButton, secondLevelButton, thirdLevelButton, fourthLevelButton, fifthLevelButton);
    levelButtonRegion.setAlignment(Pos.CENTER);
    levelButtonRegion.setPadding(new Insets(d("level.region.padding")));
    return levelButtonRegion;
  }

  private HBox createWorldButtonRegion() {
    Button forestWorldButton = new Button(configureForestWorldButtonText());
    configureWorldButton(forestWorldButton);

    Button desertWorldButton = new Button(configureDesertWorldButtonText());
    configureWorldButton(desertWorldButton);

    Button moonWorldButton = new Button(configureMoonWorldButtonText());
    configureWorldButton(moonWorldButton);

    Button glacierWorldButton = new Button(configureGlacierWorldButtonText());
    configureWorldButton(glacierWorldButton);

    Button marsWorldButton = new Button(configureMarsWorldButtonText());
    configureWorldButton(marsWorldButton);

    HBox worldButtonRegion = new HBox(d("world.region.spacing"), forestWorldButton, desertWorldButton, moonWorldButton, glacierWorldButton, marsWorldButton);
    worldButtonRegion.setAlignment(Pos.CENTER);
    worldButtonRegion.setPadding(new Insets(d("world.region.padding")));
    return worldButtonRegion;
  }

  private String configureForestWorldButtonText() {
    return s("world.button.forest");
  }

  private String configureDesertWorldButtonText() {
    return s("world.button.desert");
  }

  private String configureMoonWorldButtonText() {
    return s("world.button.moon");
  }

  private String configureGlacierWorldButtonText() {
    return s("world.button.glacier");
  }

  private String configureMarsWorldButtonText() {
    return s("world.button.mars");
  }

  private String configureFirstLevelButtonText() {
    return s("level.button.first");
  }

  private String configureSecondLevelButtonText() {
    return s("level.button.second");
  }

  private String configureThirdLevelButtonText() {
    return s("level.button.third");
  }

  private String configureFourthLevelButtonText() {
    return s("level.button.fourth");
  }

  private String configureFifthLevelButtonText() {
    return s("level.button.fifth");
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