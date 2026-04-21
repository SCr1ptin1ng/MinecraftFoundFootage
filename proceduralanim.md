# Procedural Spider Animation Notes

This document tracks how the current procedural "spyder" works, what has already been built, what is currently broken, and what should happen next.

The goal is to preserve the current progress before further iteration.

## Goal

Build a procedural ambient spider-like creature for `level959` that:

- crawls on floors, walls, and ceilings
- feels like it belongs to the architecture
- reads as a living crawler instead of a regular mob
- stays debug-friendly while we prototype

It is not meant to be a standard combat entity.

## Current Architecture

### Core Files

- `src/main/java/com/sp/entity/custom/SpyderControllerEntity.java`
- `src/main/java/com/sp/entity/client/renderer/SpyderControllerRenderer.java`
- `src/main/java/com/sp/world/generation/Level959SpyderDirector.java`
- `src/main/java/com/sp/init/ModEntities.java`
- `src/main/java/com/sp/init/ModItems.java`
- `src/main/resources/assets/spb-revamped/models/item/spyder_debug.json`

### High-Level Design

The current spyder is split into two major systems:

1. Controller / logic
2. Renderer / procedural rig

The controller owns:

- debug-follow behavior
- surface detection
- surface normal and forward vectors
- body tilt support sampling
- per-leg planted target positions
- grouped stepping logic

The renderer owns:

- drawing the body and head from cuboids
- drawing all leg segments procedurally
- aligning the whole rig to the sampled surface basis
- solving each leg chain toward its per-leg target

## Current Debug Workflow

The `spyder debug` stick is used to drive the creature manually.

While holding it:

- a ray is cast from the camera center
- the spyder follows the hit location
- the surface normal determines floor / wall / ceiling alignment
- support markers render as particles for body tilt debugging

Current support marker colors:

- red: front support
- green: back support
- blue: left support
- yellow: right support

This is useful for checking:

- body orientation
- whether the ray target is stable
- whether the support samples are landing where expected

## What Is Working

### 1. Surface Alignment

This is one of the strongest parts of the current implementation.

The spyder can now:

- orient to floors
- orient to walls
- orient to ceilings

This happens by computing:

- `surfaceNormal`
- `surfaceForward`
- a derived local basis from those vectors

The renderer uses that basis to rotate the full rig so the body follows the contacted surface.

### 2. Body Tilt Sampling

The controller samples four body support positions:

- front
- back
- left
- right

Those are used to estimate:

- pitch
- roll

This is currently good enough for early traversal testing and debugging.

### 3. Per-Leg Independent Targets

This was a major improvement over the earlier shared-support version.

The controller now stores:

- one root offset per leg
- one desired foot offset per leg
- one planted target per leg
- one step start/end pair per leg
- one stepping flag per leg

This lets each leg move independently instead of just borrowing a left/right shared target.

### 4. Step Grouping

Legs are currently split into alternating gait groups.

This is not final, but it is much better than moving all legs freely every frame.

It reduces:

- total jitter
- obvious synchronized noise
- fake "buzzing" motion

### 5. Procedural Chain Solving

The renderer moved through several generations:

1. pure oscillation
2. shared support posing
3. target-based reach
4. serial segment solving

The current version uses a serial chain solve that tries to bend:

- femur first
- tibia second
- lower segment third
- claw last

This is closer to a robotic arm / finger chain than to a decorative wobble.

## Current Failure Pattern

After the latest passes, the prototype is no longer completely broken, but the remaining issues are structural rather than cosmetic.

### What the Recent Logs Are Telling Us

From the current `run/logs/latest.log` spider debug output:

- most legs stay planted while only one leg steps at a time
- `targetDelta` often grows very large in the forward/back axis before a leg is allowed to move
- `tipGround` stays at a small fixed offset, which means contact is being faked more than truly supported
- the body clearance is staying in a narrow band instead of reacting strongly to changing support geometry

That matches what we are seeing in-game:

- speed is too low
- legs still intersect each other
- feet do not convincingly carry the body
- the body still feels controller-led instead of leg-led

### Root Cause

The current system is still fundamentally:

- body/controller first
- leg reaction second

Cymaera's spider is fundamentally:

- leg support first
- body solved from support second
- gait permissions and stepping rules baked into the body/leg runtime

That difference matters more now than any individual tweak.

## What Is Still Broken

### 1. Extremely Important: Body Is Too Belly-Down

This is currently the biggest structural problem.

The spyder still tends to sit too flat on the contacted surface, almost like it is laying on its belly.

This causes several downstream problems:

- the leg roots start too close to the terrain
- the bend plane becomes awkward
- knees visually bend the wrong way
- legs clip into the floor or wall
- the creature reads as collapsed instead of supported by its limbs

This must be treated as the next top-priority animation fix.

### 2. Knee / Elbow Direction Can Still Flip

