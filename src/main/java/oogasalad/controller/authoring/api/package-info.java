/**
 Controls the view->actual change in grid for Authoring Environment
 *
 * OVERVIEW
 * This API turns raw mouse events into logical grid placements. It's the
 * "traffic cop" for the editor, coordinating between the UI, the selected
 * tools, and the data saving/loading services.
 *
 * DETAILS
 * The AuthoringController manages the workflow by talking to the Canvas (View),
 * the Palette (View), and the LevelLoader/Saver (Data). It uses the ToolSelector
 * to decide if it should be drawing or erasing based on the user's current pick.
 *
 * CONSIDERATIONS
 * By importing LevelLoader and LevelSaver from the data package, we keep
 * the authoring logic decoupled from specific file formats (like JSON).
 * This makes it easy to change how levels are stored without breaking the editor.
 */
package oogasalad.controller.authoring.api;