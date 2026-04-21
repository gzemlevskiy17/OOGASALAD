package oogasalad.model.player.api;

/**
 * Represents one component placed in the vehicle builder grid Holds grid positon and name of the
 * component (ex. "fan", "small wheel")
 *
 * @param row
 * @param col
 * @param componentType
 * @author Paulina Vvedenskaya
 */
public record PlacedComponent(int row, int col, String componentType) {

}
