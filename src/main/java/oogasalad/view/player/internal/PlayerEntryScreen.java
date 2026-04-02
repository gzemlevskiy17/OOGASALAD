package oogasalad.view.player.internal;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.view.core.internal.AbstractAppScreen;

public class PlayerEntryScreen extends AbstractAppScreen {

  private static final double ROOT_PADDING = 24;
  private static final double TITLE_FONT_SIZE = 36;
  private static final double ACTION_FONT_SIZE = 22;
  private static final double TITLE_SECTION_SPACING = 28;
  private static final double WORLD_BUTTON_REGION_SPACING = 24;
  private static final double LEVEL_BUTTON_REGION_SPACING = 24;
  private static final double WORLD_BUTTON_REGION_PADDING = 32;
  private static final double LEVEL_BUTTON_REGION_PADDING = 32;
  private static final double ACTION_BUTTON_WIDTH = 300;
  private static final double ACTION_BUTTON_HEIGHT = 180;

  public PlayerEntryScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(ROOT_PADDING));

    Label title = new Label("Quick start with our Good Piggies");
    title.setFont(Font.font(TITLE_FONT_SIZE));

    Button forestWorldButton = new Button("World 1: Forest");
    configureWorldButton(forestWorldButton);

    Button desertWorldButton = new Button("World 2: Desert");
    configureWorldButton(desertWorldButton);

    Button moonWorldButton = new Button("World 3: Moon");
    configureWorldButton(moonWorldButton);

    Button glacierWorldButton = new Button("World 4: Glacier");
    configureWorldButton(glacierWorldButton);

    Button marsWorldButton = new Button("World 5: Mars");
    configureWorldButton(marsWorldButton);

    HBox worldButtonRegion = new HBox(WORLD_BUTTON_REGION_SPACING, forestWorldButton, desertWorldButton, moonWorldButton, glacierWorldButton, marsWorldButton);
    worldButtonRegion.setAlignment(Pos.CENTER);
    worldButtonRegion.setPadding(new Insets(WORLD_BUTTON_REGION_PADDING));

    Button firstLevelButton = new Button("Level 1: 54");
    configureLevelButton(firstLevelButton);

    Button secondLevelButton = new Button("Level 2: 38");
    configureLevelButton(secondLevelButton);

    Button thirdLevelButton = new Button("Level 3: 24");
    configureLevelButton(thirdLevelButton);

    Button fourthLevelButton = new Button("Level 4: 28");
    configureLevelButton(fourthLevelButton);

    Button fifthLevelButton = new Button("Level 5: 43");
    configureLevelButton(fifthLevelButton);

    HBox levelButtonRegion = new HBox(LEVEL_BUTTON_REGION_SPACING, firstLevelButton, secondLevelButton, thirdLevelButton, fourthLevelButton, fifthLevelButton);
    levelButtonRegion.setAlignment(Pos.CENTER);
    levelButtonRegion.setPadding(new Insets(LEVEL_BUTTON_REGION_PADDING));

    VBox centerContent = new VBox(TITLE_SECTION_SPACING, title, worldButtonRegion, levelButtonRegion);
    centerContent.setAlignment(Pos.TOP_CENTER);
    root.setCenter(centerContent);

    Button backButton = new Button("Back");
    backButton.setOnAction(event -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));
    root.setBottom(wrapBottomLeft(backButton));

    return root;
  }

  private void configureWorldButton(Button button) {
    button.setFont(Font.font(ACTION_FONT_SIZE));
    button.setPrefSize(ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
    button.setWrapText(true);
  }

  private void configureLevelButton(Button button) {
    button.setFont(Font.font(ACTION_FONT_SIZE));
    button.setPrefSize(ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
    button.setWrapText(true);
  }

  private StackPane wrapBottomLeft(Button button) {
    StackPane container = new StackPane(button);
    StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
    return container;
  }
}
