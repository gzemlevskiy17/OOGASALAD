# Schema Decisions Log

Old level files in `data/levels/{forest,glacier,mars,moon,desert}/` will be **deleted** rather than migrated. Fresh example files will be authored against the new schema later.

## Decisions made

| # | Topic | Decision |
|---|-------|----------|
| 1 | Vehicle palette cap (`max_selectable`) | Re-added as `mainCharacter.maxSelectable` (required, integer ≥ 0). |
| 2 | Existing rectangle-terrain levels | Delete; do not migrate. |
| 3 | Camera follow target | Marked once in `game.json` as `primaryComponentId`. Levels do not specify it. |
| 4 | Per-level physics | `gravity` and `drag` are required per level — no engine fallback. |
| 5 | Multiple splines per level | Flat `map.knotLocations` only. Single spline. |

## Things being dropped from the old format

These fields existed in old level files and are not represented in the new schema. Flag any that should be re-added before deletion:

- `restitution` — per-terrain bounciness
- `splines[].controls[]` — extra Bezier control points (now derived from knots)
- `splines[].resolution`, `splines[].baselineY` — tessellation params (now engine constants)
- `pickups[].size`, `rocks[].size` — sprite size (uniform 60×60 per project convention)

## Open follow-ups (not in this ticket)

- New example `game.json` + `level.json` pair will be written from scratch later (separate ticket).
- Loader/saver/validator rewrites: GP-130, GP-131.
- Old loader code stays in place until the new path is wired up; old level JSON files can be deleted at any time once nothing reads them.
