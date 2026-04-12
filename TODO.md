# LIST OF STUFF I WANNA ADD

* lvl 324 entities accurrate with the wiki 
* custom spider entity procedurally animated
* sort of siren head or scp 094 stle humanoid figures 


## Personal Notes

SAM ALWAYS REMEMBER TO SET THE CORRECT JDK 
> $env:JAVA_HOME="C:\Program Files\Java\jdk-17"
> $env:Path="C:\Program Files\Java\jdk-17\bin;$env:Path"
> spb-revamped:level959/start

## Level 959 Spyder Concept

Goal:
Create a procedural ambient "spyder" for level 959 that makes the player feel watched without becoming a normal combat mob or a fully readable mechanic.

Core rules:
* make it a peripheral horror presence, not a standard enemy
* never rely on rare close passes
* never freeze when looked at, because that teaches the player the rule too quickly
* keep it mostly on ceilings, upper walls, doorway tops, and corners
* let it appear, traverse, and vanish before the player can fully study it
* persistent in feeling, not persistent in simulation

Best architecture:
* use one invisible controller entity
* attach a visual rig built from display entities
* controller handles spawn, despawn, path choice, timing, and sound cues
* display entities handle the body, abdomen, and legs

Initial states:
* HIDDEN
* ENTERING
* TRAVERSING
* EXITING
* DESPAWNED

Good behaviors:
* ceiling_scuttle
* wall_cross
* doorway_header_cross
* gap_traverse
* partial_silhouette
* vanish_on_occlusion

Bad behaviors to avoid:
* standing still in plain sight for too long
* obvious looping patrol routes
* charge attacks
* freeze on direct sight
* constant close-range exposure

Surface rules for first version:
* support ceiling traversal first
* support upper wall traversal second
* support doorway top transitions
* skip full arbitrary surface traversal until later

Visual rig plan:
* 1 main body display
* 1 abdomen / rear body display
* fake 4 logical leg pairs first instead of full realistic 8-leg IK
* 2-3 segment displays per logical leg
* use staggered stepping and subtle body bobbing

Movement math:
* treat movement as surface-bound, not standard mob walking
* each update should use:
  * position
  * surface normal
  * forward direction projected onto that surface
* align the body to floor / wall / ceiling normals
* replant feet when stretched too far from the body

Spawn / sighting rules:
* usually 10-24 blocks away
* sometimes 24-36 in long hallways
* avoid under 8 blocks
* prefer upper third of the view space
* prefer hallway seams, corners, upper wall bands, and doorway headers
* only trigger when the player is not already overloaded by another major event

Interaction with level 959 atmosphere:
* tie spawn chance up slightly during flicker events
* use subtle dust / stagnant air fog in level 959 to soften distant silhouettes
* let flicker hide route transitions so the spyder can "jump" farther without feeling fake

Implementation order:
* add subtle level 959 fog / dust haze first
* build a SpyderDirector system for timing and route choice
* build an invisible SpyderControllerEntity
* add a simple display-entity rig
* get ceiling-only traversal working
* add wall / doorway transitions
* add sparse skitter / scrape / dry creak sounds

Important design note:
The spyder should feel like something that lives in the architecture itself, not like a mob that happens to be inside the building.
