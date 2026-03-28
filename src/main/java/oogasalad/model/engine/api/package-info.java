/**
 * Defines the physical and structural engine of the simulation
 *
 * OVERVIEW
 *
 * This package provides the contracts for the game's mathematical model, entity
 * relationships, and the physics engine's execution. It exposes the following
 * interfaces and records to external actors (e.g., the Controller and View):
 *
 * 1. GameEngine: The physics logic.
 * - step(): Advances the entire simulation world by one temporal unit.
 *
 * 2. World: The global entity container.
 * - getObjects(): Provides access to all active GameObjects.
 * - getJoints(): Provides access to all physical connections between objects.
 *
 * 3. GameObject: The base physical entity (extends Renderable).
 * - getMass(): Returns the object's mass for physics calculations.
 * - getVelocity(): Returns the current movement vector.
 * - getHitboxes(): Returns the collection of areas used for collision detection.
 *
 * 4. Vector: An immutable mathematical utility.
 * - x(), y(): Direct access to coordinate components.
 *
 * 5. CollisionDetector / CollisionStrategy: The physics resolution pair.
 * - detectCollisions(World): Identifies overlapping hitboxes within the simulation.
 * - resolve(World): Steps the simulation forward by resolving physical conflicts.
 *
 * DETAILS
 *
 * The Model follows a hierarchical structure where a World holds GameObjects
 * and Joints. The GameEngine uses a CollisionDetector to find overlaps and
 * delegates the physical response to a CollisionStrategy.
 *
 * CONSIDERATIONS
 *
 * All spatial data is handled via the Vector record to ensure immutability,
 * preventing "side-effect" bugs where a view might accidentally modify an
 * object's position during a render pass.
 */
package oogasalad.model.engine.api;