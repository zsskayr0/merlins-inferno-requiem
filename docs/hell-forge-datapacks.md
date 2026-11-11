# Hell Forge thermal datapacks

The Demonic Core processes registered blasting recipes, including recipes from other mods, plus the existing Hell Forge recipes. Add items with smelting recipes to the item tag `merlins_inferno:hell_forge_inputs` to enable them as well. A tag grants eligibility. The underlying recipe supplies the result.

Thermal profiles live in `data/<namespace>/hell_forge/<path>.json`. Temperature values are Kelvin. Convert Celsius by adding 273.15. Profiles reload with datapacks and synchronize to connected clients.

## Material profiles

A profile can select an item tag, a list of item IDs, a recipe ID, or a combination of these. All supplied selectors must match. Higher `priority` wins. Equal priorities use resource-ID order. Unmatched ingredients use `merlins_inferno:default_iron`.

```json
{
  "type": "material",
  "tag": "example:raw_materials/example_metal",
  "priority": 10,
  "min_temperature": 1333.15,
  "max_temperature": 1383.15,
  "heat_capacity": 12,
  "overheat_grace_ticks": 100,
  "overheat_progress_decay": 0.5,
  "critical_temperature": 1473.15,
  "critical_exposure_ticks": 400,
  "dross_chance": 0.10
}
```

Use `"items": ["example:raw_metal"]` for item IDs or `"recipe": "example:metal_from_blasting"` for a recipe selector.

At 20 game ticks per second, grace 100 means five seconds. Progress decay 0.5 removes half a processing tick per game tick after grace. Below the minimum, progress is preserved. Returning below the maximum drains accumulated overheat exposure one tick at a time. Critical exposure must be continuous. At its limit, one ingredient becomes Forged Scrap and that channel resets. A full scrap output delays conversion.

The default Dusk Dross chance is 10% per processed ingredient. Each Yield Catalyst adds five percentage points, capped at 100%. The same catalyst adds ten percentage points to the chance of doubling the primary result. Residual and primary outputs must have room before consuming an ingredient.

## Core and shell

The bundled Core profile is `merlins_inferno:core/demonic`. It names its block, heating operation, allowed fuel items, resting/minimum/maximum/efficient temperatures, heat capacity, stability amplitude and cooling rate. The current operation is `heating`.

The shell profile `merlins_inferno:shell/compressed_netherrack` sets maximum temperature, heat capacity per shell block and a stability multiplier. The geometric validator currently accepts the existing Compressed Netherrack frame and glass face materials.

The working limit is the lowest maximum of Core, shell and loaded fuel. The minimum is the greater Core/fuel minimum. The shared temperature automatically follows an eligible waiting channel. Channels with incompatible ranges take turns. A running channel retains priority until it completes or becomes blocked.

## Fuel

```json
{
  "type": "fuel",
  "item": "example:hot_fuel",
  "fuel_value": 1600,
  "min_temperature": 873.15,
  "max_temperature": 1923.15,
  "response_ticks": 200
}
```

Add the item ID to the Core profile's `allowed_fuels` as well. Fuel values range from 1 to 20000. Coal supplies 1600, Lava Bucket 20000. Processing costs 200 reserve units per ordinary completed operation, plus one reserve unit per tick for maintaining/heating the chamber. Preheating therefore reduces the number of finished operations per fuel item. `response_ticks` describes nominal time to achieve 90% of a requested temperature increase; structure mass and hot operating conditions increase that time.

Fuel types cannot mix. Dump clears the reserve while leaving residual heat. Lava pipes follow the same lock and convert each mB into 20 reserve units. Buckets have a dedicated return slot.

## Controls and persistence

Click a Vault in the rotatable 3D view, then use its mode, automatic-transfer and channel buttons. Output Vaults also expose the two residue outputs. Left-click a Vault with the Forge Wrench to open the console. Right-click cycles its mode; sneak-right-click toggles automatic transfer.

The temperature-unit button saves Celsius, Fahrenheit or Kelvin in the client configuration for all forges. Recipe displays cycle the three units every two seconds.

Inventory, shared temperature, reserve type, per-channel progress and exposure, and Vault channel masks persist with the Controller. Original three-slot inventories migrate into channel one, the fuel slot and its output. Legacy untyped fuel becomes Lava-equivalent energy. Channels disabled by a smaller structure or extra catalysts retain their items for manual retrieval.

Shift-right-click a Vault (with or without a wrench) to toggle its individual automatic transfer. Output mode pushes assigned outputs, Dusk Dross and Forged Scrap into adjacent inventories. Input/Fuel modes pull supplies. The setting persists in that Vault block state and is shared with its GUI Auto toggle.
