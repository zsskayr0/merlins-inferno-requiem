package dev.zsskayr.merlins_inferno.entity.cinder;

import dev.zsskayr.merlins_inferno.entity.CinderEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

public final class CinderPart extends PartEntity<CinderEntity> {
    private final EntityDimensions dimensions;
    public CinderPart(CinderEntity parent, float diameter, float height) {
        super(parent);
        dimensions = EntityDimensions.scalable(diameter, height);
        refreshDimensions();
    }
    @Override public EntityDimensions getDimensions(Pose pose) { return dimensions == null ? super.getDimensions(pose) : dimensions; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() { return getParent().isAlive(); }
    @Override public boolean hurt(DamageSource source, float damage) { return !isInvulnerableTo(source) && getParent().hurt(source, damage); }
    @Override public boolean is(Entity entity) { return this == entity || getParent() == entity; }
    @Override public boolean shouldBeSaved() { return false; }
}
