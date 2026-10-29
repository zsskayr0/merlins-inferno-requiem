package dev.zsskayr.merlins_inferno.attachment;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge event bus, server side, after the world's Circle changed (up through the ritual, either way through debug). */
public class CircleChangedEvent extends Event {
    private final MinecraftServer server;
    private final int oldCircle;
    private final int newCircle;

    public CircleChangedEvent(MinecraftServer server, int oldCircle, int newCircle) {
        this.server = server;
        this.oldCircle = oldCircle;
        this.newCircle = newCircle;
    }

    public MinecraftServer getServer() { return server; }
    public int getOldCircle() { return oldCircle; }
    public int getNewCircle() { return newCircle; }
    public boolean isIncrease() { return newCircle > oldCircle; }
}
