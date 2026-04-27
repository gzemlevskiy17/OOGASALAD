package oogasalad.data.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import oogasalad.data.api.GameConfig;
import oogasalad.data.api.GameLoader;

/**
 * Reads a game.json file and produces a {@link GameConfig}. Each list in the catalog is parsed
 * top-down; missing optional fields default to sensible empty/null values rather than throwing.
 *
 * @author Paulina Vvedenskaya
 */
public class GameLoaderImpl implements GameLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * @deprecated Use {@link GameConfigImpl} directly.
   */
  @Deprecated
  public static GameConfig parseFromFile(String fileName) {
    return new GameConfigImpl(fileName);
  }

  @Override
  public GameConfig loadGame(String fileName) {
    return new GameConfigImpl(fileName);
  }
}
