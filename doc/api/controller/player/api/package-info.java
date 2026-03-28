/**
 * Makes sure that the the controller for a Player is working properly
 *
 * OVERVIEW
 *
 * This package manages the "Play" mode lifecycle.
 * It provides a decoupled architecture through three primary interfaces accessible to
 * external actors:
 *
 * 1. GameController: The simulation "heartbeat."
 * - startSimulation(): Initiates the 60-FPS update loop.
 * - pauseSimulation(): Suspends physics and rendering ticks while maintaining state.
 * - stopSimulation(): Terminates the simulation session.
 *
 * 2. InputRouter: The translation layer for hardware events.
 * - bind(KeyCode, Action): Maps a specific hardware key to a gameplay command.
 *
 * 3. Action: The command pattern implementation for gameplay logic.
 * - execute(): Triggers the specific logic encapsulated by the command.
 *
 * It follows the Single Responsibility Principle by handling timing and input routing,
 * leaving the actual physics math to the model.
 *
 * DETAILS
 *
 * The GameController orchestrates the frame sequence:
 * 1. Step the physics engine.
 * 2. Move the camera.
 * 3. Paint the frame.
 *
 * CONSIDERATIONS
 *
 * We split Pause vs. Stop to allow for resuming exactly where
 * you left off vs. wiping the current run. It adds a bit
 * of state complexity but keeps the resource management much cleaner.
 */
package oogasalad.controller.player.api;