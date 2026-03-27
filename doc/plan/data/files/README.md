# Example Data Files — Good Piggies (OOGA)

## Design Conventions

1. **JSON everywhere** — all config, level, vehicle, and profile data is JSON.
2. **Game config files** define game-type-level settings (scroll behavior, build mode, input mapping, win condition type). These are what make the same engine run Bad Piggies vs. Geometry Dash.
3. **Level files** describe terrain, obstacles, goals, and game objects. The schema is shared across game types — only which fields are populated changes.
4. **Vehicle files** describe component layouts on a grid with attachment relationships.
5. **Physics config** provides global defaults; individual game configs or level files can override specific values.
6. **All positions use a grid coordinate system** — (0,0) is bottom-left of the level. One grid unit = one block width.

## Key Abstraction: How One Engine Supports Multiple Games

The `game_config.json` file is the primary differentiator. It controls:
- Whether the player builds a vehicle or gets a fixed one
- Whether the camera auto-scrolls or follows the player
- What input actions are available
- What constitutes a win or loss

The level file schema stays the same — terrain blocks, obstacles, collectibles, start/goal positions — but different game types use different subsets. For example, Geometry Dash levels have no vehicle builder and use `auto_scroll` camera mode, while Bad Piggies levels enable `build_mode` and use `follow_player` camera mode.
