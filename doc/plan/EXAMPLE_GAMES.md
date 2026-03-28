# Example Games

## Team Name: Good Piggies

### Why These Two Games

We chose **Bad Piggies** and **Geometry Dash** because they sit at opposite ends of our engine's flexibility spectrum. Bad Piggies is a construction-first, player-paced physics sandbox. Geometry Dash is a reflexes-first, auto-scrolling precision platformer. Despite being very different experiences, they share a common foundation: a 2D physics world with terrain, obstacles, a player-controlled subject, and a goal. The fact that our engine must support both forces us to build clean abstractions around the things that vary (vehicle construction, camera behavior, input handling, win/loss conditions) while keeping the things that don't vary (physics simulation, collision detection, level data format, rendering pipeline) in a shared core.

---

## Game 1: Bad Piggies

### Description

Bad Piggies is a physics-based vehicle construction puzzle game. Before each level begins, the player enters a **build phase** where they assemble a vehicle from a library of components (wooden frames, wheels, engines, balloons, fans, boosters) on a grid. Components snap to attachment points and form joints (rigid connections for frames, revolute joints for wheels). Once the player is satisfied with their contraption, they press "Go" and the game switches to a **play phase** where the vehicle is placed at the level's start position and physics simulation begins. The player can activate certain components during gameplay (toggling the engine, firing a booster) using keyboard input. The goal is to transport the pig safely to the finish line.

### Key Mechanics

- **Vehicle construction**: The player drags components from a palette onto a grid. The system validates attachments (wheels must connect to frames, the vehicle must contain a pig). Invalid placements are rejected with feedback. This is the core creative element — there are many possible vehicle designs for any given level.
- **Multi-component physics**: The vehicle is not a single rigid body. It is a collection of components connected by joints. Wheels rotate independently, balloons apply upward force, engines apply directional thrust. The physics engine must simulate forces on each component and propagate them through the joint graph.
- **Player-paced gameplay**: There is no auto-scroll. The player controls the vehicle's speed via engine activation. The camera follows the vehicle. The player can take as long as they want.
- **Health and survival**: The pig has a health value. Hard collisions reduce health. If health reaches zero, the level is failed. This creates a tradeoff between speed (more momentum = more damage on impact) and caution.
- **Scoring**: Score is based on completion, time bonus, and coins collected. Three-star thresholds encourage replay.
- **Win condition**: Reach the finish line with the pig alive.
- **Loss conditions**: Pig health reaches zero, or the vehicle falls out of bounds.

### Level Structure

Levels are finite, bounded areas with hand-placed terrain (ground, ramps, walls), obstacles (pits, exploding blocks), collectibles (coins), and a single finish-line goal. Levels are designed with specific vehicle solutions in mind but allow creative alternatives. Progression is linear: complete level 1 to unlock level 2.

---

## Game 2: Geometry Dash

### Description

Geometry Dash is an auto-scrolling rhythm platformer. The player controls a cube (or other shape) that moves rightward at a constant speed. The only input is **tap to jump**. The world scrolls automatically and the player must time their jumps to avoid obstacles. Any contact with an obstacle (spikes, walls, sawblades) kills the player instantly and restarts the level. There is no vehicle construction — the player's avatar is fixed.

### Key Mechanics

- **No construction phase**: The game starts immediately in the play phase. The player's subject is a predefined cube with fixed properties (mass, jump force). There is no build grid, no component palette, no attachment system. The `build_mode.enabled` flag in the game config is `false`.
- **Single-body physics**: The player avatar is one rigid body. There are no joints, no multi-component vehicles. The physics engine still handles gravity, collision, and velocity, but the joint/attachment system is not used.
- **Auto-scroll camera**: The camera moves rightward at a constant speed regardless of player input. The player's horizontal position is effectively locked — they can only control vertical movement (jumping). This is fundamentally different from Bad Piggies where the camera follows a player-controlled vehicle.
- **Instant death**: Any collision with a lethal obstacle immediately triggers level failure. There is no health system, no damage thresholds. The `instant_death_on_obstacle` gameplay modifier controls this behavior.
- **Binary input**: The only meaningful input is jump (spacebar/tap). Compare this to Bad Piggies where the player can activate multiple components with different keys. The input mapping in the game config has one action instead of four.
- **Scoring**: Score is based on progress percentage (how far through the level the player got before dying) and coins collected. There is no time bonus since the player cannot control speed.
- **Win condition**: Reach the end of the level (the portal/finish line).
- **Loss conditions**: Touch any lethal obstacle, or fall out of bounds.

