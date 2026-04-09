package oogasalad.model.auth.internal;

import oogasalad.model.auth.api.PlayerProfile;

/**
 * A raw database record for a user account.
 *
 * Includes sensitive fields like {@code passwordHash} and {@code salt} —
 * never pass this to the view layer. Convert to a {@link PlayerProfile} instead.
 */
public record UserRecord(String id, String username, String passwordHash, String salt, String avatarPath) {}
