package oogasalad.data.api;

/**
 * Engine behavior triggered when the main character collides with a static object.
 *
 * @param type   registry key, e.g. "heal", "damage", "finish"
 * @param amount magnitude for actions that take one; ignored for zero-argument actions like
 *               "finish"
 */
public record CollisionAction(String type, double amount) {

}
