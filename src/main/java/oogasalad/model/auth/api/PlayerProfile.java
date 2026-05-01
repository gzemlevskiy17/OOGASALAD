package oogasalad.model.auth.api;

import java.util.List;

/**
 * The public-facing profile of a logged-in player.
 * <p>
 * Contains only display information — no passwords, no internal IDs. Safe to pass to the view
 * layer.
 *
 * @author Paulina Vvedenskaya
 */
public interface PlayerProfile {

  /**
   * The player's unique display name.
   */
  String username();

  /**
   * File path to the player's avatar image, or an empty string if none is set.
   */
  String avatarPath();

  /**
   * Usernames of players this player has added as friends.
   */
  List<String> friendUsernames();
}
