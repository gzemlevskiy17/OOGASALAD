package oogasalad.view.authoring.internal.level;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import oogasalad.view.core.internal.SpriteCache;
import org.junit.jupiter.api.Test;
import util.DukeApplicationTest;

/**
 * Tests stable metadata applied to vehicle component palette cells.
 *
 * @author Emilio Garcia
 */
class VehicleComponentCellFactoryTest extends DukeApplicationTest {

  private VehicleComponentCellFactory factory;

  @Override
  public void start(Stage stage) {
    factory = new VehicleComponentCellFactory(VehiclePaletteConfig.loadDefault(),
        new SpriteCache());
    stage.setScene(new Scene(new HBox(), 400, 200));
    stage.show();
  }

  @Test
  void createCell_componentWithSpaces_getsStableNodeId() {
    StackPane cell = createCell("Wood Wheel");

    assertTrue(cell.getStyleClass().contains("vehicle-component-cell"));
    assertTrue("vehicle-component-cell-WoodWheel".equals(cell.getId()));
  }

  @Test
  void setSelected_selectedCellGetsSelectedStyleClass() {
    StackPane cell = createCell("Frame");

    runAsJFXAction(() -> factory.setSelected(cell, true));

    assertTrue(cell.getStyleClass().contains("vehicle-component-cell-selected"));
  }

  private StackPane createCell(String componentId) {
    AtomicReference<StackPane> cell = new AtomicReference<>();
    runAsJFXAction(() -> cell.set(factory.createCell(
        new SelectedVehicleComponent(componentId, componentId), component -> {
        })));
    return cell.get();
  }
}