The current solver reaches the target, but the bend direction is not fully constrained.

So depending on position, a leg can:

- fold inward
- choose the wrong bend side
- look like the knee is inverted

This is why some poses still look broken even though the feet are more independent now.

### 3. Clipping Is Still Present

Even after raising body clearance multiple times, clipping still happens because:

- root positions are too low relative to the body
- the body is too belly-down
- per-leg targets do not yet enforce a minimum clearance rule
- the bend plane is not pole-constrained

### 4. Locomotion Read Is Better, But Still Not Fully Convincing

The legs are improving, but they still do not fully read as:

- stepping
- pushing
- pulling body weight

The main reason is that the feet are independent, but the body does not yet feel clearly suspended by them.

The motion still looks partly like animated appendages instead of fully weight-bearing limbs.

## Restart Strategy

This is the new plan going forward.

We should not throw away the whole spider feature, but we should stop trying to salvage the current controller as the long-term architecture.

The right move is:

- keep our entity entrypoint
- keep our renderer and custom visual model direction
- keep our debug stick and debug overlays
- rebuild the locomotion runtime around Cymaera's body/leg/gait architecture

In other words:

- use Cymaera's movement workflow
- use our rendering and game integration

## New Target Architecture

### Keep From Our Project

- `SpyderControllerEntity` as the Minecraft entity and debug interaction owner
- `SpyderControllerRenderer` as the visual output layer
- debug stick modes and target selection
- our custom body proportions and black "void block" look
- future `level959` spawning / director logic

### Replace or Rebuild

- current stepping scheduler
- current body follow controller
- current planted-foot ownership model
- current support-to-body relationship
- current mixed controller/IK responsibilities

### Borrow Directly From Cymaera

- `SpiderBody` style central locomotion runtime
- `Leg` style memo/update/target/step ownership
- `Gait` profile values
- `GaitType` stepping permissions and update order
- target grounding / stranded target logic
- body preferred height from support and look-ahead
- leg cooldown logic based on pair relationships

## Planned Refactor Phases

### Phase 1. Freeze the Renderer Contract

Goal:

- keep the renderer dumb
- renderer only consumes solved joints, body orientation, and debug markers

Rules:

- no gait logic in the renderer
- no target selection in the renderer
- no body support solving in the renderer

### Phase 2. Introduce a New Body Runtime

Create a new internal runtime layer, likely as nested types or new files:

- `SpyderBodyRuntime`
- `SpyderLegRuntime`
- `SpyderGaitProfile`
- `SpyderGaitType`

This runtime should own:

- position
- velocity
- orientation
- preferred orientation
- support polygon / support normal
- grounded state
- gait selection
- leg update order

This is the part that should become "Cymaera-style".

### Phase 3. Make Legs Primary

Each leg should own:

- memo positions
- trigger zone
- comfort zone
- grounded target
- stranded target
- current end effector
- step state
- cooldown timers

The leg decides whether it wants to move.

The gait decides whether it is allowed to move.

The body should no longer drag the feet around as the primary source of motion.

### Phase 4. Rebuild Body Height and Suspension

The body height must be computed from:

- grounded leg targets
- actual grounded leg effectors
- support normal / support polygon
- velocity / look-ahead

The body should feel suspended between planted legs, not simply offset upward by a fixed amount.

### Phase 5. Port Cymaera Gait Logic

We should use Cymaera's ideas almost directly here:

- diagonal update ordering
- cross-pair grounding checks
- same-pair / cross-pair cooldowns
- uncomfortable / outside-trigger stepping
- special handling when a target is no longer grounded

This will help fix:

- very slow stepping
- only one useful leg moving
- tangled legs waiting too long

### Phase 6. Re-solve the Leg Chain

Only after Phases 2 through 5 are in place should we revisit the leg chain solver again.

At that point, if needed, we can swap the planar solve for:

- a cleaner constrained analytic chain
- or a proper FABRIK pass with bend constraints

But solver work should come after locomotion ownership is fixed, not before.

## Immediate Next Implementation Pass

The next code pass should do only these things:

1. Create a new Cymaera-style runtime inside our project without deleting the current renderer.
2. Move step permission logic out of ad-hoc controller code and into gait/leg runtime.
3. Make body velocity and height derive from planted leg support.
4. Keep our current visual model and debug stick intact.

We should not do another "micro-tune the old controller" pass first.

## Success Criteria For the New Runtime

The new runtime is only good enough when all of these become true:

- the spider can walk to a clicked point instead of visually teleport-dragging
- at least two gait groups visibly alternate
- feet stay planted until a leg is actually stepping
- body height clearly reacts to leg support changes
- the spider does not visibly float when crossing simple height changes
- front, middle, and back legs all participate in locomotion
- leg crossing is rare and understandable instead of constant

## Iteration Rule

Going forward, we should judge each spider locomotion pass against a simple 5-point counter:

