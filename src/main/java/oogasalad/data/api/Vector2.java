package oogasalad.data.api;

/**
 * Plain 2D coordinate or direction in JSON-loaded data. Lives in the data layer (not the engine) so
 * the data API doesn't depend on engine types.
 *
 * @author Paulina Vvedenskaya
 */
public record Vector2(double x, double y) {

}
