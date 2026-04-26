package oogasalad.data.api;

/**
 * On-screen button contributed by a movable component. Pressing the button while in play applies
 * {@link #direction} as a movement vector to the component each frame.
 *
 * @param name      author-supplied label (uniqueness is the author's responsibility)
 * @param direction movement vector applied while the button is held
 * @author Paulina Vvedenskaya
 */
public record Button(String name, Vector2 direction) {

}
