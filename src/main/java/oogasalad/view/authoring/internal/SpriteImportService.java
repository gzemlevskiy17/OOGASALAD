package oogasalad.view.authoring.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Imports selected sprite files into the shared authoring asset directory.
 *
 * @author Paulina Vvedenskaya
 * @author Emilio Garcia
 * @author Iurii Beliaev
 */
final class SpriteImportService {

  private static final Logger LOG = LogManager.getLogger(SpriteImportService.class);

  private final Path assetsDir;
  private final AuthoringAlertService alertService;

  SpriteImportService(Path assetsDir, AuthoringAlertService alertService) {
    this.assetsDir = Objects.requireNonNull(assetsDir);
    this.alertService = Objects.requireNonNull(alertService);
  }

  void browseSprite(Node owner, TextField target) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Choose Sprite Image");
    chooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
    File selected = chooser.showOpenDialog(owner.getScene().getWindow());
    if (selected == null) {
      return;
    }
    try {
      Files.createDirectories(assetsDir);
      Path dest = assetsDir.resolve(selected.getName());
      Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
      target.setText(normalizeSpritePath(dest));
      LOG.info("Copied sprite to {}", dest);
    } catch (IOException e) {
      LOG.warn("Could not import sprite {}", selected, e);
      alertService.showError("Sprite Import Error", e.getMessage());
    }
  }

  static String normalizeSpritePath(Path path) {
    return path.toString().replace(File.separatorChar, '/');
  }
}
