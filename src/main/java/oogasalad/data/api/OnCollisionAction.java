package oogasalad.data.api;

/**
 * Engine behavior triggered when a dynamic body contacts a static component. {@code amount} is
 * required for actions that take a magnitude (e.g. heal, damage) and ignored for those that don't
 * (e.g. finish).
 *
 * @param type   registry key, e.g. "heal", "damage", "finish"
 * @param amount magnitude for actions that take one; null otherwise
 * @author Paulina Vvedenskaya
 */
public record OnCollisionAction(String type, Double amount) {

}
