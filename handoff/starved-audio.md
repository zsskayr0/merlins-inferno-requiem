# Handoff: Starved audio (Merlin's Inferno)

## Context
Mod: Minecraft NeoForge 1.21.x, mod id `merlins_inferno`. The **Starved** is a new Nether miniboss (between the Imp and the final boss). It has no custom audio yet: every sound is a vanilla placeholder (Ravager/Hoglin), all in [StarvedEntity.java](../src/main/java/dev/zsskayr/merlins_inferno/entity/StarvedEntity.java). The mod has **no custom-sound infrastructure yet** (no `sounds.json`, no `ModSounds` registry, no `sounds/` folder), so the first custom sound needs it created (see "Integration").

## Creature
A large, four-legged, animalistic infernal demon, a wendigo in the vein of Bloodborne / Elden Ring beasts. Cracked charcoal skin with glowing orange/red fissures, claws, no wings. Never completed forming: it is stuck in a hunger that cannot be sated. It is meant to inspire dread the moment the player gets near.

Mood: wet, guttural, starved, rasping. Not a roar of triumph; a roar of *need*. Low frequencies, breathy rattle, throat clicks, cracking/creaking of dry charred hide, occasional almost-human crying undertones in the ambient (uncanny, not cartoonish). It is the opposite of the Imp (small, shrill, cackling), so keep it low, heavy and slow.

## Mechanic that drives the audio
- **Hunger** (0-10 stacks): grows every 4s while it hunts without landing a hit; body cracks glow brighter. Landing a hit resets it to 0 and heals it. Sounds tied to hunger should escalate: idle/step/breath gets faster, harsher and hungrier as stacks rise.
- **Charge** (from 4 stacks): 1.25s telegraphed windup (roars in place), then a straight-line dash. The windup roar must be an unmistakable "dodge now" cue.
- Never flees, keeps tracking the player even out of sight.

## Sound list
Priority: P0 = needed for it to feel right, P1 = nice to have.

| Event key (proposed) | Trigger in code | Now (placeholder) | Notes | Pri |
|---|---|---|---|---|
| `entity.starved.ambient` | idle ambient (default interval) | `RAVAGER_AMBIENT` | Slow labored breathing/wheeze with a hungry gurgle. 2-3 variants. | P0 |
| `entity.starved.notice` | first time it acquires a target | `RAVAGER_ROAR` pitch 0.6 | The "it has seen you" cue. Long, low, ~2s, must scare. | P0 |
| `entity.starved.hurt` | takes damage | `RAVAGER_HURT` | Short pained rasp, 2-3 variants. | P0 |
| `entity.starved.death` | dies | `RAVAGER_DEATH` | Long collapsing wail fading to a rattle. | P0 |
| `entity.starved.step` | each step | `RAVAGER_STEP` vol 0.15 | Heavy padded claw steps on stone, quadruped rhythm, crackle of charred hide. 4 variants. | P0 |
| `entity.starved.hunger` | +1 hunger stack (every ~4s while hunting) | `HOGLIN_ANGRY` pitch 0.6 | Stomach/throat growl. Plays up to 10 times a fight, so it needs several variants; pitch is scaled by stack in code if wanted. | P0 |
| `entity.starved.attack` | claw swing (`doHurtTarget`) | none (vanilla hit sound only) | Fast claw whoosh plus a wet snarl. | P0 |
| `entity.starved.feed` | its hit lands (feeds) | `GENERIC_EAT` pitch 0.5 | Grotesque chew/tear/gulp. Should feel like a reward for it and a punishment for the player. | P0 |
| `entity.starved.charge_windup` | charge telegraph (25 ticks) | `RAVAGER_ROAR` pitch 0.6 | Distinct from `notice`: shorter (~1.2s), building, ends right as the dash starts. | P0 |
| `entity.starved.charge` | dash (18 ticks) | none | Rapid heavy galloping/scrabbling, looped or one long clip. | P1 |
| `entity.starved.charge_impact` | dash connects | none | Heavy body hit plus a bark. | P1 |
| `entity.starved.ambient_hungry` | ambient when hunger >= 6 | none | Ambient variant that is frantic and near-panting. | P1 |

## Technical specs
- Format: `.ogg` (Vorbis), **mono** for anything positional (all of the above), 44.1 kHz.
- Keep clips short except `notice`, `death`, `charge_windup`; normalize to about -3 dBFS peak; vanilla mobs sit around that loudness.
- Deliver variants as `name_1.ogg`, `name_2.ogg`, ... 
- Final path: `src/main/resources/assets/merlins_inferno/sounds/entity/starved/<name>.ogg`.

## Integration (code side)
Nothing exists yet, so on delivery these are needed (the developer or I can do it):
1. `ModSounds` registry (`DeferredRegister<SoundEvent>` on `Registries.SOUND_EVENT`) with a `SoundEvent.createVariableRangeEvent` per key above.
2. `assets/merlins_inferno/sounds.json` mapping each key to its files (with `"subtitle"` entries) and lang keys for subtitles in `en_us.json` / `pt_br.json`.
3. Replace the vanilla `SoundEvents.*` in `StarvedEntity` (`getAmbientSound`, `getHurtSound`, `getDeathSound`, `playStepSound`, `roar`, `aiStep` hunger growl, `feed`) and add attack / charge sounds in `doHurtTarget` and `StarvedChargeGoal`.

## Reference
Bloodborne: Cleric Beast, Ashen Blood beasts. Elden Ring: Godskin/Beast-type enemies. Skyrim's Frost Troll for the breath and weight. Use only as a tonal reference, not for sampling.

Please return the `.ogg` files with the names above (or tell me which ones you changed) so the wiring is straightforward.
