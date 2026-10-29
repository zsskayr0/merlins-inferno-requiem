package dev.zsskayr.merlins_inferno.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.entity.cinder.*;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A lava-dependent, multipart Circle 2 predator. All damage and heat decisions are server-side. */
public class CinderEntity extends Monster implements GeoEntity {
    public static final double MAX_HEALTH = 60;
    public static final double ATTACK_DAMAGE = 12;
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(CinderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_START = SynchedEntityData.defineId(CinderEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> COLD = SynchedEntityData.defineId(CinderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(CinderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceKey<DamageType> BITE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "cinder_bite"));
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final CinderVitals vitals = new CinderVitals();
    private final CinderPart[] parts;
    private final Set<UUID> bitten = new HashSet<>();
    private Vec3 previousHead;
    private Vec3 attackAim;
    private Vec3 roamPoint;
    private int attackCooldown = 40;
    private int diveTicks;
    private int roamTicks;
    private int hurtCooldown;
    private boolean fullySubmerged;
    private double immersedFraction;
    private boolean landPoseReady;
    private int returnSearchTicks;

    public CinderEntity(EntityType<? extends CinderEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
        setPathfindingMalus(PathType.LAVA, 0);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0);
        setPathfindingMalus(PathType.DANGER_FIRE, 0);
        parts = new CinderPart[CinderRig.partCount()];
        for (int i = 0; i < parts.length; i++) parts[i] = new CinderPart(this,
                i == 0 ? 1.25F : i > 24 ? .55F : 1.0F, i == 0 ? 1.25F : i > 24 ? 1.0F : 1.9F);
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE).add(Attributes.ARMOR, 4)
                .add(Attributes.MOVEMENT_SPEED, .3).add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.STEP_HEIGHT, 1);
    }

    @Override protected void registerGoals() {
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, CinderAction.SWIM.ordinal());
        builder.define(ACTION_START, 0L);
        builder.define(COLD, 0);
        builder.define(LANDED, false);
    }
    @Override public boolean isMultipartEntity() { return true; }
    @Override public CinderPart[] getParts() { return parts; }
    @Override public void setId(int id) {
        super.setId(id);
        if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }
    public CinderAction action() { return CinderAction.fromId(entityData.get(ACTION)); }
    public int coldTicks() { return entityData.get(COLD); }
    public boolean onLand() { return entityData.get(LANDED); }
    public boolean fullySubmerged() { return fullySubmerged; }
    public double immersedFraction() { return immersedFraction; }
    public double actionTicks(float partialTick) { return Math.max(0, level().getGameTime() - entityData.get(ACTION_START) + partialTick); }
    public float[] animationPose(float partialTick) {
        return CinderRig.pose(action(), actionTicks(partialTick), coldTicks(), onLand());
    }
    public float damageMultiplier() { return 1 - .7F * coldTicks() / CinderVitals.COLD_LIMIT; }

    private void begin(CinderAction next) {
        if (action() == next) return;
        entityData.set(ACTION, next.ordinal());
        entityData.set(ACTION_START, level().getGameTime());
        bitten.clear();
    }

    @Override public void tick() {
        super.tick();
        if (parts == null) return;
        updateParts();
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        scanImmersion();
        vitals.tick(immersedFraction, fullySubmerged);
        entityData.set(COLD, vitals.coldTicks());
        if (vitals.shouldHeal()) heal(3);
        if (vitals.isTerminal()) {
            hurt(damageSources().genericKill(), Float.MAX_VALUE);
            return;
        }
        if (vitals.shouldFreezeDamage()) hurt(damageSources().freeze(), 2);
        if (!isAlive()) return;
        if (attackCooldown > 0) attackCooldown--;
        if (hurtCooldown > 0) hurtCooldown--;
        if (diveTicks > 0) diveTicks--;
        // Keep the land pose across a small step or a short fall. Only real
        // re-entry into lava releases it, not one lost ground-contact tick.
        boolean land = !isInLava() && (onGround() || onLand());
        boolean previousLand = onLand();
        entityData.set(LANDED, land);
        if (!land) landPoseReady = false;
        if (!isNoAi()) {
            if (land && !previousLand && !action().attacking() && action() != CinderAction.HURT) begin(CinderAction.RISE);
            think(server);
        }
        bodyContact(server);
        wake(server);
    }

    private void updateParts() {
        Vec3[] pose = CinderRig.parts(animationPose(0));
        previousHead = parts[0].position().add(0, parts[0].getBbHeight() / 2, 0);
        float angle = (180 - getYRot()) * Mth.DEG_TO_RAD;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        for (int i = 0; i < parts.length; i++) {
            CinderPart part = parts[i];
            Vec3 p = pose[i];
            Vec3 world = position().add(p.x * cos + p.z * sin, p.y, -p.x * sin + p.z * cos);
            part.xo = part.getX(); part.yo = part.getY(); part.zo = part.getZ();
            part.setPos(world.x, world.y - part.getBbHeight() / 2, world.z);
        }
        if (tickCount < 3 || previousHead.distanceToSqr(parts[0].position()) > 64) previousHead = headPosition();
    }
    public Vec3 headPosition() { return parts[0].position().add(0, parts[0].getBbHeight() / 2, 0); }

    /** All corners, top and centre must be under the actual fluid surface. */
    private boolean covered(AABB box) {
        for (double x : new double[]{box.minX + .05, box.maxX - .05}) {
            for (double y : new double[]{box.minY + .05, box.maxY - .05}) {
                for (double z : new double[]{box.minZ + .05, box.maxZ - .05}) if (!lavaAt(new Vec3(x,y,z))) return false;
            }
        }
        return true;
    }
    private boolean lavaAt(Vec3 p) {
        BlockPos pos = BlockPos.containing(p);
        if (!level().hasChunkAt(pos)) return false;
        var fluid = level().getFluidState(pos);
        return fluid.is(FluidTags.LAVA) && p.y < pos.getY() + fluid.getHeight(level(), pos);
    }
    private void scanImmersion() {
        int count = 0;
        boolean all = covered(getBoundingBox());
        for (CinderPart part : parts) {
            if (lavaAt(part.position().add(0, part.getBbHeight() / 2, 0))) count++;
            all &= covered(part.getBoundingBox());
        }
        immersedFraction = (double)count / parts.length;
        fullySubmerged = all;
    }

    @Override public void travel(Vec3 input) {
        // Inertia is intentional: the wind-up fixes a dodgeable trajectory.
        Vec3 velocity = getDeltaMovement();
        move(MoverType.SELF, velocity);
        if (isInLava()) setDeltaMovement(velocity.multiply(.90, .85, .90));
        // Keep a small downward probe on land. A zero-Y move clears onGround,
        // which used to alternate LANDED and restart RISE every other tick.
        else setDeltaMovement(velocity.x * .99, isNoGravity() ? velocity.y * .98 : (onGround() ? -.08 : velocity.y - .08), velocity.z * .99);
        calculateEntityAnimation(false);
    }

    private void think(ServerLevel server) {
        int t = (int)actionTicks(0);
        if (getTarget() != null && (!getTarget().isAlive() || distanceToSqr(getTarget()) > 40 * 40
                || (getTarget() instanceof Player p && (p.isCreative() || p.isSpectator())))) setTarget(null);
        CinderAction current = action();
        if (current == CinderAction.HURT) {
            if (t >= current.ticks) { diveTicks = 80; resume(); }
            return;
        }
        if (current == CinderAction.RISE) {
            if (isInLava()) resume();
            else if (t >= current.ticks) { landPoseReady = true; begin(CinderAction.LAND_IDLE); }
            setDeltaMovement(getDeltaMovement().multiply(.8, 1, .8));
            return;
        }
        if (current.attacking()) {
            runAttack(current, t);
            if (t >= current.ticks) {
                diveTicks = isInLava() ? 70 : 0;
                attackCooldown = 100 + random.nextInt(61);
                resume();
            }
            return;
        }
        if (onLand()) {
            if (getTarget() != null) face(getTarget().position(), 4);
            begin(coldTicks() >= 200 ? CinderAction.COOLING : CinderAction.LAND_IDLE);
            crawlBackToLava();
            if (getTarget() != null && attackCooldown == 0 && headPosition().distanceToSqr(getTarget().getBoundingBox().getCenter()) < 4.5 * 4.5) startAttack(CinderAction.LAND_ATTACK);
            return;
        }
        if (!isInLava()) return; // Ballistic flight, falling, or a ledge.
        if (getTarget() != null && diveTicks == 0 && getHealth() > 12 && attackCooldown == 0 && hasLineOfSight(getTarget())) {
            double d = distanceToSqr(getTarget());
            if (d < 12 * 12 && getTarget().getY() - getY() <= 5) {
                startAttack(getTarget().isInLava() ? CinderAction.DASH : CinderAction.LEAP);
                return;
            }
        }
        swim(server);
    }

    private void resume() {
        if (onLand() && !landPoseReady) { landPoseReady = true; begin(CinderAction.RISE); }
        else if (onLand()) begin(coldTicks() >= 200 ? CinderAction.COOLING : CinderAction.LAND_IDLE);
        else begin(CinderAction.SWIM);
    }
    private void startAttack(CinderAction type) {
        attackAim = getTarget().position().add(0, getTarget().getBbHeight() * .45, 0);
        face(attackAim, 180);
        begin(type);
        playSound(SoundEvents.SKELETON_AMBIENT, .7F, .55F);
    }

    private void runAttack(CinderAction type, int t) {
        if (attackAim == null) { resume(); return; }
        if (type == CinderAction.LEAP) {
            if (t < 7) setDeltaMovement(getDeltaMovement().scale(.65));
            if (t == 7) {
                Vec3 offset = CinderRig.parts(CinderRig.pose(type, 17, coldTicks(), false))[0];
                double a = (180 - getYRot()) * Mth.DEG_TO_RAD;
                offset = new Vec3(offset.x * Math.cos(a) + offset.z * Math.sin(a), offset.y, -offset.x * Math.sin(a) + offset.z * Math.cos(a));
                Vec3 delta = attackAim.subtract(position().add(offset));
                double steps = 10, horizontalSum = (1 - Math.pow(.99, steps)) / .01;
                setDeltaMovement(Mth.clamp(delta.x / horizontalSum, -1.25, 1.25),
                        Mth.clamp((delta.y + .04 * steps * (steps - 1)) / steps, .25, 1.1),
                        Mth.clamp(delta.z / horizontalSum, -1.25, 1.25));
                hasImpulse = true;
                playSound(SoundEvents.GENERIC_SPLASH, 1.4F, .55F);
            }
            if (t >= 15 && t <= 21) bite(1, 0);
        } else if (type == CinderAction.DASH) {
            if (t == 3) {
                Vec3 delta = attackAim.subtract(headPosition()).normalize();
                setDeltaMovement(delta.scale(1.15));
                hasImpulse = true;
            }
            int window = t == 6 ? 0 : t == 11 ? 1 : t == 16 ? 2 : -1;
            if (window >= 0) { bitten.clear(); bite(.5F, window); }
        } else if (type == CinderAction.LAND_ATTACK) {
            setDeltaMovement(getDeltaMovement().multiply(.7, 1, .7));
            if (t >= 10 && t <= 14) bite(.75F, 0);
        }
    }

    private void bite(float multiplier, int window) {
        Vec3 head = headPosition();
        AABB swept = parts[0].getBoundingBox().inflate(.55).minmax(new AABB(previousHead, head).inflate(.65));
        for (var victim : level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, swept,
                e -> e != this && !(e instanceof CinderEntity) && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            if (bitten.contains(victim.getUUID()) || !clearRay(head, victim.getBoundingBox().getCenter())) continue;
            bitten.add(victim.getUUID());
            DamageSource source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BITE), this);
            if (victim.hurt(source, (float)getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier * damageMultiplier())) {
                Vec3 toward = victim.position().subtract(position());
                if (!victim.isInLava()) {
                    BlockPos lava = findLava(victim.blockPosition(), 10, 4);
                    if (lava != null) toward = Vec3.atCenterOf(lava).subtract(victim.position());
                }
                if (toward.horizontalDistanceSqr() > 1e-5) victim.knockback(.85 * damageMultiplier(), -toward.x, -toward.z);
                playSound(SoundEvents.SKELETON_STEP, 1.2F, .6F);
            }
        }
    }
    private boolean clearRay(Vec3 from, Vec3 to) {
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void swim(ServerLevel server) {
        boolean retreat = diveTicks > 0 || getHealth() < getMaxHealth() * .4F;
        Vec3 destination;
        if (!retreat && getTarget() != null) {
            double angle = tickCount * .012 + getId();
            destination = getTarget().position().add(Math.cos(angle) * 7, 0, Math.sin(angle) * 7);
        } else {
            if (roamPoint == null || --roamTicks <= 0 || position().distanceToSqr(roamPoint) < 4) {
                roamTicks = 60 + random.nextInt(80);
                BlockPos lava = findLava(blockPosition().offset(random.nextInt(17)-8, 0, random.nextInt(17)-8), 6, 5);
                roamPoint = lava == null ? position() : Vec3.atCenterOf(lava);
            }
            destination = roamPoint;
        }
        BlockPos fluid = findLava(BlockPos.containing(destination), 4, 8);
        if (fluid == null) fluid = blockPosition();
        int surface = fluid.getY();
        while (surface < fluid.getY() + 10 && server.hasChunkAt(new BlockPos(fluid.getX(), surface + 1, fluid.getZ()))
                && server.getFluidState(new BlockPos(fluid.getX(), surface + 1, fluid.getZ())).is(FluidTags.LAVA)) surface++;
        destination = new Vec3(fluid.getX() + .5, surface + 1 - (retreat ? 3.0 : 1.35 + .2*Math.sin(tickCount*.025)), fluid.getZ() + .5);
        face(destination, retreat ? 2 : 1.5F);
        Vec3 toward = destination.subtract(position());
        double speed = retreat ? .23 : getTarget() != null ? .24 : .15;
        begin(getTarget() != null && !retreat ? CinderAction.FAST : CinderAction.SWIM);
        Vec3 desired = toward.lengthSqr() > .1 ? toward.normalize().scale(speed) : Vec3.ZERO;
        // Do not push the head into a bank while it is cruising.
        if (!server.noCollision(this, getBoundingBox().move(desired))) { roamPoint = null; desired = new Vec3(0, -.1, 0); }
        setDeltaMovement(getDeltaMovement().scale(.8).add(desired.scale(.2)));
    }

    private void face(Vec3 at, float maximum) {
        Vec3 delta = at.subtract(position());
        float desired = (float)(Math.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90;
        setYRot(Mth.approachDegrees(getYRot(), desired, maximum));
        yBodyRot = getYRot(); setYHeadRot(getYRot());
    }
    private void crawlBackToLava() {
        if (--returnSearchTicks <= 0) {
            returnSearchTicks = 40;
            BlockPos lava = findLava(blockPosition(), 14, 5);
            if (lava != null) {
                var path = getNavigation().createPath(lava.above(), 1);
                if (path != null && path.canReach()) getNavigation().moveTo(path, .1);
                else getNavigation().stop();
            } else getNavigation().stop();
        }
        var path = getNavigation().getPath();
        if (path == null || path.isDone()) {
            setDeltaMovement(getDeltaMovement().multiply(.65,1,.65));
            return;
        }
        Vec3 toward = path.getNextEntityPos(this).subtract(position());
        double horizontal = toward.horizontalDistance();
        if (horizontal < .15) { path.advance(); return; }
        double speed = .03 * Math.max(.3, 1 - coldTicks() / 850.0);
        setDeltaMovement(toward.x / horizontal * speed, getDeltaMovement().y, toward.z / horizontal * speed);
        face(path.getNextEntityPos(this), 1);
    }
    private BlockPos findLava(BlockPos origin, int radius, int vertical) {
        BlockPos best = null; double distance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx += 2) for (int dz = -radius; dz <= radius; dz += 2) {
            for (int dy = -vertical; dy <= vertical; dy++) {
                BlockPos pos = origin.offset(dx,dy,dz);
                if (!level().hasChunkAt(pos) || !level().getFluidState(pos).is(FluidTags.LAVA)) continue;
                double d = pos.distSqr(origin);
                if (d < distance) { best = pos; distance = d; }
            }
        }
        return best;
    }

    private void bodyContact(ServerLevel server) {
        if (tickCount % 20 != 0 || action().attacking()) return;
        Set<UUID> touched = new HashSet<>();
        for (CinderPart part : parts) {
            for (Player player : server.getEntitiesOfClass(Player.class, part.getBoundingBox(), p -> !p.isCreative() && !p.isSpectator())) {
                if (touched.add(player.getUUID()) && clearRay(part.getBoundingBox().getCenter(), player.getBoundingBox().getCenter()))
                    player.hurt(damageSources().mobAttack(this), 4 * damageMultiplier());
            }
        }
    }
    private void wake(ServerLevel server) {
        if (tickCount % 5 != 0 || immersedFraction == 0) return;
        for (int i = 0; i < parts.length; i += 4) {
            CinderPart part = parts[i]; BlockPos p = part.blockPosition();
            int y = p.getY();
            while (y < p.getY()+4 && server.getFluidState(new BlockPos(p.getX(),y,p.getZ())).is(FluidTags.LAVA)) y++;
            if (server.getFluidState(new BlockPos(p.getX(),y-1,p.getZ())).is(FluidTags.LAVA)
                    && !server.getFluidState(new BlockPos(p.getX(),y,p.getZ())).is(FluidTags.LAVA)) {
                server.sendParticles(ParticleTypes.SMALL_FLAME, part.getX(), y + .03, part.getZ(), i == 0 ? 3 : 1, .18, .015, .18, .008);
                if (i == 0) server.sendParticles(ParticleTypes.SMOKE, part.getX(), y+.1, part.getZ(), 1, .25,.02,.25,.01);
            }
        }
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && isAlive() && source.getEntity() != null && hurtCooldown == 0) {
            hurtCooldown = 25;
            Vec3 away = position().subtract(source.getEntity().position()).normalize();
            setDeltaMovement(away.x*.35, isInLava() ? -.08 : getDeltaMovement().y, away.z*.35);
            begin(CinderAction.HURT);
        }
        return accepted;
    }
    @Override public void die(DamageSource source) {
        if (!level().isClientSide) begin(CinderAction.DEATH);
        super.die(source);
    }
    @Override protected void tickDeath() {
        deathTime++;
        setDeltaMovement(Vec3.ZERO);
        if (deathTime >= CinderAction.DEATH.ticks && !level().isClientSide) remove(RemovalReason.KILLED);
    }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SKELETON_DEATH; }
    @Override public int getAmbientSoundInterval() { return 180; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean isPickable() { return false; } // Use the actual head/body parts, not the root's empty box.
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        // Vanilla derives distance from the small head/root hitbox, which would
        // cull a fifteen-block body as though it were a one-block creature.
        double range = 160 * getViewScale();
        return distance < range * range;
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { /* shared sampled rig owns every bone */ }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CinderCold", coldTicks());
        tag.putInt("CinderSoaked", vitals.soakedTicks());
        tag.putInt("CinderAttackCooldown", attackCooldown);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        vitals.restore(tag.getInt("CinderCold"), tag.getInt("CinderSoaked"));
        entityData.set(COLD, vitals.coldTicks());
        attackCooldown = Math.max(20, tag.getInt("CinderAttackCooldown"));
        entityData.set(ACTION_START, level().getGameTime());
        entityData.set(ACTION, CinderAction.SWIM.ordinal());
    }

    public static boolean checkCinderSpawnRules(EntityType<CinderEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND || reason == MobSpawnType.SPAWNER) return true;
        if (!(level instanceof ServerLevel server) || server.dimension() != OblivionDimension.LEVEL || server.getDifficulty() == Difficulty.PEACEFUL) return false;
        return ProgressionHelper.hasReached(server, ProgressionHelper.SECOND_CIRCLE)
                && level.getFluidState(pos).is(FluidTags.LAVA) && level.getFluidState(pos.below()).is(FluidTags.LAVA);
    }
}