### Level Structure

Levels are long, narrow horizontal courses. Terrain consists of flat platforms with gaps between them. Obstacles are precisely placed to create timing challenges — spike patterns, moving sawblades, walls to jump over. The level is designed around the constant scroll speed so obstacles arrive at predictable intervals (often synced to music in the original game). There are no branching paths or open exploration — it is a single linear gauntlet.

---

## Functional Commonalities

These are the things both games share, which become the **closed, stable core** of our engine:

| Feature | Shared Implementation |
|---|---|
| 2D physics simulation | Both use gravity, collision detection, velocity/position updates. The `PhysicsEngine` and `CollisionDetector` classes are identical. |
| Level data format | Both use the same JSON schema: terrain array, obstacles array, collectibles array, goals array, subjects array, start position, dimensions. The `LevelLoader` parses both without special-casing. |
| Terrain and obstacles | Both have ground blocks, obstacles with `lethal` properties, and collectible coins. The `TerrainComponent`, `PowerupComponent` (coins), and `FinishLineComponent` CRC cards apply to both. |
| Rendering pipeline | Both use the `Renderer` → `Renderable` → `GameComponent` chain. Terrain tiles textures, sprites render as single images. The `tile_texture` and `render_mode` fields in the data files drive this. |
| Game loop | Both use the same `GameController` → `GameEngine.step()` → `PhysicsEngine` → `CollisionDetector` → `GameRenderer` loop. |
| Win/loss state machine | Both check conditions each frame and transition to a result screen. The `GameController` calls a generic win/loss checker. |

## Functional Differences

These are the things that vary, which become the **open, configurable** parts of our engine:

| Feature | Bad Piggies | Geometry Dash |
|---|---|---|
| Build mode | Enabled. Full grid-based vehicle construction with component palette, attachment validation, and joint creation. Uses `VehicleBuildRulesManager`, `CarBuilderController`. | Disabled. Player gets a fixed single-body avatar. Build UI never appears. |
| Subject complexity | Multi-component vehicle with joints. `Vehicle` holds a list of `VehicleComponent` objects connected by attachments. | Single rigid body. One `GameComponent` with no attachments. |
| Camera behavior | `follow_player` — camera tracks the vehicle's position with optional lead-ahead. Player controls horizontal speed. | `auto_scroll` — camera moves at constant speed. Player only controls vertical position. |
| Input model | Multiple actions: activate engine, fire booster, toggle fan. Each mapped to a `DriveComponent` on the vehicle. | One action: jump. Mapped directly to applying an upward force on the subject. |
| Damage model | Health-based. Collision impulse reduces pig health. Pig can survive minor bumps. | Instant death. Any lethal collision ends the run. Health is effectively 1. |
| Scoring model | Completion bonus + time bonus + coins. Rewards efficiency. | Progress percentage + coins. Rewards consistency and memorization. |
| Level design philosophy | Open, sandbox-like. Multiple solutions per level. Short levels. | Linear gauntlet. One intended path. Long levels. |

## How This Clarifies Our Abstractions

The comparison above maps directly to our design:

- **`game_config.json`** is the abstraction that switches between these two games. It controls `build_mode`, `camera.mode`, `input_mapping`, `win_condition.type`, `loss_conditions`, `scoring`, and `gameplay_modifiers`. The engine reads this config and configures itself accordingly — no code changes needed to switch game types.
- **`GameComponent` hierarchy** must support both single-body subjects (Geometry Dash cube) and multi-body vehicles (Bad Piggies cart). This is why `Vehicle extends GameObject` holds a list of `VehicleComponent` objects — a Geometry Dash cube is just a Vehicle with one component and no attachments.
- **`CollisionStrategy` pattern** handles the difference between health-based damage and instant death. The `VehicleComponentStrategy`, `TerrainComponentStrategy`, etc. on our CRC cards resolve collisions differently depending on the game config's `instant_death_on_obstacle` flag.
- **`AuthoringEnvironment`** must support creating levels for both game types. The terrain/obstacle/goal placement tools are the same, but the authoring environment should show or hide the vehicle builder based on the game type being authored. This validates our decision to separate `AuthoringController` (level editing) from `CarBuilderController` (vehicle editing).