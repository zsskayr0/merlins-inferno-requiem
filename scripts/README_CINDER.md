# Cinder asset conversion

The editable `cinder.bbmodel` is kept outside the repository in `../src/Models/cinder/`,
as required by the repository's working-art policy. The runtime assets are committed.

Run `python scripts/bake_cinder.py [path/to/cinder.bbmodel]` with Python and NumPy.
Without an argument, the script reads the external location above. It writes the
GeckoLib bone hierarchy, triangle meshes, shared pose samples, animation clips and
embedded base texture into `src/main/resources/assets/merlins_inferno/`.

The shared rig is consumed by the client renderer and server multipart hitboxes.
The current input uses Blockbench 5, 230 meshes, 10 clips and 32 body sections.
The crests are edited in the source model; conversion never raises the swimming body.

Validation: `gradlew verifyCinder` checks heat/regeneration cadence; `gradlew
runGameTestServer` covers immersion, cooling, multipart damage and grounded return.
Visual appearance and terrain-dependent combat still require manual game testing.
