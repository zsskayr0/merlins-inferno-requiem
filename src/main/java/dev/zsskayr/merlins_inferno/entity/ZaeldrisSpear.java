package dev.zsskayr.merlins_inferno.entity;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.effect.GodTouchEffect;
import dev.zsskayr.merlins_inferno.registry.ModEffects;

/**
 * One of Zaeldris' spears of light (see {@code item.ZaeldrisItem}). It is summoned above and behind the wielder and hangs there
 * for a few ticks, riding along with them and turned toward its foe, like a truth-seeking orb; then it is loosed. It flies at {@value #SPEED} blocks a tick, bending toward the
 * foe it was loosed at, and lodges in the first living thing it meets that is not the wielder's. A foe holds at most
 * {@value #MAX_STUCK} at once: the next one to lodge sets off the oldest. Each lodged spear strikes once a second and keeps God
 * Touch on its host; after {@value #STUCK_TICKS} ticks, or when the host dies, it bursts. A spear that strikes a block sticks in
 * it like an arrow, still shining, and bursts after the same {@value #STUCK_TICKS} ticks (or at once if the block goes); one
 * that flies {@value #FLIGHT_TICKS} ticks without finding anything bursts in the air. The burst hurts every foe within
 * {@value #BURST_RADIUS} blocks and breaks nothing.
 * <p>
 * Every strike is the wielder's own: a share of the attack damage read when the spear was loosed (Strength, the lance's
 * attributes, any affix or gear bonus), raised by the lance's enchantments (Sharpness, Smite, Holy...) and followed by their
 * on-hit effects (Fire Aspect and the like). The damage names the wielder as the attacker and the lance as the weapon, so kills,
 * loot, Looting and experience are the player's, as if hit by hand.
 * <p>
 * The spear lights the world: it keeps an invisible light block where it is (only ever in air or water, put back as it was the
 * moment the spear moves on or goes). It is never saved with the chunk, so no light is ever left behind by a spear that was.
 * Its damage type bypasses the hurt cooldown, so seven spears, the burst and God Touch all land on the same foe. It is deliberately
 * not a projectile damage type ({@code #minecraft:is_projectile}), so Endermen take it instead of teleporting away. End Crystals
 * are hit too (by the spear and its burst), and so is the Ender Dragon, through any of its body parts.
 */
