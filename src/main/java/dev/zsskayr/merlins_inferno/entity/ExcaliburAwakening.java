package dev.zsskayr.merlins_inferno.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The awakening of a mythic: a carried Excalibur turns into the true one when the world reaches Circle 3, and a carried
 * Gehenna into the sinned one in Circle 2 (red rays and hellfire instead of gold). An
 * earthquake built like the Ender Dragon's death, seen by everyone around:
 * <ol>
 *   <li><b>The call</b> (0 to {@value #RAYS_AT} ticks): the worn sword leaves its bearer's hand and comes to stand,
 *   upright and floating, a few blocks in front of them; the ground starts to shake and crack.</li>
 *   <li><b>The rays</b> ({@value #RAYS_AT} to {@value #CLIMAX_AT}): golden rays burst out of it like a dying dragon's,
 *   the light sweeps the sword from pommel to point, and the quake grows.</li>
 *   <li><b>The climax</b> ({@value #CLIMAX_AT}): a flash, a lightning bolt that burns nothing, a shockwave that throws
 *   mobs back without hurting them. The sword becomes the true one here.</li>
 *   <li><b>The return</b> (to {@value #DURATION}): the true sword flies back into its bearer's hand and slot.</li>
 * </ol>
 * The sword really leaves the inventory: it is kept on the player ({@code ModAttachments.EXCALIBUR_HELD}, saved and kept
 * through death) and an outline holds its slot (item.ExcaliburSlotItem); at the end it goes back there. The entity is not
 * saved: a ritual cut short (death, logout, a crash) is finished by {@link #resume} once the player is back - the sword
 * always comes back and is never dropped.
 * The camera shake, the rays and the flash are client side (client.ExcaliburAwakeningClient).
 */
public class ExcaliburAwakening extends Entity {
    public static final int RAYS_AT = 20;
    public static final int CLIMAX_AT = 80;
    public static final int DURATION = 100;
    /** How far the quake reaches on the ground, in blocks. */
    private static final double QUAKE_RADIUS = 10.0;
    /** Mobs inside this radius are thrown back at the climax. */
    private static final double SHOCKWAVE_RADIUS = 9.0;
    /** How far in front of its bearer the sword stands, and how high over their feet. */
    private static final double AHEAD = 3.5;
    private static final double STAND_HEIGHT = 2.2;
    /** Ticks the sword takes to fly out of the hand to where it stands; it heads back at RETURN_AT. */
    private static final int OUT_TICKS = 18;
    private static final int RETURN_AT = CLIMAX_AT + 4;

    /** Whether the sword rising is Gehenna (red rays, hellfire) rather than Excalibur. Synced for the renderer. */
    private static final EntityDataAccessor<Boolean> GEHENNA = SynchedEntityData.defineId(ExcaliburAwakening.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(ExcaliburAwakening.class, EntityDataSerializers.INT);
    /** Bearers whose ritual is running, so one sword does not start a second one. Server side. */
    private static final Map<UUID, ExcaliburAwakening> RUNNING = new HashMap<>();

    /** Where the sword stands: picked in front of the bearer when the ritual starts, then fixed. Server side. */
    private Vec3 anchor;

    public ExcaliburAwakening(EntityType<? extends ExcaliburAwakening> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Starts the ritual for this player: their worn Excalibur leaves the inventory. Nothing if one is already running. */
    public static void start(ServerPlayer player) {
        ExcaliburAwakening running = RUNNING.get(player.getUUID());
        if (running != null && !running.isRemoved()) {
            return;
        }
        if (!player.getData(ModAttachments.EXCALIBUR_HELD).isEmpty() || !take(player)) {
            return;
        }
        ExcaliburAwakening ritual = new ExcaliburAwakening(ModEntityTypes.EXCALIBUR_AWAKENING.get(), player.level());
        ritual.entityData.set(OWNER, player.getId());
        ritual.entityData.set(GEHENNA, player.getData(ModAttachments.EXCALIBUR_HELD).is(ModItems.GEHENNA.get()));
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        look = look.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, player.getYRot()) : look.normalize();
        ritual.anchor = new Vec3(player.getX() + look.x * AHEAD, player.getY() + STAND_HEIGHT, player.getZ() + look.z * AHEAD);
        Vec3 hand = hand(player);
        ritual.setPos(hand.x, hand.y, hand.z);
        player.level().addFreshEntity(ritual);
        RUNNING.put(player.getUUID(), ritual);
        ServerLevel level = player.serverLevel();
        playAt(level, ritual.position(), SoundEvents.WARDEN_EMERGE, 2.5F, 0.6F);
        playAt(level, ritual.position(), SoundEvents.BEACON_ACTIVATE, 1.5F, 0.5F);
    }

    /** Takes the first worn Excalibur out of the inventory into the player's keeping, leaving the outline in its slot. */
    private static boolean take(ServerPlayer player) {
        for (NonNullList<ItemStack> compartment : compartments(player)) {
            for (int i = 0; i < compartment.size(); i++) {
                ItemStack stack = compartment.get(i);
                if (ripe(player, stack)) {
                    player.setData(ModAttachments.EXCALIBUR_HELD, stack.copy());
                    compartment.set(i, new ItemStack(ModItems.EXCALIBUR_SLOT.get()));
                    return true;
                }
            }
        }
        return false;
    }

    /** A worn sword whose Circle has come: Gehenna in Circle 2, Excalibur in Circle 3. */
    private static boolean ripe(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        return stack.is(ModItems.GEHENNA.get()) && ProgressionHelper.hasReached(level, ProgressionHelper.SECOND_CIRCLE)
                || stack.is(ModItems.EXCALIBUR.get()) && ProgressionHelper.hasReached(level, ProgressionHelper.THIRD_CIRCLE);
    }

    /**
     * Gives the kept sword back: into the slot its outline holds (wherever it was moved to), else anywhere free. With no
     * room at all it stays kept and {@link #resume} tries again every tick - it is never dropped.
     */
    public static void giveBack(ServerPlayer player) {
        ItemStack held = player.getData(ModAttachments.EXCALIBUR_HELD);
        if (held.isEmpty()) {
            return;
        }
        for (NonNullList<ItemStack> compartment : compartments(player)) {
            for (int i = 0; i < compartment.size(); i++) {
                if (compartment.get(i).is(ModItems.EXCALIBUR_SLOT.get())) {
                    compartment.set(i, held.copy());
                    player.setData(ModAttachments.EXCALIBUR_HELD, ItemStack.EMPTY);
                    return;
                }
            }
        }
        ItemStack copy = held.copy();
        player.getInventory().add(copy);
        if (copy.isEmpty()) {
            player.setData(ModAttachments.EXCALIBUR_HELD, ItemStack.EMPTY);
        }
    }

    /**
     * Finishes a ritual cut short (death, logout, a crash): once the player is alive with no ritual running, the kept
     * sword is awakened if the world is in Circle 3 and given back. Called every server player tick.
     */
    public static void resume(ServerPlayer player) {
        if (player.getData(ModAttachments.EXCALIBUR_HELD).isEmpty() || !player.isAlive()) {
            return;
        }
        ExcaliburAwakening running = RUNNING.get(player.getUUID());
        if (running != null && !running.isRemoved()) {
            return;
        }
        awaken(player);
        giveBack(player);
    }

    /** The kept worn sword becomes the true one, keeping all its data. */
    private static void awaken(ServerPlayer player) {
        ItemStack held = player.getData(ModAttachments.EXCALIBUR_HELD);
        if (ripe(player, held)) {
            var next = held.is(ModItems.GEHENNA.get()) ? ModItems.GEHENNA_SINNED : ModItems.EXCALIBUR_TRUE;
            player.setData(ModAttachments.EXCALIBUR_HELD, held.transmuteCopy(next.get(), held.getCount()));
        }
    }

    private static List<NonNullList<ItemStack>> compartments(Player player) {
        return List.of(player.getInventory().items, player.getInventory().armor, player.getInventory().offhand);
    }

    /** Roughly where the main hand is: the sword leaves from here and comes back here. */
    private static Vec3 hand(Player player) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw)).scale(side);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        return player.position().add(0.0, player.getBbHeight() * 0.55, 0.0).add(right.scale(0.4)).add(forward.scale(0.35));
    }

    /** Where the sword is at tick {@code t}: out of the hand, up to stand in front, hanging, then back into the hand. */
    private Vec3 path(Player owner, float t) {
        Vec3 hand = hand(owner);
        Vec3 stand = this.anchor.add(0.0, 0.08 * Math.sin(t * 0.3), 0.0);
        if (t < OUT_TICKS) {
            float k = t / OUT_TICKS;
            return hand.lerp(stand, 1.0 - (1.0 - k) * (1.0 - k));
        }
        if (t < RETURN_AT) {
            return stand;
        }
        float k = Mth.clamp((t - RETURN_AT) / (DURATION - RETURN_AT), 0.0F, 1.0F);
        return stand.lerp(hand, k * k);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(GEHENNA, false);
    }

    public boolean isGehenna() {
        return this.entityData.get(GEHENNA);
    }

    public Player owner() {
        return this.level().getEntity(this.entityData.get(OWNER)) instanceof Player player ? player : null;
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = this.owner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide()) {
                this.end();
            }
            return;
        }
        if (this.level().isClientSide()) {
            dev.zsskayr.merlins_inferno.client.ExcaliburAwakeningClient.tick(this);
            return;
        }
        if (this.anchor == null) {
            this.end();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        int t = this.tickCount;
        Vec3 at = this.path(owner, t);
        this.setPos(at.x, at.y, at.z);
        if (t < CLIMAX_AT) {
            this.quake(level, owner, t);
        }
        if (t == RAYS_AT) {
            playAt(level, this.position(), SoundEvents.ENDER_DRAGON_DEATH, 1.6F, 1.1F);
        }
        if (t == CLIMAX_AT) {
            this.climax(level, owner);
        }
        if (t > CLIMAX_AT && t < CLIMAX_AT + 16) {
            this.shockwave(level, owner, t - CLIMAX_AT);
        }
        if (t >= DURATION) {
            if (owner instanceof ServerPlayer player) {
                giveBack(player);
                playAt(level, owner.position(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.2F, 0.8F);
            }
            this.end();
        }
    }

    /** The ground cracking around the bearer, wider and harder as the call goes on. */
    private void quake(ServerLevel level, Player owner, int t) {
        float strength = (float) t / CLIMAX_AT;
        int bursts = 2 + Mth.floor(strength * 8.0F);
        double radius = 3.0 + strength * (QUAKE_RADIUS - 3.0);
        for (int i = 0; i < bursts; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double r = 1.5 + this.random.nextDouble() * radius;
            BlockPos ground = ground(level, owner.getX() + Math.cos(angle) * r, owner.getZ() + Math.sin(angle) * r);
            BlockState state = level.getBlockState(ground);
            if (state.isAir()) {
                continue;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), ground.getX() + 0.5, ground.getY() + 1.05, ground.getZ() + 0.5,
                    4 + Mth.floor(strength * 10.0F), 0.4, 0.05, 0.4, 0.2 + strength * 0.3);
            if (this.random.nextFloat() < 0.15F + strength * 0.25F) {
                level.playSound(null, ground, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8F + strength, 0.5F + this.random.nextFloat() * 0.3F);
            }
        }
        if (t % 20 == 10) {
            playAt(level, owner.position(), SoundEvents.WARDEN_DIG, 1.2F + strength * 1.5F, 0.5F);
        }
        // the worn sword shedding its moss and leaves
        level.sendParticles(this.isGehenna() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FALLING_SPORE_BLOSSOM, this.getX(), this.getY() + 0.4, this.getZ(), 2, 0.25, 0.4, 0.25, 0.0);
    }

    /** The peak: lightning that burns nothing, the flash and boom, mobs thrown back, and the sword changed. */
    private void climax(ServerLevel level, Player owner) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(this.getX(), this.anchor.y - STAND_HEIGHT, this.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        playAt(level, this.position(), SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.6F);
        playAt(level, this.position(), SoundEvents.BELL_BLOCK, 2.0F, 0.5F);
        playAt(level, this.position(), SoundEvents.TOTEM_USE, 1.2F, 1.2F);
        level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(this.isGehenna() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 80, 0.2, 0.2, 0.2, 0.45);
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(SHOCKWAVE_RADIUS),
                e -> e != owner && !(e instanceof Player) && e.isAlive())) {
            Vec3 away = mob.position().subtract(owner.position());
            double falloff = 1.0 - Math.min(1.0, away.length() / SHOCKWAVE_RADIUS);
            mob.knockback(0.8 + 1.4 * falloff, -away.x, -away.z);
            mob.hurtMarked = true;
        }
        if (owner instanceof ServerPlayer player) {
            awaken(player);
        }
    }

    /** A ring of broken ground running out from the bearer after the climax. */
    private void shockwave(ServerLevel level, Player owner, int step) {
        double r = 1.0 + step * 0.65;
        int points = 10 + step * 3;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double x = owner.getX() + Math.cos(angle) * r;
            double z = owner.getZ() + Math.sin(angle) * r;
            BlockPos ground = ground(level, x, z);
            BlockState state = level.getBlockState(ground);
            if (!state.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, ground.getY() + 1.05, z, 3, 0.1, 0.05, 0.1, 0.25);
            }
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, x, ground.getY() + 1.1, z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    private static BlockPos ground(ServerLevel level, double x, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        return new BlockPos(bx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1, bz);
    }

    private static void playAt(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void end() {
        RUNNING.values().removeIf(r -> r == this);
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        RUNNING.values().removeIf(r -> r == this);
        super.remove(reason);
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
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
