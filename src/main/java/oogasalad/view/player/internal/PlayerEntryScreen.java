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
  private static final double BUTTON_REGION_SPACING = 24;
  private static final double BUTTON_REGION_PADDING = 32;
  private static final double ACTION_BUTTON_WIDTH = 300;
  private static final double ACTION_BUTTON_HEIGHT = 180;

  public PlayerEntryScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(ROOT_PADDING));

    Label title = new Label("Build Your Piggies");
    title.setFont(Font.font(TITLE_FONT_SIZE));

    Button createNewLevelButton = new Button("Create a New Level");
    configureActionButton(createNewLevelButton);

    Button loadExistingLevelButton = new Button("Load and Edit on Existing Level");
    configureActionButton(loadExistingLevelButton);

    HBox buttonRegion = new HBox(BUTTON_REGION_SPACING, createNewLevelButton, loadExistingLevelButton);
    buttonRegion.setAlignment(Pos.CENTER);
    buttonRegion.setPadding(new Insets(BUTTON_REGION_PADDING));

    VBox centerContent = new VBox(TITLE_SECTION_SPACING, title, buttonRegion);
    centerContent.setAlignment(Pos.TOP_CENTER);
    root.setCenter(centerContent);

    Button backButton = new Button("Back");
    backButton.setOnAction(event -> screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null));
    root.setBottom(wrapBottomLeft(backButton));

    return root;
  }

  private void configureActionButton(Button button) {
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
