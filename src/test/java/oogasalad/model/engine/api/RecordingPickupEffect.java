package oogasalad.model.engine.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only {@link PickupEffect} that records every {@code apply} invocation so tests can verify
 * how many times an effect fired and against which target. This is intentionally a fake (records
 * calls) rather than a stub or noop.
 *
 * @author Paulina Vvedenskaya
 */
public final class RecordingPickupEffect implements PickupEffect {

  private final List<GameObject> targets = new ArrayList<>();

  @Override
  public void apply(GameObject target) {
    targets.add(target);
  }

  public int callCount() {
    return targets.size();
  }

  public GameObject lastTarget() {
    if (targets.isEmpty()) {
      throw new IllegalStateException("apply has not been called");
    }
    return targets.get(targets.size() - 1);
  }
}
