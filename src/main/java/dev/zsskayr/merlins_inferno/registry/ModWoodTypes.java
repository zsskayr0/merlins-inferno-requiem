package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * The {@link BlockSetType}/{@link WoodType} pair behind Ashwood's doors, trapdoors, buttons,
 * pressure plates, fence gates and signs. Both live in vanilla's own (non-deferred) registries,
 * so they're registered eagerly in a static initializer - it has to have run before any of those
 * blocks is constructed, which {@link ModBlocks}' field initializers guarantee by referencing
 * these fields.
 * <p>
 * The name carries the mod's namespace on purpose: {@code Sheets} builds the sign textures'
 * location from it ({@code textures/entity/signs/ashwood.png} and
 * {@code textures/entity/signs/hanging/ashwood.png} under this mod's own assets) - a bare
 * "ashwood" would look under {@code minecraft} instead.
 */
public final class ModWoodTypes {
    public static final BlockSetType ASHWOOD_SET = BlockSetType.register(new BlockSetType(Merlins_inferno.MODID + ":ashwood"));
    public static final WoodType ASHWOOD = WoodType.register(new WoodType(Merlins_inferno.MODID + ":ashwood", ASHWOOD_SET));

    private ModWoodTypes() {
    }
}
