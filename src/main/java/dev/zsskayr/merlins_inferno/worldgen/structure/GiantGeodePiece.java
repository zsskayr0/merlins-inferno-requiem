package dev.zsskayr.merlins_inferno.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * The Giant Geode's one piece: the 31-cube template, pasted as it is. The template holds no air outside the geode's own
 * sphere, so only the shell and the hollow replace the terrain; the loot chest at the centre carries its loot table in the template.
 */
public class GiantGeodePiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "giant_geode");

    public GiantGeodePiece(StructureTemplateManager templateManager, BlockPos pos) {
        super(ModStructurePieceTypes.GIANT_GEODE.get(), 0, templateManager, TEMPLATE, TEMPLATE.toString(), makeSettings(), pos);
    }

    public GiantGeodePiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(ModStructurePieceTypes.GIANT_GEODE.get(), tag, templateManager, location -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(true);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // No data markers in this template.
    }
}
