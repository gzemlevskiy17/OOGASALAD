# OOGA Scope Plan Discussion
## FUN Team Name: Good Piggies
## Student Names
Georgiy Zemlevskiy, Michael Li, Paulina Vvedenskaya, Iurii Beliaev, Mason Wu, Xuye Chen, Emilio Garcia


### Project Goals
* Create a playable replica inspired by Bad Piggies, focusing on physics‑based vehicle construction and puzzle solving. I played ALL of Bad Piggies. Other examples of games that we would like to support include platformers like geometry dash, super mario bros, jetpack joyride, Hill Climb Racing.
* The player will build contraptions using different components and attempt to transport a pig safely to the goal across multiple obstacle‑filled levels. 
* Allow the player to build levels (and change the car) through the no-code environment

### Project Emphasis
* Modeling real physics interactions/game play.
* Construction and creativity
	* Add interesting car customizations so that players assemble vehicles using wheels, frames, engines, and special items.
	* Make level and car construction flexible enough so that many different types of platformers can be created just by changing parameters, cars, or levels in the authoring environment.
	* Design the physics engine in a way that supports easy adding of features like:
* car movement mechanisms, 
* different types of materials that the car interacts with (exploding blocks)
* in game incentives like coins 
* multiple & different gameplay subjects (e.g. different pigs with different physics behaviors that may be scattered in the level).

* User-friendly UI so that players can easily and reliably drag and drop car components in the car-builder mode


### Project Extensions
* Basic:
	* Player Profiles (name, pfp, password, vehicles/customizations)
	* High Score (saved via which track/car config)
	* Ghost Racing (compete against your previous run or best run for a given level)
* Challenge
	* Multiplayer gaming/networked players (introduce queuing system)
	* Procedurally generate levels (if we have time for it).

### Sprint Progress Goals
	* Plan
		* plan project, design APIs, agree to standard file formats
	* Sprint 1: working on a basic game with a basic car and two basic levels.
		* Gameplay: 
* Implement a basic physics system that ensures vehicle reacts realistically to obstacles:
	*Gravity, collision, friction, and boundaries
	* Interactions between vehicle components and terrain
	* Vehicle Construction 
* Define and implement relationships between blocks (attachment points, constraints and joints)
Create a library of car building blocks such as frames, wheels, and structures
* Implement the relationship of the blocks so that the car actually works

		* Levels and Progression 
			* Implement two basic levels
* Support the transition between levels.
* GUI 
* start screen, gameplay, saving, and view window moving as the   car moves forward to keep the car in the middle of the screen
		* Authoring Environment (initial start)
* Start working on the authoring environment. Prioritize building a mechanism to generate level files.
	*Define level schema/level reading API.
	*generate and load levels programmatically 
	* Sprint 2: Persistence, Profiles, Competitive Replay
		* Gameplay
		* Player Profiles & Saving
* Implement player profiles w/ name, pfp, and auth
* Add saving systems to store player-specific vehicles and customization
	* Load saved data reliably across sessions
* Ghost racing 
			* Run recording to capture vehicle position, rotation, etc. over time
			* Implement ghost playback w/ a translucent ghost vehicle
			* Allow players to race against these ghosts
			* Implement a “winner”/competition against said ghosts
		* Expanded authoring environment
			* improve authoring tools to edit terrain, obstacles, and goals
			* Add additional car tools (engine, balloons, etc.)
	* Sprint 3: 
		* Gameplay
		* Add high score
			*potential metrics for efficiency/cost 
			*integrate high scores into the u (ex. leaderboard)
		* Multiplayer experience 
			* Synchronous racing to enable players to compete the way a ghost competes with the players 
			* Data comparison page
			* Handle networking concerns like data/latency
		* Procedurally generate levels (if we have time).
	* Authoring environment
	* Complete: 
		* Finish unimplemented features and test/debug 
	* Demo
		* create additional example data files and record a demo of the games chosen to show off your design's flexibility