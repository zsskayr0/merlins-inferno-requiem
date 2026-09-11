package dev.zsskayr.merlins_inferno.attachment;

/**
 * Per-player, in-memory-only (not saved/synced) counter of how many continuous ticks Lyrium Bruto
 * has been held in a hand - see {@code registry.ModAttachments} and
 * {@code event.SanctifiedTickHandler}. Resets to 0 the instant the item leaves both hands; there's
 * nothing here worth persisting across a logout/restart.
 */
public class SanctifiedProgress {
    public int ticksHeld;
}
