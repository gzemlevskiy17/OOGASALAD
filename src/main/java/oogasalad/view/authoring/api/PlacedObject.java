package oogasalad.view.authoring.api;

/**
 * Immutable metadata for an occupied authoring-grid cell and its drawing bounds.
 *
 * @param col grid column of the occupied cell
 * @param row grid row of the occupied cell
 * @param drawX top-left x-coordinate on the canvas in pixels
 * @param drawY top-left y-coordinate on the canvas in pixels
 * @param width drawn width on the canvas in pixels
 * @param height drawn height on the canvas in pixels
 */
public record PlacedObject(int col, int row, double drawX, double drawY, double width,
                           double height) {
}
