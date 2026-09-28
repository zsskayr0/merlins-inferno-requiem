package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.structure.DruidSanctuaryPiece;
import dev.zsskayr.merlins_inferno.worldgen.structure.AncientBattlefieldPiece;
import dev.zsskayr.merlins_inferno.worldgen.structure.RowanwoodTreePiece;
import dev.zsskayr.merlins_inferno.worldgen.structure.SacredChurchPiece;

/** Custom {@code StructurePieceType}s this mod adds. */
public final class ModStructurePieceTypes {
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Merlins_inferno.MODID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ROWANWOOD_TREE = STRUCTURE_PIECE_TYPES.register("rowanwood_tree",
            () -> (StructurePieceType.StructureTemplateType) RowanwoodTreePiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> DRUID_SANCTUARY = STRUCTURE_PIECE_TYPES.register("druid_sanctuary",
            () -> (StructurePieceType) (context, tag) -> new DruidSanctuaryPiece(tag));

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SACRED_CHURCH = STRUCTURE_PIECE_TYPES.register("sacred_church",
            () -> (StructurePieceType.StructureTemplateType) SacredChurchPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ANCIENT_BATTLEFIELD = STRUCTURE_PIECE_TYPES.register("ancient_battlefield",
            () -> (StructurePieceType.StructureTemplateType) AncientBattlefieldPiece::new);

    private ModStructurePieceTypes() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_PIECE_TYPES.register(modEventBus);
    }
}
