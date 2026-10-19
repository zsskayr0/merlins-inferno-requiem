# Hell Forge block textures (placeholders)

All files here are temporary placeholders (a darkened Compressed Netherrack tile with a coloured marker).
Replace them **keeping the same file names** (or update the model JSONs in `models/block/`). They live directly in `textures/block/`.
Every texture is a **16x16 PNG**.

| File | Used by | Notes |
|---|---|---|
| `forge_core_demonic.png` | Demonic Core, inactive | All six faces. The idle (grey orb) look. |
| `forge_core_demonic_active.png` (+ `.mcmeta`) | Demonic Core, active | 16x128 strip, 8 frames stacked top to bottom, 3 ticks per frame: a fireball with a bright head circling inside the orb. Generated from the inactive texture; redo it if that one changes. |
| `hellforge_load_1.png` .. `hellforge_load_4.png` | Hell Forge Controller front | Animation frames. `load_1` is the idle (inactive) front; while the forge cooks it plays 1 > 2 > 3 > 4 > 3 > 2 and loops, blending smoothly between frames. |
| `hellforge_load_anim.png` (+ `.mcmeta`) | Hell Forge Controller front, active | 16x64 strip stacking load_1..load_4 top to bottom (5 ticks per frame, interpolated). **Rebuild it whenever a `hellforge_load_N.png` changes.** |
| `netherrack_compressed.png` | Hell Forge Controller | Sides, top and bottom (shared with Compressed Netherrack; there is no separate side file). |
| `forge_vault_input.png` | Hell Forge Vault, mode `input` | All six faces. Must read clearly as "input". |
| `forge_vault_output.png` | Hell Forge Vault, mode `output` | All six faces. Must read clearly as "output". |
| `forge_vault_fuel.png` | Hell Forge Vault, mode `fuel` | All six faces. Must read clearly as "fuel". |

Also needed (outside this folder): `textures/item/forge_wrench.png`, 16x16 (placeholder at the moment).
Compressed Netherrack (`block/netherrack_compressed.png`) is the real texture and stays as is.
