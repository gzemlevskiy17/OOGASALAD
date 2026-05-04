# DESIGN Document for Oogasalad
### Good Piggies
### Georgiy Zemlevskiy, Iurii Beliaev, Paulina Vvedenskaya, Michael Li, Emilio Garcia, Mason Wu, Xuye Chen


## Team Roles and Responsibilities

 * Georgiy Zemlevskiy: My responsibility was most of the physics engine: mostly collision handling and the component abstractions, although I worked on joints (which attach components to each other in different ways) as well. Smaller features include drag from the surrounding medium (air), and drive components.

* Iurii Beliaev: My responsibilities throughout the Sprints included the following:
  * Discussion and Agile Lead - I led the group meetings, encouraged participation and following Agile process, I encouraged engagement in the meetings and tried to make sure everybody knows what they are doing and what everybody doing. I also led the standup meetings
  * Designer - I was extremely engaged in the architecture decision making and was the one who created the final version of the initial design based on our discussions. Then we discussed it in a team and the team agreed. I was responsible for the design of the initial APIs
  * VIEW API Lead - I created the foundation for the entire View API and rigid structure (`Renderer`, `Renderable`, `Camera`, `CameraBehavior`, `AppScreen`, `AbstractAppScreen` logic. 
  * Engine debugger - during the sprints 3 and 4 with the LLM Teammate I had to debug the engine and make it behave realistically
  * Worked on engine - implemented some of the engine feature with the LLM Teammate to finish its integration and implementation
  * Engine and Joints Integration - during the sprints 3 and 4 I fixed the integration between the Engine and Joints
  * Merge specialist - I spent a very long time merging the work of other people together
  * Debugger - during the last 2 sprints, I had to fix a lot of bugs to make our product demoable
  * Worked on Controller and pulling everything together
  * Overall generalist, implemented a good amount of features


 * Paulina Vvedenskaya: I worked across gameplay control, screen flow, authentication/accounts, vehicle building, data schema evolution, and late-stage refactoring. Early in the project, I implemented the screen manager, game controller loop, input routing, pause/restart actions, camera following, and navigation outcomes. I also built the login, account creation, profile, update profile, local authentication service, SQLite user repository, password hashing, and auth controller integration.

    Later, I worked on vehicle builder behavior, sprites, vehicle part registry, placement validation, build rules, main character support, goal line behavior, HP/damage systems, pickup components, collision effects, button binding, camera target resolution, and gameplay session wiring. Near the end, I refactored several large systems, including breaking apart `GameEngineImpl` into focused physics collaborators, extracting gameplay world/renderable/session helpers, improving authoring viewport behavior, fixing background/track color support, and cleaning up level/game data.


 * Michael Li: I focused on the authoring environment, data layer, terrain systems, and gameplay wiring throughout the project. For sprint 1, I built the foundation for the level editor, implementing the level loader and saver (JSON parsing with Jackson), authoring controller, game config, and authoring canvas. I later expanded it with attribute and terrain property panels, object selection, draw/erase tools, and undo/redo functionality.
A major area of ownership was the spline terrain system, including parabola and terrain spline classes, interactive knot authoring, and spline rendering. I also handled the spawn point system, vehicle-terrain interaction, multi-polygon collision detection, and a responsive camera with zoom support. Because the splines themselves changed how the ground was rendered, I additionally rewrote the level validator and level saver as part of an overhaul of the game catalog editing flow.
In the final sprint, I redesigned the finish line as a draggable stripe, added an HP display to the HUD, fixed terrain block persistence and finish line win detection, resolved vehicle builder palette crashes, and corrected a SmallPig name mismatch bug. I also refactored the authoring canvas rendering.

 * Emilio Garcia: I focused mainly on the authoring environment, game catalog authoring, cheat system, and AI/LLM level generation. Early in the project, I created the first game menu, authoring entry screen, screen factory, authoring screen layout, component palettes, CSS styling, and screen switching support. I also worked on making authoring views use properties files and integrating the authoring canvas into the main UI. I implemented the LLM help/chat window and AI level generation pipeline, including prompt files, generated level normalization, validation, JSON building, and generated terrain/object/pickup handling. I also designed and implemented the cheat system: cheat actions, key bindings, input handling, runtime context, target resolution, effect registry, cheat data records, validation, and cheat authoring UI. In the final sprint, I refactored the authoring and game catalog code by extracting controllers, draft models, sprite import services, catalog tabs, file actions, layout factories, and authoring mode controllers.


 * Mason Wu: I worked on the authoring canvas, level validation, scoring, player progression, logging, and level-selection flow. Early in the project, I implemented the initial authoring canvas view, component palette support, placed object representation, and terrain block/controller refactors. I also worked on undo/redo, fill/autofill behavior, occupancy grid logic, drag-and-drop placement, clear actions, save/load wiring, and validation for level files. I also implemented gameplay scoring and progression systems, including score calculation, elapsed time/progress tracking, play history persistence, user progression, locked/unlocked level behavior, furthest-level tracking, level scanning, result screen statistics, and terminal outcome handling. I also added logging support through log4j configuration and event logging for startup/shutdown, screen transitions, auth, game outcomes, HP changes, AI level generation, help chat sessions, and caught/uncaught exceptions.


 * Xuye Chen: I worked mainly on the game component model, world implementation, joints, and vehicle stability. Early in the project, I implemented the base `GameComponent` abstraction with position, velocity, force, mass, hitboxes, friction, and active state. I also created concrete component types such as vehicle components, drive components, finish line components, power-up components, and pig/main-character components. I also implemented the `World` abstraction for storing and filtering game objects, along with the joint interface and joint
implementations such as rigid, rope, spring, and revolute joints. Later, I worked on dynamic/static component abstractions, rigid joint fixes, vehicle stability issues, wheel/terrain behavior, component compaction, cursor behavior fixes, terrain descriptions,


## Design Goals

 * Goal #1: Make the engine as flexible as possible so that we can support many variations of our game. This includes making game components, collision handling, and joint resolution extensible by making them as independent as possible from each other. Specifically, we wanted to make it easy to not only write new game components but also integrate them into the game, namely into the joint resolution and collision detection and handling. That is, we wanted all of our logic to be as independent as possible from the components and from other parts of the logic. 
 
 * Goal #2: Make game variation data-driven so designers can create new worlds, levels, components, terrain, static objects, and assets without changing Java code. Instead of hard-coding one Bad Piggies game, we wanted game.json to define the available game pieces and level.json to define the map. This supports the Open-Closed Principle because new game content should usually be added by extending data rather than modifying core engine/controller classes.

 * Goal #3: Keep the major systems separated by responsibility so that authoring, data loading/saving, gameplay, physics, and rendering could evolve independently. The authoring environment should create valid data, the data layer should translate files into runtime objects, the engine should simulate physics without knowing about UI screens, and the view/controller code should coordinate user workflows without owning physics rules.


#### How were Specific Features Made Easy to Add

 * Game Components: We made game components easy to add by defining an interface (GameComponent) and a hierarchy with abstract classes that new classes (of new component types) could extend, only implementing what they needed. See [engine](#engine) for more details.
 
* New game variations: We made new game variations easier to add by moving game-specific information into data files. A new game can define its own components, sprites, terrain types, static objects, collision actions, controls, and background assets in game.json, while individual levels reference that catalog through level.json. This means a Mario-style game can use the same engine and player flow as Bad Piggies while changing the visual theme and component definitions.

* New levels and authored maps: We made new levels easier to add through the authoring environment and the LevelLoader/LevelSaver APIs. The authoring tool lets designers place terrain, spawn points, static objects, pickups, hazards, goal lines, and background images, then saves them into level files. The runtime does not need to know whether a level was hand-authored or loaded from an existing folder because both go through the same data layer.

 

## High-level Design

### Core Classes and Abstractions, their Responsibilities and Collaborators

#### Engine: Georgiy

 * Game component hierarchy: 
* The GameObject interface heads off the hierarchy, defining a contract, and abstract class GameComponent defines some basic implementation for code sharing between all the component classes. It defines methods that all objects will have, like getPolygon(), and getStaticState(). It also extends Renderable, so all components that implement GameObject must implement the methods required for the view to render them. We have made 3 basic types of components: abstract class StaticComponent, which don’t move and are intended for terrain, so don’t have a DynamicState, abstract class DynamicComponent, which do move and are the most general-purpose component, so do have a DynamicState, and abstract class PassthroughComponent, through which other objects ‘pass’ without affecting the state of either object. abstract class RigidBody is extended by DynamicComponent and StaticComponent for code sharing and type-checking purposes in the collision and joint solvers. All of our current components extend the 3 classes, but it is easy to add a new type of component into the hierarchy that implements some other features. 
* Other notable components/interfaces: Another notable component class is abstract class DriveComoponent, which is a subclass of DynamicComponent, which implements the Driveable interface that allows a thrust force to be added to the component’s total forces upon user input. Components implement Damageable to give them HP properties and the ability to be damaged in collisions.
* Geometry: For maximum extensibility, we decided to represent the geometry of game objects/components (we use these terms interchangeably) through convex polygons. A convex polygon (defined in ConvexPolygon) is simply a list of points that are implicitly defined relative to an object’s chosen center. That is, when creating an object, one will specify a position in 2D coordinates for the object’s center and provide a list of points that are interpreted relative to the center point as the object’s boundaries (in some cases, the constructor will initialize the polygon itself for ease of use, like in the case of boxes, which are frequently shapes for game objects). 
* State: All objects contain a StaticState, which is just a container for linear and angular position. Dynamic objects (abstract class DynamicComponent) also contain DynamicState, which is a container for the linear and angular velocities and accelerations. We use vectors to represent linear quantities and doubles for angular quantities, and the vector representation and useful math functions are defined in Vector.
 
 * CollisionDispatcher holds the collision resolution dispatching logic. Its main component is a ‘collision matrix’: a map of pairs of components to their corresponding collision solver. We knew that there would need to be different logic for collisions with different types of components: e.g., a collision between a static and dynamic component would not cause the static component to move, and a collision between a dynamic component and a powerup would not affect the motion of the dynamic component. To standardize the solvers, we defined the CollisionSolver functional interface, which all of the types of solvers implement and which takes a pair of objects and some context about the collision that was computed previously. The map is then from pairs of components to solvers that adhere to this interface, like DynamicDynamicSolver, DynamicStaticSolver, and so on. 
 
 * Physics orchestration: World is an interface that defines a contract to hold all of the game objects, joint constraints, and global physics variables like drag and gravity. A WorldImpl is instantiated and configured with all the components, joints, and physics values (which are stored in a PhysicsEnvironment record) and passed to a GameEngine. Importantly, the World also keeps track of which objects are active (those which have not been destroyed yet), and the current active dynamic objects, for the convenience of the GameEngine. 

    The GameEngine, implemented in GameEngineImpl, contains the physics loop. It accumulates time and operates in fixed steps, so it is independent of any particular time step. Its main responsibility is physics resolution, during which forces are integrated into velocities. constraints (collisions and joints) are solved, and velocities are integrated into positions, although it (or more specifically, its collaborator, CollisionPhase, dispatches collisions between objects that resolve gameplay mechanisms such as HP besides physics). 

Loop Details: We split the loop into distinct phases, and perform one operation for all components at a time for stability purposes.
We use DynamicComponentUtilities to perform operations on dynamic components such as force addition & application, velocity & position integration, and updating their polygons (not necessarily in this order). 
SweptCollisionResolver checks a component’s path through the timestep for collisions with static objects, which helps prevent ‘tunneling’, where fast-moving objects tunnel through static objects because the physics engine tick rate was too low. Checking the entire path for a tick bypasses this.
We delegate collision responsibilities to CollisionPhase, which handles detection and resolution. 
It first detects collisions by calling CollisionDetectorImpl, which implements the CollisionDetector interface. The interface contains one function that returns a Collision object (which contains context about the collision required for the solver) if there is a collision between the two objects passed to the detector. We decided to make this an interface so that different types of solvers could be implemented.
After all the collisions have been detected, solving happens in a few phases: First, impulses applied previously to a certain pair of objects (stored in a cache) are checked to see if they are suitable to be applied to this collision, and they are applied if so. This is called ‘warm-starting’, and it maintains continuity to reduce instability. Then, we solve for impulse and positional corrections separately. In the position and velocity steps, CollisionDispatcher’s dispatch() method is called so that it can apply the correct solver to the pair of objects.

 
 * Joint, AbstractJoint
The `Joint` interface defines the contract for constraints between components. A joint
does not represent a visible game object by itself; instead, it describes a relationship
that the physics engine must maintain between two components during simulation. For example, a joint may try to keep two components a fixed distance apart, allow rotation around an anchor point, or apply spring-like correction forces.

     `AbstractJoint` provides shared logic for concrete joint types. It stores the connected
  components/endpoints and centralizes common calculations that multiple joints need, such as
  reading component positions, computing displacement between anchors, and applying
  corrections. This avoids duplicating endpoint-management code across every joint
  implementation.

     Concrete subclasses encode different constraint rules. A `RigidJoint` keeps components
  locked together like a fixed connection, a `SpringJoint` allows stretch while pulling
  components back toward a rest length, a `RopeJoint` limits maximum distance without forcing
  a fixed distance at all times, and a `RevoluteJoint` allows rotation around a shared anchor.
  These subclasses use polymorphism: the engine can treat them all as `Joint` objects while
  each class implements its own constraint behavior.

     During each physics step, `GameEngineImpl` delegates joint resolution to `JointSolver`.
  This keeps the main engine loop from knowing the details of every joint type. The engine
  only coordinates the simulation phases, while each joint class defines what it means to
  satisfy its own constraint. This design supports extension because adding a new joint type
  should mostly require creating a new `Joint` implementation rather than rewriting the entire
  physics loop.

 #### 


## Assumptions or Simplifications

  - Decision #1: The physics engine assumes all moving gameplay objects can be represented as rigid bodies with convex collision polygons. More complex shapes, like terrain splines, are approximated by multiple convex slabs so they still work with the same impulse solver.

  - Decision #2: The engine is largely meant for side-view vehicle/platformer games, not arbitrary 2D games. Gravity, friction, joints, and terrain are all built around vehicles or player bodies moving across authored ground.

  - Decision #3: Collision response is separated from collision detection. CollisionDetector only detects overlaps, while CollisionDispatcher chooses the solver for each pair of object categories.

  - Decision #4: Game variation is mostly data-driven through game.json and level.json, but brand-new low-level physics behaviors still require small Java additions, such as adding a new component type, collision solver, or effect implementation.

- Decision #5: Components will be represented through convex polygons. We decided that these are versatile enough to represent most shapes, even circles that are normally represented by centers and radii so that they are ‘perfect’, which can be approximated.


## Changes from the Original Plan 

 - Change #1: The idea to use a strategy pattern to handle collisions with different types of objects did not work and had to be replaced by the collision matrix. This way we could provide a lookup value to determine what specific type of collision existed within that part of the matrix. 
 
  - Change #2: The original plan described many per-game behaviors as fully configurable, but in practice the stable approach was to keep the physics engine fixed and expose only safe variation points through data: component catalogs, terrain types, static collision actions, input forces, level physics, win/loss settings, and assets.

  - Change #3: Also, our original plan focused on solely making individual levels within Bad Piggies only. The notion of changing our authoring environment to be more ‘extensible’ forced us to consider better world options, where we have to define all the custom components (Mario sprites) and static objects before authoring a level within that world. That required us to split level.json files up into world.json files as well.

  - Change #4: Vehicle stability required more engine work than expected. The solver moved toward a split-phase impulse approach with joint velocity solving, positional correction, warm starting, and special handling for rigid/revolute joints to avoid unstable jittering of the car’s components.


## How to Add New Features

  #### Features Designed to be Easy to Add 

  - Feature #1: New game variations through data files
    Add a new folder under data/games/<gameId>/game.json and matching levels under data/levels/<gameId>/. The catalog can define different vehicle parts, sprites, static objects, terrain types, controls, masses, HP values, and collision actions without changing the engine. These files can be generated easily through the authoring environment provided the required assets like sprites and a background image are present.

  - Feature #2: New levels for an existing game
    Create another level.json that references the existing gameType. Designers can change terrain splines, spawn location, gravity, drag, pickups, enemies, goal placement, background image, and allowed vehicle parts entirely through level data.

  - Feature #3: New visual themes / schematics
    A game can look different by changing sprite paths, terrain colors, background images, and CSS/resource files. For example, Mario uses the same gameplay systems as the other games but swaps in Mario assets and different level data.

  - Feature #4: New pickup or hazard effects using existing action types
    Static objects can be added in game.json with collision actions like score, damage, heal, or finish. Then levels can place those objects anywhere in the map without adding new Java code.

- Feature #5: New types of game components can be added by simply extending one of the existing classes in the game component hierarchy, or even implementing the GameObject interface. Then, solvers must be written to resolve collisions between the new object and desired existing objects and added to the solver map in CollisionDispatcher.



#### Features Not Yet Done
 
* Multiplayer / networking: We did not implement networked multiplayer or matchmaking. The current architecture assumes one local World and one local gameplay session. To support multiplayer, we would need a networking layer, synchronized world state, deterministic or server-authoritative physics updates, and UI for joining or hosting sessions.

* Ghost racing: A ghost racing mode would record a previous run's position over time and replay it as a non-interactive visual object during a later attempt (Mario Kart reference). This fits the existing replay/progression direction, but we would need to persist time-stamped player positions and add a view/model object that renders the ghost without participating in collisions.

* Sound effects and music: The project has visual assets and CSS/resource files, but not a full audio system. A good design would add an AudioService that maps game events, such as collision, pickup, win, loss, button press, or engine activation, to sound resources defined in
data. This would avoid hard-coding audio calls throughout gameplay classes.

* More general main-character support: Some game variations, like Mario, work better as direct character-control games than vehicle-building games. Our data-driven system can fake this with invisible transport components, but a cleaner design would add a general
  ControllableActor or MainCharacterComponent abstraction so each game can choose between vehicle assembly and direct actor control.
