package oogasalad;

import javafx.application.Application;
import javafx.stage.Stage;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.controller.core.internal.ScreenManagerImpl;

/**
 * Launches the Good Piggies application.
 */
public class Main extends Application {

    private static final double VERSION = 0.001;

    /**
     * Start of the program.
     */
    static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ScreenManager screenManager = new ScreenManagerImpl(stage);
        ScreenFactory screenFactory = new ScreenFactory(screenManager);
        screenFactory.registerScreens();
        screenManager.switchScreen(ScreenFactory.LOGIN_SCREEN_KEY);
        stage.setTitle("Good Piggies");
        stage.show();
        stage.setMaximized(true);
    }

    /**
     * Returns the current application version used by the starter sanity test.
     */
    public double getVersion() {
        return VERSION;
    }
}
