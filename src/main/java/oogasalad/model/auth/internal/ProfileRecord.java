package oogasalad.model.auth.internal;

import java.util.List;
import oogasalad.model.auth.api.PlayerProfile;

/**
 * A snapshot of a logged-in player's public profile.
 * <p>
 * Contains only display information — no passwords, no salts. A new instance is created whenever
 * the profile changes (e.g. avatar update).
 *
 * @author Paulina Vvedenskaya
 */
record ProfileRecord(String username, String avatarPath) implements PlayerProfile {

  /**
   * Friend system is not yet implemented — always returns an empty list.
   */
  @Override
  public List<String> friendUsernames() {
    return List.of();
  }
}
