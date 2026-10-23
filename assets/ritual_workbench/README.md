# Ritual Workbench — source asset

## Selected source

- Origin: `C:\Users\roque\Downloads\free-voxel-medieval-starter-pack.zip`
- Original archive preserved unchanged.
- Source model: `source/Free_Medieval_Starter_Pack.glb`
- Selected object inside the GLB: node `table`, mesh `table.001`
- Selected texture: `textures/tavern_table_1.png` (embedded GLB material is named `tavern_table.png`)
- GLB generator metadata: Khronos glTF Blender I/O v5.1.19, glTF 2.0

## Why this base

The package contains one table, one wooden chair, and one small crate. The table is a separate mesh with its own tavern-table material, so it is the appropriate editable base for the Ritual Workbench. It has enough surface area to be widened to roughly two blocks and can accept ritual additions such as runes, supports, and darker altar details.

No code or Minecraft model registration has been changed yet.

## Adaptation pass

- Preserved the downloaded source as `source/free_medieval_starter_pack.original.glb` before editing.
- Increased the workbench scale to read as a sturdier, wider approximately two-block workstation while retaining the voxel geometry.
- Added a centered raised red/crimson voxel inlay named `Central Ritual Inlay`, with a mildly emissive ritual-core material.
- Kept the existing front-facing composition and aligned the new center interaction detail to the workbench's front-readable orientation.
- Final GLB was structurally re-read as glTF 2.0 after editing; it contains the original three meshes plus `ritual_core`.
- Created editable Blockbench project: `source/ritual_workbench.bbmodel`.
- The `.bbmodel` preserves the robust voxel silhouette, centered `Central Ritual Inlay`, crimson material slot, and front-oriented support details. It is a best-effort cube-based Blockbench conversion rather than a lossless GLB mesh import; the original/adapted GLBs remain preserved for reference.

## Texture files included

- `tavern_table_1.png` — table wood texture copied from the starter pack.
- `ritual_core_crimson.png` — local crimson texture for the central ritual inlay and rune supports.
- The `.bbmodel` texture paths are relative to the model folder.

The deeper texture display bug was an incompatibility between 0–16 cube UVs and the original 2048x2048 image, compounded by non-embedded texture sources. The Blockbench package now uses 32x32 working copies with explicit 0–16 UVs and embeds both PNG sources directly in the `.bbmodel`. The original 2048x2048 texture remains available as `tavern_table_1.png`. The prior broken package was backed up under `backup_before_texture_fix/` and `backup_before_texture_fix_2/`.

The geometry was subsequently compacted into one connected 16-unit workbench layout: legs end at the body, the top rests at y=10–12, the ritual inlay is centered at y=11.8–12.2, and the rune supports are mirrored at the front. Every cube now has a centered origin and zero rotation. The pre-geometry version is preserved under `backup_before_geometry_fix/`.

## GLB comparison / restoration

The adapted GLB contains four visible scene components: `Ritual Workbench` (`table.001`), `Central Ritual Inlay` (`ritual_core`), `WOODEN CHAIR` (`seat.001`), and `small_crate`. The earlier `.bbmodel` had only the workbench cuboids and ritual detail, so the chair and crate were lost; they are now restored as connected editable Blockbench components with their original `wooden_chair_0.png` and `small_crate_2.png` textures. The table uses the 32x32 Blockbench working atlas with embedded source, while the original 2048x2048 `tavern_table_1.png` remains preserved separately.

## License / provenance

The ZIP contains no license file or explicit license metadata. Treat the asset as provenance-recorded but not cleared for redistribution until the package author/source license is confirmed. This working copy is retained for project evaluation and adaptation only.

## Checksums of the working copy

- `source/free_medieval_starter_pack.glb`: `A169690C5C6CE63DAABD3EBD258140A58E28009F256CDD04FA27FC31D727E073`
- `textures/tavern_table_1.png`: `83CD233FE5372392605E398342212CFA2535B1EFA2F25CE3F2F364F11B279547`
