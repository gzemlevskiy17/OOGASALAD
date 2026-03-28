/**
 * Makes sure that the Simulation is working properly tick-by-tick
 *
 * OVERVIEW
 *
 * Manages the "Play" mode lifecycle. It follows the Single Responsibility Principle
 * by handling timing, leaving the actual physics math to the model. It's built
 * to be swappable—you could swap a local timer for a networked one without
 * the UI ever knowing.
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
 * We split Pause vs. Stop to allow for "hibernation" (resuming exactly where
 * you left off) vs. "destruction" (wiping the current run). It adds a bit
 * of state complexity but keeps the resource management much cleaner.
 */
package oogasalad.controller.player.api;