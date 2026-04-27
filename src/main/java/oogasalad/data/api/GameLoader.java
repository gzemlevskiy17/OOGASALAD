package oogasalad.data.api;

/**
 * Parses a game.json file into a {@link GameConfig} record. Loaded once per game; the result is
 * then handed to {@link LevelLoader} when a level of that game is opened.
 *
 * @author Paulina Vvedenskaya
 */
public interface GameLoader {

  /**
   * @param fileName path to a game.json file
   * @return the parsed catalog
   * @throws LevelLoadException on missing files, malformed JSON, or schema violations
   */
  GameConfig loadGame(String fileName);
}
