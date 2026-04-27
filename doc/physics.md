# Physics Engine Documentation

This document explains the architecture and mathematics behind the custom 2D rigid-body physics engine used in this project. The engine is an impulse-based sequential impulse solver, heavily inspired by Box2D but simplified for this specific game's requirements.

## Core Concepts

The engine operates on **Rigid Bodies** which have mass, moment of inertia, and geometric boundaries (polygons). Physics resolution happens in discrete time steps (`dt`), during which forces are integrated into velocities, constraints (collisions and joints) are solved, and velocities are integrated into positions.

## The Physics Loop (`GameEngineImpl`)

The physics update loop (`GameEngineImpl.update`) is split into several distinct phases to guarantee stability, particularly for complex jointed structures like vehicles. 

### Logic Flow

1. **Force Accumulation**: 
   - `applyForcesToAcceleration()` loops over all active dynamic components and applies external forces (gravity, drive forces from motors/fans, and drag) converting them into linear and angular accelerations.
2. **Velocity Integration**: 
   - `integrateObjectState()` applies the accelerations to update the linear and angular velocities of all dynamic bodies ($v_{new} = v_{old} + a \cdot dt$).
3. **Broad-phase Collision Detection**: 
   - `getCollisions()` iterates over pairs of active bodies. It first uses a fast AABB (Axis-Aligned Bounding Box) overlap check to cull non-colliding pairs.
4. **Narrow-phase Collision Detection**: 
   - If the AABBs overlap, the Separating Axis Theorem (SAT) via `collisionDetector.detectCollision()` is used to find exact penetration depths, normals, and contact manifolds between polygons.
5. **Velocity Constraint Solver (8 iterations)**: 
   - **Joint Velocities**: `joint.applyVelocity()` resolves distance and revolute joint constraints to equalize velocities at anchor points.
   - **Collision Velocities**: `solveCollisions(..., applyImpulse=true, isFirstIteration)` resolves collision impulses (normal bouncing and friction). "Warm starting" is only applied on the very first iteration.
6. **Position Integration**: 
   - `integrateObjectState()` applies the final solved velocities to update the $x, y$ positions and rotations of all rigid bodies ($p_{new} = p_{old} + v \cdot dt$).
7. **Position Constraint Solver (8 iterations)**: 
   - **Joint Positions**: `joint.apply(dt)` directly translates and rotates bodies to fix joint separation/stretching without affecting their momentum.
   - **Collision Positions**: `solveCollisions(..., applyImpulse=false, ...)` uses Baumgarte stabilization (`applyPositionalCorrection`) to directly push overlapping bodies apart.

## Split-Phase Solver Architecture

Historically, the engine coupled velocity resolution and positional correction in a single loop. This caused "constraint fighting," where positional corrections would artificially inject velocity back into the system, leading to massive instability (vehicles spinning out of control or "jittering").

To solve this, the engine now uses a **Split-Phase Solver**:
- **Velocity Phase**: Solves only for $\Delta v$ and $\Delta \omega$. It computes the impulses needed to prevent bodies from moving towards each other (collisions) or drifting apart (joints).
- **Position Phase**: Solves only for $\Delta p$ and $\Delta \theta$. It directly translates and rotates bodies to fix visual overlap or joint separation without affecting their momentum.

By strictly isolating velocity changes from positional changes, the engine ensures that structural constraint enforcement doesn't violate the conservation of energy.

## Collisions

Collisions are resolved using **Sequential Impulses**. When two objects collide, the engine calculates the relative velocity at the contact point.

### Normal Impulse (Bouncing)
If the objects are moving towards each other, an impulse is applied along the collision normal. The magnitude of the impulse $j$ is calculated using the effective mass at the contact point and the coefficient of restitution $e$:

$$j = \frac{-(1 + e) \vec{v}_{rel} \cdot \vec{n}}{m_{eff}}$$

### Friction Impulse (Sliding)
Friction opposes sliding along the tangent of the collision. The friction impulse is calculated similarly to the normal impulse but is clamped by Coulomb's friction law: $j_{friction} \le \mu \cdot j_{normal}$.

