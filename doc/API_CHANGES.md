## OOGASalad API Changes

### Team

### Names

### Changes Made

#### API #1

* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)


* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)

#### API #2

* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)


* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)

#### API #3

* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)


* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)

#### API #4

* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)


* Method changed:

    * Why was the change made?

    * Major or Minor (how much they affected your team mate's code)

    * Better or Worse (and why)

#### Vehicle part construction refactor (`VehicleFactory` → `VehiclePartRegistry`)

* Method changed: `VehicleFactory.createComponent(String, double, double)` removed; replaced by
  `VehiclePartRegistry.get(String).create(double, double)`. `VehicleFactory` no longer dispatches on
  a string `componentType` via a switch (and no longer owns the twin `configureThrust` switch).
  `WHEEL_TYPES`, `PIG_TYPES`, and the `isNonWheelCell` / `isNonWheelPair` helpers are gone —
  joint-style routing now goes through `VehiclePartSpec.jointStyle()` (a `JointStyle` enum value:
  `WHEEL`, `PIG`, `STRUCTURAL`).

    * Why was the change made? The post-merge `createComponent` had a literal unreachable-code bug:
      it
      returned before calling `configureThrust`, so every drive part silently used constructor
      defaults. Fixing that exposed the deeper issue — the same `componentType` string was
      dispatched
      on in four places (component construction, thrust config, wheel-set check, pig-set check),
      each
      a separate source of truth that had to be kept in sync. The fix turns each palette kind into a
      `VehiclePartSpec` implementation that owns its own `create()`, joint style, and dimensions.

    * Major or Minor: Minor. `VehicleFactory.buildVehicle(VehicleLayout, World, double, double)` —
      the
      only public entry point — is unchanged. A no-arg `VehicleFactory()` still works; a new
      `VehicleFactory(VehiclePartRegistry)` constructor exists for test injection.

    * Better: adding a new palette kind goes from editing four separate strings to adding one line
      in
      `VehiclePartRegistry`. Drive parts set their thrust in a single constructor-time call instead
      of
      the prior build-then-mutate pattern. Joint pairing rules are expressed as `JointStyle`
      comparisons, not string-set lookups, so a typo in a palette name surfaces as a registry miss
      rather than silent mis-routing.

#### `VehicleComponent` / `DriveComponent` / `StructuralComponent` constructors — drop `spritePath`

* Method changed:
  `VehicleComponent(double x, double y, double rotation, ConvexPolygon polygon, String tag, double friction, double mass, double momentOfInertia, double restitution, boolean affectedByGravity, double maxHp, String displayName, String spritePath, double bodyWidth, double bodyHeight)` →
  same signature with `spritePath` removed. Corresponding changes in `DriveComponent` and
  `StructuralComponent` constructors. `VehicleComponent.getSpritePath()` and the `spritePath` field
  are deleted.

    * Why was the change made? The merge added `spritePath` as a field on model components, storing
      view-layer state inside physics objects. The field was not read by any caller;
      `getSpritePath()`
      had no usages. Removing it restores the model's independence from sprite concerns. (
      `implements SpriteRenderable` on the model classes remains for now — separate follow-up.)

    * Major or Minor: Minor. Call sites were three places in `VehicleFactory` (rewritten wholesale
      in
      the same change) and two test files (`VehicleComponentTest`, `GameEngineCollisionFilterTest`),
      both updated to drop the `""` spritePath argument.

    * Better: smaller constructor surface, no dead field, model no longer carries a view-layer
      string.
      Sprite resolution can happen in the view layer via `getComponentType()` (already exposed)
      without the model knowing about assets.

#### Replace view-side `SpriteRenderable` with model-side `TypedComponent`

* Method changed: `oogasalad.view.core.api.SpriteRenderable` deleted. New
  `oogasalad.model.engine.api.TypedComponent` takes its place — same single method
  `String getComponentType()`, but in the model package. `PigComponent`, `VehicleComponent`, and
  `GoalLineComponent` now `implements TypedComponent` instead of `implements SpriteRenderable`. The
  `oogasalad.view.core.api.SpriteRenderable` import is gone from the model.

    * Why was the change made? The prior setup had the physics model implementing a view-package
      interface, i.e. the model depended on the view. `SpriteRenderable` was a marker with a single
      type-name getter — nothing sprite-specific — and the view's `PaletteSpriteRegistry` only used
      it
      to read the type string. Relocating the interface to the model flips the dependency to the
      correct direction (view → model) without changing runtime behavior.

    * Major or Minor: Minor. Two view/controller consumers updated in place: `PaletteSpriteRegistry`
      (swapped `instanceof SpriteRenderable` → `instanceof TypedComponent`) and
      `GameplaySessionFactoryImpl` (same swap plus error-message text). One test
      (`PigComponentTest.pig_isSpriteRenderable`) renamed to `pig_isTypedComponent`.

    * Better: removes a model-on-view dependency; interface name now describes what it actually does
      (exposes a component type) rather than hinting at rendering; view code now depends on a stable
      model type instead of a shape-for-shape view-layer marker.
