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

    /** Set when the player died holding a Pandora Box: it is handed back on respawn (see PandoraHandler). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> RETURN_PANDORA_BOX =
            ATTACHMENT_TYPES.register("return_pandora_box", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** Set once the player has ever held a Rowanwood Scrap: it opens the Druid's tool and enchantment trades. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> ROWANWOOD_UNLOCKED =
            ATTACHMENT_TYPES.register("rowanwood_unlocked", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /** Where a player stood when they last entered Oblivion; the arrival portal brings them back there. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.minecraft.core.GlobalPos>> OBLIVION_ORIGIN =
            ATTACHMENT_TYPES.register("oblivion_origin", () -> AttachmentType.builder(() -> net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.OVERWORLD, net.minecraft.core.BlockPos.ZERO))
                    .serialize(net.minecraft.core.GlobalPos.CODEC).copyOnDeath().build());

    /** Where a player stood when they last entered El'ysium; the arrival portal brings them back there. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.minecraft.core.GlobalPos>> ELYSIUM_ORIGIN =
            ATTACHMENT_TYPES.register("elysium_origin", () -> AttachmentType.builder(() -> net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.OVERWORLD, net.minecraft.core.BlockPos.ZERO))
                    .serialize(net.minecraft.core.GlobalPos.CODEC).copyOnDeath().build());

    /** The spot in El'ysium a player last arrived at: only the portal there leads back to {@link #ELYSIUM_ORIGIN}; any other opens at its own coordinates. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.minecraft.core.BlockPos>> ELYSIUM_GATE =
            ATTACHMENT_TYPES.register("elysium_gate", () -> AttachmentType.builder(() -> net.minecraft.core.BlockPos.ZERO)
                    .serialize(net.minecraft.core.BlockPos.CODEC).copyOnDeath().build());

    /** Level attachment: the world day Ostara was last called to a Rowanwood tree (see OstaraSpawnHandler). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> OSTARA_LAST_DAY =
            ATTACHMENT_TYPES.register("ostara_last_day", () -> AttachmentType.builder(() -> -1L).serialize(Codec.LONG).build());

    /** Ticks of wings boost a player still has left (see WingsItem). Throwaway state: not saved. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> WINGS_BOOST_TICKS =
            ATTACHMENT_TYPES.register("wings_boost_ticks", () -> AttachmentType.builder(() -> 0).build());
    /** Game time at which the wings' cooldown ends, kept so it can run faster on the ground (see WingsItem). Not saved. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> WINGS_READY_AT =
            ATTACHMENT_TYPES.register("wings_ready_at", () -> AttachmentType.builder(() -> 0L).build());

    /** Item id of the Mark the player bears, "" for none; the source of truth for Marks (see MarkHelper). Soulbound: kept on death. Synced, so the client can draw what a Mark does to the HUD. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> EQUIPPED_MARK =
            ATTACHMENT_TYPES.register("equipped_mark", () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).copyOnDeath()
                    .sync(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8).build());

    /** Mark of Cain: the game time by which the bearer must have killed something (0 = not started; see MarkOfCainHandler). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> CAIN_KILL_DEADLINE =
            ATTACHMENT_TYPES.register("cain_kill_deadline", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());
    /** Mark of Cain: the game time of the next roll for "Mark of the Murder". */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> CAIN_MURDER_AT =
            ATTACHMENT_TYPES.register("cain_murder_at", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).build());
    /** Mark of Cain: whether "Mark of the Murder" is waiting for the next blow its bearer takes. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> CAIN_MURDER_ARMED =
            ATTACHMENT_TYPES.register("cain_murder_armed", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    /** Dark Coven: game time up to which a held right-click keeps the coven going; -1 when it is not in "hold" mode. Not saved. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> COVEN_HOLD_UNTIL =
            ATTACHMENT_TYPES.register("coven_hold_until", () -> AttachmentType.builder(() -> -1L).build());
    /** Dark Coven: game time of the last right-click, to tell a fresh press from the repeats of a held button. Not saved. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> COVEN_LAST_USE =
            ATTACHMENT_TYPES.register("coven_last_use", () -> AttachmentType.builder(() -> -100L).build());

    /** Dark Coven swords a player died holding: handed back on respawn (see DarkCovenKeepHandler). Throwaway, not saved. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<java.util.List<net.minecraft.world.item.ItemStack>>> KEPT_COVENS =
            ATTACHMENT_TYPES.register("kept_covens", () -> AttachmentType.builder(() -> (java.util.List<net.minecraft.world.item.ItemStack>) new java.util.ArrayList<net.minecraft.world.item.ItemStack>()).build());

    /** Chunk attachment: set once the Arganite vein retrogen has run on the chunk (see ArganiteRetrogen). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> ARGANITE_VEINS_DONE =
            ATTACHMENT_TYPES.register("arganite_veins_done", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
