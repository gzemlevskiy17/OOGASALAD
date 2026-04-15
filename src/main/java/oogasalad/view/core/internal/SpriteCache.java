package oogasalad.view.core.internal;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpriteCache {

  private static final Logger logger = LogManager.getLogger(SpriteCache.class);

  private final Map<String, Image> cache = new HashMap<>();

  public Image get(String path) {
    return cache.computeIfAbsent(path, this::load);
  }

  private Image load(String path) {
    try {
      File file = new File(path);
      if (file.exists()) {
        return new Image(file.toURI().toString());
      }
      logger.warn("Sprite not found: {}", path);
    } catch (Exception e) {
      logger.warn("Could not load sprite: {}", path);
    }
    return null;
  }
}
