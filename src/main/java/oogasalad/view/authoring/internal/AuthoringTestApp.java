package oogasalad.view.authoring.internal;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AuthoringTestApp extends Application {

  @Override
  public void start(Stage stage) {
    AuthoringScreenImpl screen = new AuthoringScreenImpl();
    stage.setTitle("Authoring Screen");
    stage.setScene(new Scene(screen.getRoot()));
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
