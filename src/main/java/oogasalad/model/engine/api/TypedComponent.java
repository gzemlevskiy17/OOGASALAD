package oogasalad.model.engine.api;

/**
 * A component that exposes a palette/display type name. Used by the view layer to select sprites
 * and by game logic that dispatches on type without relying on runtime class identity.
 *
 * @author Paulina Vvedenskaya
 */
public interface TypedComponent {

  String getComponentType();
}