### Warm Starting
To improve the convergence of the iterative solver, the engine caches the impulses applied in the previous frame and re-applies them at the very beginning of the *first* velocity iteration. Because the solver needs fewer iterations to find the correct impulse if it starts near the solution, this significantly stabilizes stacking and resting contacts.

### Penetration Normal Inversion
During the sequential impulse loops, `GameEngineImpl` forces a standard parameter order (Reference vs. Incident object) for the solvers. If the original collision detection returned the objects in the reverse order, the solver must multiply the penetration normal by `-1` so the impulses and positional corrections push the objects in the correct directions. 

*Note on Implementation Bug:* If the `Collision` object is mutated directly (i.e. `collision.setPenetration(penetration.multiply(-1))`) inside the 8-step loops, the normal will violently flip back and forth, causing the solver to push the objects *into* the ground half the time. This causes extreme friction drag, preventing vehicles powered by weak forces (like fans) from moving. The penetration normal must be inverted locally or restored after use.

## Joints

Joints constrain the relative motion of two rigid bodies. The engine supports several types of joints, which are heavily used by the `VehicleFactory` to assemble modular cars.

### 1. DistanceJoint
A distance joint forces the distance between two local anchor points to remain within a specific range `[minDistance, maxDistance]`. 
- **Velocity Solver**: It applies a 1D velocity constraint to eliminate relative velocity *along the axis* connecting the two anchors.
- **Position Solver**: It applies a 1D positional correction, pulling or pushing the bodies along the axis to restore the correct distance.
- **Springs**: If `stiffness > 0`, it acts as a damped spring rather than a rigid constraint.

### 2. RevoluteJoint
A revolute joint (hinge) forces two anchor points to remain exactly coincident, allowing the bodies to rotate freely around the pin.
- **Velocity Solver**: Applies a full 2D velocity constraint (`applyAnchorVelocityEqualization`) to eliminate *all* relative translational velocity at the anchor point.
- **Position Solver**: Applies a full 2D positional correction, which uses the effective mass matrix to both translate and *rotate* the bodies back together if they drift apart.

### 3. RigidJoint (Weld & Chords)
`RigidJoint` extends `DistanceJoint` but provides specialized behavior depending on how it's constructed.
- **Weld Mode (`minDistance == 0.0`)**: Used to weld two adjacent blocks together (e.g., the outer shell of the vehicle). It enforces coincident anchors (like a RevoluteJoint) *and* locks the relative rotation. It uses the 2D Revolute position/velocity solver to prevent the blocks from "shearing" diagonally.
- **Chord Mode (`minDistance > 0.0`)**: Used by the `VehicleFactory` as internal diagonal bracing (anti-rotation joints). Because the anchors are at the centers of mass of distant blocks, it falls back to the 1D Distance constraint. This prevents the vehicle from collapsing while avoiding over-constraining the system.

## Vehicle Stability (The "Jelly" Car Problem)

Early versions of the engine suffered from severe vehicle instability. When a car landed on an uneven surface, it would violently spin or act like jelly. This was resolved by fixing three critical flaws:
1. **1D vs 2D Corrections**: Weld joints were incorrectly using 1D positional corrections. When a wheel was pushed up by the ground, the 1D solver translated the connected blocks without rotating them, causing the car to shear out of shape and shift its center of mass. By upgrading `RigidJoint` to use true 2D equalization, the outer chassis acts as a solid rigid body.
2. **Chord Crushing**: Diagonal bracing joints were accidentally initialized with a target distance of `0.0`. They were constantly trying to crush the car into a single point, creating massive internal tension that amplified any ground impulse. Correcting the initial chord distance stabilized the frame.
3. **Solver Coupling**: The collision solver was only running on the first iteration, while the joint solver ran 8 times. This allowed joints to over-correct and multiply tiny collision impulses into massive artificial torques. The split-phase solver fully couples them, resolving both simultaneously.
