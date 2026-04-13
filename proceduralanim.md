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
