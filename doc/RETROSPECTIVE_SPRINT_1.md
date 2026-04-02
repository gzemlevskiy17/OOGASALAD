# OOGASalad Stand Up and Retrospective Discussion
### TEAM 3
### Georgiy Zemlevskiy, Michael Li, Iurii Belaev, Paulina Vvedenskaya, Mason Wu, Xuye Chen, Emilio Garcia


## Stand Up Meeting

### Georgiy Zemlevskiy

* Work done this Sprint
    * Worked on the physics engine:
        * Implemented position and velocity integration
        * Build out abstractions necessary for game object state and collision detection
        * Worked on collision detection
* Plan for next Sprint?
    * Finish the physics engine.
        * Top priority: understand force transfer and implement Actions for DriveComponents
        * Refactor, test, and connect together collision detection
        * Test position and velocity integration
        * Implement joints
* Blockers/Issues in your way
    * Difficult to test without a game player interface, but it is possible. I will need to build pure model tests anyway.

### Team Member 2 - Michael Li

* Work done this Sprint:
* Implemented LevelLoader
  Parses a JSON file, and returns a level populating the World with TerrainBlock objects.
* Implemented LevelSaver
  Serializes all TerrainBlock objects from the World back to the same JSON format.
* Implemented GameConfig
  Exposes settings via dotpath keys and reads Game_config json.

* Implemented TerrainBlock
  Concrete static terrain object and stores size, position, friction, tiling
* AuthoringCanvas and AuthoringController
  renders TerrainBlock objects, canvas draws grid lines using a JavaFX grid canvas.

* Plan for next Sprint?
* Fix wiring to support running current level, add a “Play” button in authoring screen. Loads the current level into the game player and starts it.


* Blockers/Issues in your way

Xuye needs to add World.removeObject() to enable erase tool, and World.clear() to enable proper level reloading so that we can integrate a reload within the auth env as well.

Need to figure out which AuthoringCanvasImpl to use moving forward


### Paulina Vvedenskaya

* Work done this Sprint
    * Created the GameController Impl → main methods are start, stop, and pause. holds the game loop and updates what should happen in the next frame via tick(), which advances the physics via gameEngine, redraws, checks win/loss.
    * Implemented some Action classes for balloon and engine
    * Implemented pause and start and stop actions that actually stop the GameController loop
* Plan for next Sprint?
* Player Profiles & Saving
  * Implement the views for logging in
  * Implement the backend (database) for logging in.
* Implement player profiles w/ name, pfp, and auth
* Add saving systems to store player-specific
* Implement saving player preferences
* Blockers/Issues in your way
    * not entirely sure what

### Xuye Chen

* Work done this Sprint
* Abstract GameComponent class, PowerLevelComponent, TerrainComponent, FinishLineComponent, VehicleComponent, StructuralComponent, DriveComponent

* Plan for next Sprint?
    * joint
    * World Interface - maintain the Object list
    * GameComponent&ConcreteGameObject Similarity
* Blockers/Issues in your way


### Emilio Garcia

* Work done this Sprint
  Created various AppScreen Implementations, built starting UI as well as UI for authoring environment entry point and the actual authoring environment.

* Plan for next Sprint?
  Integrate backend functionality for authoring environment actions like run, save, load, as well as drag and drop from ComponentPalettes. Will also refactor classes to better fit MVC

* Blockers/Issues in your way
  Need backend logic for everything (vehicles, drag and drop, object parameters). Need to figure out whose canvas we will use


### Mason Wu

* Work done this Sprint
* Implemented authoring canvas and created PlacedObject that contains mouse location and compute the locations of the blocks that are placed onto the canvas
* Implemented ComponentPalette for drawing samples of the block (currently dummy rectangles in PlacedObjectFactory)

* Plan for next Sprint?
* Work on other level attributes panels and navigation (controller)
* Work on tool selector

* Blockers/Issues in your way
* Resolve the current overlapping/conflicting files


### Iurii Beliaev

* Work done this Sprint
  Frontend player flow (except for win/lose screens) UI + navigation: AbstractAppScreen, GamePlayAppScreen, PlayerEntryAppScreen, VehicleBuilderAppScreen
  View foundation structure

* Plan for next Sprint?
  Finish Player View

* Blockers/Issues in your way


### LLM

* Work done this Sprint
  None

* Plan for next Sprint?
  Help with player View
  File parsing

* Blockers/Issues in your way




## Project's current progress
Basic implementations in all areas are almost done. Missing integration between parts. Model testing is somewhat implemented, UI needs to be tested.


## Current level of communication
Not ideal, sometimes it is very hard to have a team meeting with everyone in it, though it is understandable (7 people in a team). We will attempt to meet after class on Tuesday so that everyone is there for the “Final Bash” check in. We can also split into smaller subteams to discuss everything.


## Satisfaction with team roles
Overall fine, though we might try to find a better distribution of work (some members work more than the others).

## Teamwork that worked well

* Thing #1 - Everyone was decently responsive in both the Discord and the imessage group chat.

* Thing #2 - The merging process was good because we talked to each other before pushing/pullings. Also the apis were predefined.


## Teamwork that could be improved

* Thing #1 - Some of the meetings were not as productive as it could have been because we didn’t think about what to talk about before hand.

* Thing #2 - We didn’t communicate clearly about specific class implementation so we ended up with several classes that were almost duplicates of each other.


## Teamwork to improve next Sprint
Try to assign issues more evenly and together. Raise concerns and discuss potential blockers ASAP. If your implementation if dependent on the other person’s work - consult with them/let them know what you are doing
