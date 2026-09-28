"""Generates the three chest loot tables of the Ancient Battlefield (Andras' keep).

Run from the repo root:  python tools/gen_andras_loot.py
Edit the tables below and re-run - the JSON files are the output, this is the source of truth for the numbers.
"""
import json, os

OUT = "src/main/resources/data/merlins_inferno/loot_table/chests"


def item(name, w, lo=None, hi=None, enchant=None):
    e = {"type": "minecraft:item", "name": name, "weight": w}
    fn = []
    if lo is not None:
        fn.append({"function": "minecraft:set_count",
                   "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi if hi is not None else lo)}})
    if enchant:
        fn.append({"function": "minecraft:enchant_with_levels",
                   "levels": {"type": "minecraft:uniform", "min": float(enchant[0]), "max": float(enchant[1])},
                   "options": "#minecraft:on_random_loot"})
    if fn:
        e["functions"] = fn
    return e


def pool(entries, rolls, chance=None):
    p = {"rolls": rolls if isinstance(rolls, float) else {"type": "minecraft:uniform", "min": float(rolls[0]), "max": float(rolls[1])},
         "bonus_rolls": 0.0, "entries": entries}
    if chance is not None:
        p["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    return p


def table(pools):
    return {"type": "minecraft:chest", "pools": pools}


def write(name, t):
    with open(os.path.join(OUT, name + ".json"), "w", newline="\n") as f:
        json.dump(t, f, indent=2)
        f.write("\n")


M = "minecraft:"
I = "merlins_inferno:"
ESSENCE = [item(I + "infernal_essence", 1, 1)]

# --- The ~10 chests in the rubble piles: "fortress"-grade, a little demon flavour, no guaranteed valuable. ---
write("andras_ruins", table([
    pool([
        item(M + "nether_wart", 8, 3, 7), item(M + "gold_ingot", 8, 1, 4), item(M + "gold_nugget", 8, 5, 12),
        item(M + "iron_ingot", 7, 2, 5), item(M + "quartz", 5, 4, 10), item(M + "obsidian", 5, 2, 4),
        item(M + "crying_obsidian", 3, 1, 3), item(M + "flint_and_steel", 3), item(M + "golden_sword", 3),
        item(M + "golden_helmet", 2), item(M + "saddle", 2), item(M + "diamond", 2, 1),
        item(I + "demon_blood", 4, 1, 2), item(I + "withered_bone", 4, 1, 3), item(I + "infernal_sinew", 2, 1),
        item(M + "lava_bucket", 1),
    ], (2.0, 4.0)),
]))

# --- The single chest behind the sword: a step above the ruins, one guaranteed valuable. ---
write("andras_sword_chest", table([
    pool([
        item(I + "demon_blood", 10, 3, 5), item(I + "withered_bone", 8, 3, 5), item(I + "infernal_sinew", 8, 1, 3),
        item(M + "gold_ingot", 6, 8, 16), item(M + "crying_obsidian", 5, 4, 8), item(M + "golden_apple", 5, 1, 2),
        item(M + "diamond", 6, 1, 3), item(M + "nether_wart", 4, 4, 8),
    ], (3.0, 4.0)),
    pool([
        item(M + "netherite_scrap", 6, 1), item(M + "ancient_debris", 5, 1, 2), item(M + "diamond", 8, 2, 4),
        item(M + "diamond_sword", 2, enchant=(20, 30)), item(M + "diamond_pickaxe", 2, enchant=(20, 30)),
        item(M + "netherite_upgrade_smithing_template", 2),
    ], 1.0),
    pool(ESSENCE, 1.0, 0.25),
]))

# --- Each half of the 8 double chests under the dome. Both halves roll it, so one double chest = 2x this. ---
write("andras_vault", table([
    pool([
        item(I + "demon_blood", 10, 2, 4), item(I + "withered_bone", 8, 2, 4), item(I + "infernal_sinew", 6, 1, 2),
        item(M + "gold_ingot", 9, 6, 12), item(M + "crying_obsidian", 6, 3, 6), item(M + "nether_wart", 5, 4, 8),
        item(M + "wither_skeleton_skull", 2, 1),
    ], (2.0, 3.0)),
    pool([
        item(M + "netherite_scrap", 6, 1), item(M + "ancient_debris", 6, 1, 2), item(M + "diamond", 8, 2, 4),
        item(M + "golden_apple", 4, 1), item(M + "enchanted_golden_apple", 1, 1), item(M + "netherite_ingot", 1, 1),
        item(M + "diamond_sword", 2, enchant=(20, 30)), item(M + "diamond_chestplate", 2, enchant=(20, 30)),
        item(M + "netherite_upgrade_smithing_template", 2),
    ], 1.0),
    pool(ESSENCE, 1.0, 0.12),
]))
print("ok")
