## Sprint 1: basic game player with a working car and level editing environments.

* Implement all APIs/abstractions defined in the design plan
* Game player :
* Implement the physics engine
* Implement game component abstractions.
* Implement the required concrete classes so that
    * A few basic vehicle components (drives, structures, wheels) are in the library can can be used on the vehicle.
    * A basic vehicle can be assembled (components can be attached to each other and exhibit common behavior when appropriate.
    * Vehicle components will accurately collide with world objects on their own, and collisions will affect the whole vehicle when appropriate.
* Implement the parsing of level files (including level, game objects, and other properties).
* Levels and Progression
    * Implement two basic levels
        * Support the transition between levels.
    * Implement a basic level selection screen (could just select a file/folder)
* Basic View-related APIs \- Iurii
    * start screen, gameplay, saving, and view window moving as the   car moves forward to keep the car in the middle of the screen
    * Implement rendering to the view.
* Basic Controller-related APIs \- Paulina
* Implement a button to activate the drive component.

* Authoring Environment \- prioritize level creation mechanism so that the game player team can test
    * Implement loading and storing of level file bundles (level files, available objects, etc.)
        * Edit existing game, and ‘save’ buttons.
    * Implement the views that display available terrain objects and the level canvas.
    * Implement tiling & terrain drawing.
        * Dragging and snapping objects, saving those to the backend object.
    * Implement the views for the vehicle object selection tab (no attributes yet).
        * Currently selected objects and full object library.
    * Implement the necessary backend store level & object state before they are written to files.
* Implement camera that follows the vehicle while the game is playing.

## Sprint 2: Persistence, Profiles, Competitive Replay  
* Game player  
* Player Profiles & Saving  
* Implement the views for logging in  
* Implement the backend (database) for logging in.  
* Implement player profiles w/ name, pfp, and auth  
* Add saving systems to store player-specific	  
* Implement saving player preferences  
* Ghost racing   
* Run recording to capture vehicle position, rotation, etc. over time  
* Implement ghost playback with a translucent ghost vehicle  
* Allow players to race against these ghosts  
* Implement a “winner”/competition against said ghosts

* Support key input to activate vehicle components.
* Implement a more robust level selection screen, with more level files for testing.
* Animate some component motion (like wheels)


* Implement the settings screen where users can customize their profile.
* Authoring Environment
    * Support running the current level.
    * Add a panel to view/edit global level attributes and terrain object attributes.
    * Add a panel to view/edit global vehicle attributes and vehicle object attributes.
    * Add validation to component placement and field values with corresponding view behavior.

## Sprint 3: polish core features and implement challenging features.  
* Gameplay  
* Add high score  
* potential metrics for efficiency/cost   
* integrate high scores into the u (ex. leaderboard)  
* Powerups  
* Implement the friends menu on the profile page, where users can search for, add, remove, and invite (to multiplayer sessions) friends.  
* Multiplayer experience   
* Synchronous racing to enable players to compete the way a ghost competes with the players   
 Data comparison page  
* Handle networking concerns like data/latency  
* Add endless mode (levels are generated as the player progresses through the level.
## Sprint 4  
* Finish unimplemented features and test/debug   
* create additional example data files and record a demo of the games chosen to show off your design's flexibility