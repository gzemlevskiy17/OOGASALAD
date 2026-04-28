package oogasalad.model.player.api;

/**
 * Mutable running tally of points accumulated during one gameplay session. Distinct from
 * {@link ScoreCalculator}, which derives a final score from world state — this service tracks
 * incremental awards as they happen (coin pickups, XP boosts, etc.).
 *
 * @author Paulina Vvedenskaya
 */
public interface ScoreService {

  int getPoints();

  void addPoints(int amount);
}
