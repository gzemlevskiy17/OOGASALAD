package oogasalad;

import java.util.Map;
import javafx.application.Application;
import javafx.stage.Stage;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.controller.core.internal.ScreenManagerImpl;
import oogasalad.view.core.api.AppScreen;

/**
 * Launches the Good Piggies application.
 */
public class Main extends Application {

    /**
     * A method kept for compatibility with existing tests.
     */
    public double getVersion() {
        return 0.001;
    }

    @Override
    public void start(Stage stage) {
        ScreenManager screenManager = new ScreenManagerImpl(stage);
        ScreenFactory screenFactory = new ScreenFactory(screenManager);
        Map<String, AppScreen> screens = screenFactory.createScreens();

        for (Map.Entry<String, AppScreen> screenEntry : screens.entrySet()) {
            screenManager.registerScreen(screenEntry.getKey(), screenEntry.getValue());
        }

        screenManager.switchScreen(ScreenFactory.MENU_SCREEN_KEY, null);

        stage.setTitle("Good Piggies");
        stage.show();
        stage.setMaximized(true);

    }

    /**
     * Start of the program.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
