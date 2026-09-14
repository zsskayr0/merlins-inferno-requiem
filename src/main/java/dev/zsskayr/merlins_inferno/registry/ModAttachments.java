package dev.zsskayr.merlins_inferno.registry;

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

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
