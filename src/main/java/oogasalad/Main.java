package oogasalad;

import javafx.application.Application;
import javafx.stage.Stage;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenManagerImpl;
import oogasalad.view.core.api.GameMenuScreen;

/**
 * Launches the Good Piggies application.
 */
public class Main extends Application {

    private static final String MENU_SCREEN_KEY = "MENU";

    /**
     * A method kept for compatibility with existing tests.
     */
    public double getVersion() {
        return 0.001;
    }

    @Override
    public void start(Stage stage) {
        ScreenManager screenManager = new ScreenManagerImpl(stage);
        screenManager.registerScreen(MENU_SCREEN_KEY, new GameMenuScreen());
        screenManager.switchScreen(MENU_SCREEN_KEY, null);

        stage.setTitle("Good Piggies");
        stage.show();
    }

    /**
     * Start of the program.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
