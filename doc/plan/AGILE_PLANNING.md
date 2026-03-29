# OOGASalad Agile Plan Discussion
## Good Piggies
## Georgiy Zemlevskiy, Michael Li, Iurii Belaev, Paulina Vvedenskaya, Mason Wu, Xuye Chen, Emilio Garcia


### Backlog (Feature Requirements)
See BACKLOG.md   
   

### Sprint 1
Implement a basic game player with working car and level editing environments.

#### Game Player

 * Iuriii Belaev - View-related APIs
   * Implement abstract/concrete basic classes

   * Implement basic Screen Sequence for Player environment

 * Michael Li - Authoring Environment
   * Feature/Use Case 1: Implement loading and storing of level file bundles (level files, available objects, etc.); Edit existing game, and ‘save’ buttons

   * Feature/Use Case 2: Implement tiling & terrain drawing, Dragging and snapping objects, saving those to the backend object.

 * Georgiy Zemlevskiy - Engine & Component APIs
   * The physics engine will update the positions of components based on the forces applied to them and handle collisions.

 * Paulina Vvedenskaya - Controller-related APIs
   * Implement InputRouterImpl and GameControllerImpl to reads input_mapping at startup
   * Implement ScreenManagerImpl and all application screens
   * Implement ScreenManager with UI Screens; wire restart, pause, and post-win/loss input actions

 * Xuye Chen
   * The abstract component that implements game objects and other base properties

   * World manager that stores all objects and joints

 * Mason Mu - Authoring Environment
   * Feature/Use Case 1: the backend store level and object state before they are written to files.

   * Feature/Use Case 2: the views that display available terrain objects and the level canvas.

 * Emilio Garcia
   * Feature/Use Case 1

   * Feature/Use Case 2

 * LLM: Cody
   * Implement VehicleLoaderImpl (parses starter_cart.json, instantiates a GameObject for each component entry, and populates the World 
   * Implement PhysicsConfigLoader which reads physics_config.json and extracts gravity, friction, restitution, and timestep values to configure GameEngine at startup


### Sprint 2

Persistence, Profiles, Competitive Replay
 

### Sprint 3

 * Polish core features and implement challenging features.



### Sprint 4 (Complete)

* Finish unimplemented features and test/debug 
* create additional example data files and record a demo of the games chosen to show off your design's flexibility

For more information about planned features, refer to SPRINT_PLANNING.md
