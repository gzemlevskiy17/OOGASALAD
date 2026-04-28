package oogasalad;

import javafx.application.Application;
import javafx.stage.Stage;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.controller.core.internal.ScreenFactory;
import oogasalad.controller.core.internal.ScreenManagerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Launches the Good Piggies application.
 */
public class Main extends Application {

    private static final Logger LOG = LogManager.getLogger(Main.class);
    private static final double VERSION = 0.001;
    private static final Thread.UncaughtExceptionHandler UNCAUGHT_HANDLER =
        (t, e) -> LOG.error("Uncaught exception on thread '{}'", t.getName(), e);

    /**
     * Start of the program.
     */
    static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(UNCAUGHT_HANDLER);
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Thread.currentThread().setUncaughtExceptionHandler(UNCAUGHT_HANDLER);
        LOG.info("Good Piggies starting (version={})", VERSION);
        ScreenManager screenManager = new ScreenManagerImpl(stage);
        ScreenFactory screenFactory = new ScreenFactory(screenManager);
        screenFactory.registerScreens();
        screenManager.switchScreen(ScreenFactory.LOGIN_SCREEN_KEY);
        stage.setTitle("Good Piggies");
        stage.show();
        stage.setMaximized(true);
    }

    @Override
    public void stop() {
        LOG.info("Good Piggies stopping");
    }

    /**
     * Returns the current application version used by the starter sanity test.
     */
    public double getVersion() {
        return VERSION;
    }
}