public class ZaeldrisSpear extends Projectile {
    public static final float SPEED = 1.8F;
    /** Share of the wielder's attack each lodged spear deals a second: 25 with the bare lance's 120. */
    public static final float STUCK_SHARE = 25.0F / 120.0F;
    /** Share of the wielder's attack the burst deals: 30 with the bare lance's 120. */
    public static final float BURST_SHARE = 30.0F / 120.0F;
    public static final double BURST_RADIUS = 3.0;
    public static final int MAX_STUCK = 7;
    public static final int STUCK_TICKS = 100;
    public static final int FLIGHT_TICKS = 60;
    /** How hard a spear bends toward its foe each tick (0 = straight, 1 = turns at once). */
    private static final double HOMING = 0.22;
    private static final int LIGHT_LEVEL = 14;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "zaeldris"));

    private static final EntityDataAccessor<Integer> STUCK_TO = SynchedEntityData.defineId(ZaeldrisSpear.class, EntityDataSerializers.INT);
    /** Where the spear sits in its host, in the host's own frame (turned with its body). */
    private static final EntityDataAccessor<Vector3f> STUCK_OFFSET = SynchedEntityData.defineId(ZaeldrisSpear.class, EntityDataSerializers.VECTOR3);
    /** The spear's yaw minus the host's body yaw at the moment it lodged. */
    private static final EntityDataAccessor<Float> STUCK_YAW = SynchedEntityData.defineId(ZaeldrisSpear.class, EntityDataSerializers.FLOAT);
    /** How many ticks the spear hangs by its wielder before it is loosed. */
    /** Stuck in a block, like an arrow. */
    private static final EntityDataAccessor<Boolean> IN_BLOCK = SynchedEntityData.defineId(ZaeldrisSpear.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HOVER_TICKS = SynchedEntityData.defineId(ZaeldrisSpear.class, EntityDataSerializers.INT);
    /** Where it hangs, from the wielder's feet (world frame). */
    private static final EntityDataAccessor<Vector3f> HOVER_OFFSET = SynchedEntityData.defineId(ZaeldrisSpear.class, EntityDataSerializers.VECTOR3);

    private Entity homingTarget;
    private ItemStack weapon = ItemStack.EMPTY;
    private float attackDamage = 120.0F;
    private int stuckAge;
    private BlockPos stuckBlock;
    private BlockPos lightPos;
    private boolean lightWaterlogged;

    public ZaeldrisSpear(EntityType<? extends ZaeldrisSpear> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** The lance as it was when this spear was loosed, and the wielder's attack damage at that moment. */
    public void arm(ItemStack weapon, float attackDamage) {
        this.weapon = weapon;
        this.attackDamage = attackDamage;
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.weapon;
    }

    /** Hangs {@code ticks} ticks at {@code offset} from the wielder's feet before it is loosed. Call before it is added. */
    public void summon(Vec3 offset, int ticks) {
        this.entityData.set(HOVER_OFFSET, new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
        this.entityData.set(HOVER_TICKS, ticks);
    }

    public void setHomingTarget(Entity target) {
        this.homingTarget = target;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STUCK_TO, -1);
        builder.define(STUCK_OFFSET, new Vector3f());
        builder.define(STUCK_YAW, 0.0F);
        builder.define(IN_BLOCK, false);
        builder.define(HOVER_TICKS, 0);
        builder.define(HOVER_OFFSET, new Vector3f());
    }

    public boolean isStuck() {
        return this.entityData.get(STUCK_TO) >= 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount <= this.entityData.get(HOVER_TICKS)) {
            this.tickHover();
        } else if (this.isStuck()) {
            this.tickStuck();
        } else if (this.entityData.get(IN_BLOCK)) {
            this.tickInBlock();
        } else {
            this.tickFlight();
        }
        if (!this.isRemoved()) {
            this.updateLight();
        }
    }

    private void tickHover() {
        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) {
            if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }
        Vector3f offset = this.entityData.get(HOVER_OFFSET);
        this.setPos(owner.position().add(offset.x, offset.y, offset.z));
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.04, (this.random.nextDouble() - 0.5) * 0.04, (this.random.nextDouble() - 0.5) * 0.04);
            }
            return;
        }
        Vec3 aim = this.aim(owner);
        this.face(aim);
        if (this.tickCount == this.entityData.get(HOVER_TICKS)) {
            this.setDeltaMovement(aim.scale(SPEED));
            this.hasImpulse = true;
            this.playSound(SoundEvents.TRIDENT_THROW.value(), 0.45F, 1.5F + this.random.nextFloat() * 0.3F);
        }
    }

    /** Toward the foe's middle while it lives, else where the wielder looks. */
    private Vec3 aim(Entity owner) {
        if (this.homingTarget != null && this.homingTarget.isAlive() && this.homingTarget.level() == this.level()) {
            Vec3 to = this.homingTarget.getBoundingBox().getCenter().subtract(this.position());
            if (to.lengthSqr() > 1.0E-4) {
                return to.normalize();
            }
        }
        return owner.getLookAngle();
    }

    /** Turns the spear along {@code dir}, the same way {@code updateRotation} reads a flight. */
    public void face(Vec3 dir) {
        this.setYRot((float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(dir.y, dir.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    private void tickFlight() {
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide) {
            if (this.homingTarget != null && this.homingTarget.isAlive() && this.homingTarget.level() == this.level()) {
                Vec3 to = this.homingTarget.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(SPEED);
                motion = motion.lerp(to, HOMING).normalize().scale(SPEED);
                this.setDeltaMovement(motion);
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !net.neoforged.neoforge.event.EventHooks.onProjectileImpact(this, hit)) {
                this.onHit(hit);
                if (this.isRemoved() || this.isStuck() || this.entityData.get(IN_BLOCK)) {
                    return;
                }
            }
            if (this.tickCount > this.entityData.get(HOVER_TICKS) + FLIGHT_TICKS) {
                this.burst();
                return;
            }
        } else if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
        this.setPos(this.position().add(motion));
        this.updateRotation();
    }

    private void tickStuck() {
        Entity host = this.level().getEntity(this.entityData.get(STUCK_TO));
        if (!(host instanceof LivingEntity living) || !living.isAlive()) {
            if (!this.level().isClientSide) {
                this.burst();
            }
            return;
        }
        this.followHost(living);
        if (this.level().isClientSide) {
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.05, 0.03, (this.random.nextDouble() - 0.5) * 0.05);
            }
            return;
        }
        this.stuckAge++;
        if (this.stuckAge % 20 == 0) {
            this.strike(living, STUCK_SHARE);
        }
        if (this.stuckAge >= STUCK_TICKS) {
            this.burst();
        }
    }

    private void followHost(LivingEntity host) {
        Vector3f local = this.entityData.get(STUCK_OFFSET);
        Vec3 world = new Vec3(local.x, local.y, local.z).yRot(-host.yBodyRot * Mth.DEG_TO_RAD);
        this.setPos(host.position().add(world));
        this.setYRot(host.yBodyRot + this.entityData.get(STUCK_YAW));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) {
            return false;
        }
        // End Crystals and the Ender Dragon's body parts are not living entities, but both must be hittable.
        if (target instanceof EndCrystal) {
            return true;
        }
        if (target instanceof EnderDragonPart part) {
            return this.isFoe(part.parentMob);
        }
        return target instanceof LivingEntity living && this.isFoe(living);
    }

    /** Not the wielder, nothing on the wielder's team, and none of the wielder's own pets. */
    public boolean isFoe(LivingEntity target) {
        Entity owner = this.getOwner();
        if (!target.isAlive() || target == owner || target.isSpectator()) {
            return false;
        }
        if (owner != null && (target.isAlliedTo(owner) || owner.isAlliedTo(target))) {
            return false;
        }
        return !(target instanceof OwnableEntity pet) || owner == null || !owner.getUUID().equals(pet.getOwnerUUID());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof EndCrystal crystal) {
            crystal.hurt(this.damageSource(), 1.0F);
            this.burst();
            return;
        }
        // A dragon part is hit where it is, but the spear lodges in (and strikes) the dragon itself.
        Entity hitBox = result.getEntity();
        LivingEntity host = hitBox instanceof EnderDragonPart part ? part.parentMob
                : hitBox instanceof LivingEntity living ? living : null;
        if (host == null) {
            return;
        }
        // Where the flight line enters the foe's box, pushed a little inside so the shaft looks driven in.
        Vec3 start = this.position();
        Vec3 dir = this.getDeltaMovement().normalize();
        Vec3 entry = hitBox.getBoundingBox().clip(start, start.add(this.getDeltaMovement())).orElse(hitBox.getBoundingBox().getCenter());
        Vec3 point = entry.add(dir.scale(0.25));
        Vec3 local = point.subtract(host.position()).yRot(host.yBodyRot * Mth.DEG_TO_RAD);

        List<ZaeldrisSpear> lodged = this.level().getEntitiesOfClass(ZaeldrisSpear.class, host.getBoundingBox().inflate(2.0),
                spear -> spear != this && spear.entityData.get(STUCK_TO) == host.getId());
        if (lodged.size() >= MAX_STUCK) {
            lodged.sort(Comparator.comparingInt((ZaeldrisSpear spear) -> spear.stuckAge).reversed());
            for (int i = 0; i <= lodged.size() - MAX_STUCK; i++) {
                lodged.get(i).burst();
            }
        }

        this.entityData.set(STUCK_OFFSET, new Vector3f((float) local.x, (float) local.y, (float) local.z));
        this.entityData.set(STUCK_YAW, Mth.wrapDegrees(this.getYRot() - host.yBodyRot));
        this.entityData.set(STUCK_TO, host.getId());
        this.followHost(host);
        this.strike(host, STUCK_SHARE);
        this.playSound(SoundEvents.TRIDENT_HIT, 0.6F, 1.4F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Held just short of the face: the drawn tip reaches past the entity's position, into the block.
        this.setPos(result.getLocation().subtract(this.getDeltaMovement().normalize().scale(0.2)));
        this.setDeltaMovement(Vec3.ZERO);
        this.stuckBlock = result.getBlockPos();
        this.entityData.set(IN_BLOCK, true);
        this.playSound(SoundEvents.TRIDENT_HIT_GROUND, 0.7F, 1.3F);
    }

    private void tickInBlock() {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide) {
            if (this.random.nextInt(8) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.04, 0.02, (this.random.nextDouble() - 0.5) * 0.04);
            }
            return;
        }
        this.stuckAge++;
        if (this.stuckAge >= STUCK_TICKS || this.stuckBlock == null || this.level().getBlockState(this.stuckBlock).isAir()) {
            this.burst();
        }
    }

    /** Hurts every foe in reach, lends them God Touch, and is gone. Breaks no block. */
    public void burst() {
        if (this.isRemoved()) {
            return;
        }
        if (this.level() instanceof ServerLevel level) {
            Vec3 at = this.position();
            AABB reach = new AABB(at, at).inflate(BURST_RADIUS);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach,
                    target -> this.isFoe(target) && target.getBoundingBox().getCenter().distanceToSqr(at) <= BURST_RADIUS * BURST_RADIUS)) {
                this.strike(target, BURST_SHARE);
            }
            for (EndCrystal crystal : level.getEntitiesOfClass(EndCrystal.class, reach)) {
                crystal.hurt(this.damageSource(), 1.0F);
            }
            level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 16, 0.2, 0.2, 0.2, 0.18);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 10, 0.4, 0.4, 0.4, 0.4);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.35F, 1.7F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        this.discard();
    }

    /** One hit as the wielder: a share of their attack, the lance's enchantments on top, their on-hit effects after, and God Touch. */
    private void strike(LivingEntity target, float share) {
        DamageSource source = this.damageSource();
        float amount = this.attackDamage * share;
        ServerLevel level = (ServerLevel) this.level();
        if (!this.weapon.isEmpty()) {
            amount = EnchantmentHelper.modifyDamage(level, this.weapon, target, source, amount);
        }
        if (this.getOwner() instanceof Player player) {
            target.setLastHurtByPlayer(player);
            player.setLastHurtMob(target);
        }
        if (target.hurt(source, amount) && !this.weapon.isEmpty()) {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, this.weapon);
        }
        target.addEffect(new MobEffectInstance(ModEffects.GOD_TOUCH, GodTouchEffect.DURATION_TICKS), this.getOwner());
    }

    private DamageSource damageSource() {
        return new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE),
                this, this.getOwner());
    }

    // --- light ---

    private void updateLight() {
        // Every other tick in flight, every fourth once lodged: up to seven spears ride one moving foe.
        if (this.level().isClientSide || this.tickCount % (this.isStuck() ? 4 : 2) != 0) {
            return;
        }
        BlockPos pos = this.blockPosition();
        if (pos.equals(this.lightPos)) {
            return;
        }
        this.clearLight();
        BlockState state = this.level().getBlockState(pos);
        boolean water = state.is(Blocks.WATER) && state.getFluidState().isSource();
        if (state.isAir() || water) {
            this.level().setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL)
                    .setValue(BlockStateProperties.WATERLOGGED, water), Block.UPDATE_CLIENTS);
            this.lightPos = pos.immutable();
            this.lightWaterlogged = water;
        }
    }

    private void clearLight() {
        if (this.lightPos == null) {
            return;
        }
        if (this.level().isLoaded(this.lightPos) && this.level().getBlockState(this.lightPos).is(Blocks.LIGHT)) {
            this.level().setBlock(this.lightPos, this.lightWaterlogged ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                    : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        this.lightPos = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            this.clearLight();
        }
        super.remove(reason);
    }

    /**
     * The spawn packet sets the rotation after placing the entity, which leaves the previous-tick rotation at 0: the first frame
     * would then turn the spear from due south to its aim, showing it side-on for a moment. Start both at the aim.
     */
    @Override
    public void recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }
}
