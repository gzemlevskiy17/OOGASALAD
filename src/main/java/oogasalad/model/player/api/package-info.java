/**
 * Manages gameplay state evaluation and vehicle construction constraints.
 *
 * OVERVIEW
 *
 * This package defines the rules and logic for player progression and vehicle building. It exposes the following interfaces to external actors
 * (such as the GameController or the Authoring environment):
 *
 * 1. WinConditionChecker: Evaluates victory states.
 * - hasWon(World): Returns true if the current world state meets victory criteria.
 *
 * 2. LossConditionChecker: Evaluates defeat states.
 * - hasLost(World): Returns true if the current world state meets failure criteria.
 *
 * 3. ScoreCalculator: Quantifies player performance.
 * - calculateScore(World, Settings): Computes a numerical score based on
 * time, distance, or collected items.
 *
 * 4. VehicleWorkspace: Defines assembly constraints.
 * - isValidPart(GameObject): Checks if a specific component is allowed in the sandbox.
 * - isAssemblyValid(Collection<GameObject>): Validates the entire vehicle structure.
 *
 * DETAILS
 *
 * The win and loss checkers act as observers of the World state, usually queried
 * once per simulation tick. The VehicleWorkspace is primarily used during the
 * authoring/assembly phase to ensure players adhere to the "rules of the sandbox"
 * (e.g., part limits or weight restrictions) before the simulation starts.
 *
 * CONSIDERATIONS
 *
 * We separate Win and Loss checkers into distinct interfaces to allow for
 * modular game rules. A level might have multiple ways to lose (falling off-screen,
 * running out of fuel) but only one way to win, making this separation
 * highly extensible for different game types.
 */
package oogasalad.model.player.api;