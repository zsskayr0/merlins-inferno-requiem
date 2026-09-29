package dev.zsskayr.merlins_inferno.registry;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.SanctifiedProgress;

/**
 * Central registry for every NeoForge data attachment this mod adds. First use of the attachment
 * system in this mod - previously nothing needed per-entity custom state.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Merlins_inferno.MODID);

    /** See {@link SanctifiedProgress} - deliberately not serialized, this is throwaway state. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SanctifiedProgress>> SANCTIFIED_PROGRESS =
            ATTACHMENT_TYPES.register("sanctified_progress", () -> AttachmentType.builder(SanctifiedProgress::new).build());

    /** The player's Circle (1 or 2) - see {@link dev.zsskayr.merlins_inferno.attachment.ProgressionHelper}. Kept through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> CIRCLE =
            ATTACHMENT_TYPES.register("circle", () -> AttachmentType.builder(() -> 1).serialize(Codec.INT).copyOnDeath().build());

    /** Set when the player died holding a Pandora Box: it is handed back on respawn (see PandoraHandler). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> RETURN_PANDORA_BOX =
            ATTACHMENT_TYPES.register("return_pandora_box", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** Set once the player has ever held a Rowanwood Scrap: it opens the Druid's tool and enchantment trades. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> ROWANWOOD_UNLOCKED =
            ATTACHMENT_TYPES.register("rowanwood_unlocked", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** Where a player stood when they last entered Oblivion; the hub's portal brings them back there. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.minecraft.core.GlobalPos>> OBLIVION_ORIGIN =
            ATTACHMENT_TYPES.register("oblivion_origin", () -> AttachmentType.builder(() -> net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.OVERWORLD, net.minecraft.core.BlockPos.ZERO))
                    .serialize(net.minecraft.core.GlobalPos.CODEC).copyOnDeath().build());

    /** Level attachment: the world day Ostara was last called to a Rowanwood tree (see OstaraSpawnHandler). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> OSTARA_LAST_DAY =
            ATTACHMENT_TYPES.register("ostara_last_day", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
