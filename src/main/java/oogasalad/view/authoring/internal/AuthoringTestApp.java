package oogasalad.view.authoring.internal;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AuthoringTestApp extends Application {

  @Override
  public void start(Stage stage) {
    AuthoringCanvasImpl canvas = new AuthoringCanvasImpl();
    StackPane root = new StackPane(canvas.getVisualRoot());
    Scene scene = new Scene(root);
    stage.setTitle("Authoring Canvas Test");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