- if a pass creates visible progress, the counter stays where it is
- if a pass mostly reintroduces the same failure mode, reduce the counter by 1
- when the counter reaches 0, stop tweaking and hard-reset the locomotion runtime toward the Cymaera architecture

Current status:

- we are close enough to keep the feature alive
- but structurally close enough to the limit that the next pass should already be the architecture pass, not another tuning pass

## Current Controller Notes

### In `SpyderControllerEntity`

Important concepts already implemented:

- `surfaceNormal`
- `surfaceForward`
- support sampling for pitch and roll
- `legTargets`
- `legStepStarts`
- `legStepEnds`
- `legStepProgress`
- `legStepping`
- alternating leg groups

Leg movement uses:

- desired foot offsets in local space
- world-space contact sampling
- planted targets that only update through stepping

This is the correct foundation to keep.

### In `SpyderControllerRenderer`

The renderer currently:

- converts world target positions into local rig space
- solves each leg chain toward its own target
- applies a serial bend through the segments
- applies a claw correction so the tip wants to meet the surface more cleanly

This is much better than the first versions, but still needs stronger constraints.

## Recommended Next Fixes

## Iteration Rule

From this point on, spider locomotion should be judged against a fixed checkpoint rule instead of just "looks a bit better".

Every implementation pass gets a 5-point counter.

- Start each pass at `5`
- After testing in game, subtract `1` if the pass does not meet the current checkpoint
- When the counter reaches `0`, stop tuning and do a sanity check
- If the sanity check still says the system is going in circles, reset the approach and port the matching Cymaera workflow more directly

### Current Checkpoint For The Next Pass

This pass only counts as successful if all three conditions are true at the same time:

- legs remain the primary driver and the body follows planted support instead of leading it
- the spider shows visible forward locomotion instead of side-to-side jitter
- planted feet stay near the contacted surface instead of hovering far above it or clipping deeply through it

### Current Sanity Trigger

If we burn all 5 points without meeting the checkpoint, the next move should be:

1. freeze new tuning
2. compare our control flow against Cymaera again
3. replace the current walker ownership model with a more direct leg-state / step-state workflow instead of continuing to patch the same body-led loop

### Priority 1: Lift The Body Off The Surface Properly

Do not start by adding more leg complexity.

First fix the body support relationship.

What should happen:

- body should sit clearly above the surface
- leg roots should begin higher from the contact plane
- the body should feel suspended by the limbs
- abdomen and thorax should stop reading as if dragged across the terrain

Good concrete next actions:

- raise root offsets upward relative to the local body
- add a minimum body clearance value derived from the nearest planted feet
- optionally compute a "support plane" from planted feet and place the body above it

### Priority 2: Add A Stable Pole Vector / Bend Side Per Leg

Each leg needs a preferred bend side.

Without that, the solver can pick visually wrong solutions.

A practical approach:

- define a pole direction per leg in local space
- use left legs and right legs with mirrored pole directions
- bias the bend plane toward that pole direction every solve

That should stop:

- inverted knees
- elbows flipping unexpectedly
- folded-under poses

### Priority 3: Promote The Foot To The Real Driver

The foot target should more directly drive the chain.

That means:

- treat the claw tip as the true IK endpoint
- solve upper segments to reach the foot
- keep the claw aligned to the terrain normal

This is the best path toward the "robot arm" and "finger-like" movement style you described.

### Priority 4: Improve Lateral Placement

The legs still need more side-to-side life.

Even when planted, they should not all point mostly in the same direction.

Need:

- clearer lateral spread
- stronger per-leg root yaw bias
- slightly different target envelopes per leg row

This should make the silhouette feel more spider-like.

## Best Long-Term Direction

The current foundation is good enough that the next big improvement should not be another random tweak pass.

The best next version is:

1. keep per-leg planted targets
2. keep alternating gait groups
3. add pole-vector bend constraints
4. place body height from planted feet instead of a mostly fixed lift
5. solve claw as the true endpoint

That should give:

- correct knee direction
- less clipping
- stronger locomotion
- legs that feel like they support the body

## Inspiration Notes

The public README for `TheCymaera/minecraft-spider` reinforces the same overall direction:

- procedural walking rather than canned animation
- tunable gait behavior
- separate torso / leg logic
- experimental iteration through debugging tools

Useful source:

- https://github.com/TheCymaera/minecraft-spider

The biggest takeaway for this project is not to copy visuals directly, but to continue leaning into:

- procedural stepping
- independent leg control
- debug-driven iteration

## Current Summary

The spyder is no longer a static fake animation.

It now has:

- surface alignment
- body tilt sampling
- debug-follow targeting
- per-leg planted targets
- grouped stepping
- serial chain solving



The current blocker is now much more specific:

the body is too flat against the contacted surface, and that is poisoning the leg solve.

Fixing body clearance and bend direction is the next critical milestone.
