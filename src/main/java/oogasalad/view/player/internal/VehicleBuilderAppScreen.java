package oogasalad.view.player.internal;

import java.util.ResourceBundle;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import oogasalad.controller.core.api.ScreenManager;
import oogasalad.view.core.internal.AbstractAppScreen;

public class VehicleBuilderAppScreen extends AbstractAppScreen {

  private static final ResourceBundle PROPS = ResourceBundle.getBundle("oogasalad.view.player.VehicleBuilderAppScreen");

  public VehicleBuilderAppScreen(ScreenManager screenManager) {
    super(screenManager);
  }

  @Override
  protected BorderPane createRoot() {
    BorderPane root = new BorderPane();
    root.getStylesheets().add(
        getClass().getResource("/oogasalad/view/player/vehicle-builder.css").toExternalForm()
    );

    Text message = new Text("This is VehicleBuilderAppScreen");
    root.getChildren().addAll(
        message
    );

    return root;

  }

}
