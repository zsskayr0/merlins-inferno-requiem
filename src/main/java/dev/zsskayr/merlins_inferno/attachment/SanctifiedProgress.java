package dev.zsskayr.merlins_inferno.attachment;

/**
 * Per-player, in-memory-only (not saved/synced) Sanctified bookkeeping - see
 * {@code registry.ModAttachments} and {@code event.SanctifiedTickHandler}. Nothing here is worth
 * persisting across a logout/restart; it all resets to 0/false the instant contact is lost anyway.
 */
public class SanctifiedProgress {
    /** How many continuous ticks Raw Lyrium has been held in a hand - drives the +1-level-per-minute climb. */
    public int ticksHeld;

    /** Whether Sanctified is currently actually applied - see the two counters below for why this lags contact. */
    public boolean active;

    /** Ticks of continuous contact (anywhere in the inventory) since the last time Sanctified was off - the "turning on" delay. */
    public int ticksSinceContactGained;

    /** Ticks of continuous NO contact since the last time Sanctified was on - the "turning off" grace period. */
    public int ticksSinceContactLost;
}
