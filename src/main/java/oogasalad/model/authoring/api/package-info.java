/**
 * Defines the rules for level design.
 *
 * OVERVIEW
 *
 * This package provides the interface for managing the boundaries of a level.
 * It allows a level designer to define what is "legal" for a player to build
 * or do within a specific simulation. It exposes the following interface
 * for external actors:
 *
 * 1. VehicleBuildRulesManager: The rule setter for the designer.
 * - setMaxParts(int): Restricts the total number of components a player can use.
 * - getMaxParts(): Retrieves the currently enforced part limit.
 * - validateBuild(World): Checks a simulation state against the active restrictions.
 *
 * DETAILS
 *
 * The VehicleBuildRulesManager acts as the configuration bridge between the
 * Authoring UI and the Model. While the 'VehicleWorkspace' (in model.player.api)
 * enforces rules during assembly, this Manager is what a level creator uses
 * to *define* those rules in the first place. It collaborates with the 'World'
 * to ensure that environmental constraints are mathematically sound.
 *
 * CONSIDERATIONS
 *
 * By separating the "Management" of rules from the "Execution" of the game,
 * we ensure that the Editor can provide real-time feedback to the designer
 * (e.g., "This limit is too low for the required components") without
 * interfering with the player's runtime experience.
 */
package oogasalad.model.authoring.api;