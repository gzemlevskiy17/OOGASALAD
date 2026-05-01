package oogasalad.model.player.api;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Declarative placement rules for one component type. Consumed by {@link PlacementValidator} during
 * drag/drop. Sides are expressed in the component's un-rotated frame.
 *
 * <p>{@code connectionSurfaces} is the offer-set (sides that accept an incoming attachment);
 * {@code requiredAttachments} is the demand-set (sides that must actually be attached). An
 * attachment is valid when both components advertise the shared edge in their offer-sets, so no
 * component-type branching is needed — Frame is just a component with all four sides attachable and
 * no required attachments.
 *
 * @author Paulina Vvedenskaya
 */
public record BuildRules(
    Set<Side> connectionSurfaces,
    Set<Side> requiredAttachments,
    Set<Side> requiredOpenSides,
    Containment containment,
    PositionConstraint position) {

  public BuildRules {
    connectionSurfaces = connectionSurfaces == null ? Set.of() : Set.copyOf(connectionSurfaces);
    requiredAttachments = requiredAttachments == null ? Set.of() : Set.copyOf(requiredAttachments);
    requiredOpenSides = requiredOpenSides == null ? Set.of() : Set.copyOf(requiredOpenSides);
    containment = containment == null ? Containment.NONE : containment;
    position = position == null ? PositionConstraint.NONE : position;
    if (!connectionSurfaces.containsAll(requiredAttachments)) {
      throw new IllegalArgumentException(
          "requiredAttachments must be a subset of connectionSurfaces");
    }
    if (!java.util.Collections.disjoint(requiredOpenSides, connectionSurfaces)) {
      throw new IllegalArgumentException(
          "requiredOpenSides and connectionSurfaces must be disjoint");
    }
  }

  private static Set<Side> rotate(Set<Side> sides, int quarterTurns) {
    if (sides.isEmpty() || quarterTurns % 4 == 0) {
      return sides;
    }
    return sides.stream()
        .map(s -> s.rotate(quarterTurns))
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Side.class)));
  }

  public Set<Side> rotatedConnectionSurfaces(int quarterTurns) {
    return rotate(connectionSurfaces, quarterTurns);
  }

  public Set<Side> rotatedRequiredAttachments(int quarterTurns) {
    return rotate(requiredAttachments, quarterTurns);
  }

  public Set<Side> rotatedRequiredOpenSides(int quarterTurns) {
    return rotate(requiredOpenSides, quarterTurns);
  }
}
