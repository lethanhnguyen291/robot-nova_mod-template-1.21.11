package net.teogemini.nova.entity;

import net.teogemini.nova.entity.companion.*;
import net.teogemini.nova.sound.NovaSoundController;
import net.teogemini.nova.sound.NovaSounds.Cue;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.teogemini.nova.entity.ai.NovaFollowOwnerGoal;
import net.teogemini.nova.entity.ai.NovaFetchChestGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.teogemini.nova.entity.ai.NovaWaitGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.teogemini.nova.server.NovaOwnerSessions;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;


import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRules;
import net.teogemini.nova.entity.ai.NovaRallyGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.core.particles.ParticleTypes;
import net.teogemini.nova.entity.ai.work.*;

public class NovaEntity extends TamableAnimal implements GeoEntity, net.minecraft.world.entity.ContainerUser {
    public static final int WORK_NONE = 0, WORK_CHOP = 1, WORK_MINE = 2,
            WORK_FARM = 3, WORK_DIG = 4, WORK_PLANT = 5, WORK_CHEST = 6;
    private static final EntityDataAccessor<Integer> NOVA_WORK =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NOVA_CARRYING =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.BOOLEAN);
    private final NovaWorkData novaWork = new NovaWorkData();
    private final NovaSelfRepair novaSelfRepair = new NovaSelfRepair();
    private final NovaSoundController novaSounds = new NovaSoundController(this);
    public NovaSoundController sounds() { return novaSounds; }
    private final NovaToolBelt novaToolBelt = new NovaToolBelt(new net.teogemini.nova.screen.NovaToolContainer(this));
    private final NovaPathClearing novaPathClearing = new NovaPathClearing(this);
    public NovaToolBelt toolBelt() { return novaToolBelt; }
    public ItemStack getPrimaryTool() { return novaToolBelt.primary(); }
    public NovaPathClearing pathClearing() { return novaPathClearing; }
    public boolean canStartNovaJob() { return canNovaWork() && !novaPathClearing.busy(); }
    private final NovaCompanion novaCompanion = new NovaCompanion(this);
    private static final EntityDataAccessor<Integer> NOVA_COMPANION =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.INT);
    public NovaCompanion companion() { return novaCompanion; }
    public int getCompanionState() { return entityData.get(NOVA_COMPANION); }
    public void setCompanionState(int state) {
        if (!level().isClientSide()) entityData.set(NOVA_COMPANION, Mth.clamp(state,0,6));
    }
    public void interruptCompanionAction() {
        if (!level().isClientSide() && isPerformingAction()) finishNovaAction();
        stopTriggeredAnim("companion_gesture", null);
    }
    @Override public boolean hasContainerOpen(net.minecraft.world.level.block.entity.ContainerOpenersCounter counter, BlockPos pos) {
        return companion().hasOpenChest(pos);
    }
    @Override public double getContainerInteractionRange() { return 3.5; }
    @Override public int getMaxFallDistance() { return 1; }
    @Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }
    @Override protected int decreaseAirSupply(int air) { return getMaxAirSupply(); }
    @Override public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        boolean hurt=super.hurtServer(server,source,amount);
        if (hurt && isAlive()) {
            novaSelfRepair.damaged();
            sounds().damaged();
            server.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.7, getZ(), 8, 0.25, 0.3, 0.25, 0.12);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.7, getZ(), 6, 0.2, 0.25, 0.2, 0.08);
            server.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 0.9, getZ(), Math.max(1, (int)(amount / 2)), 0.2, 0.2, 0.2, 0.1);
            showNovaFace(32, 25);
        }
        if (hurt && isAlive() && isTame()) companion().hurt(source.getSourcePosition());
        return hurt;
    }

    @Override public void die(DamageSource source) {
        if (!level().isClientSide() && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 12, 0.35, 0.45, 0.35, 0.04);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 20, 0.4, 0.5, 0.4, 0.12);
        }
        super.die(source);
    }
    private final net.teogemini.nova.entity.ai.tree.NovaTreeMemory treeMemory = new net.teogemini.nova.entity.ai.tree.NovaTreeMemory();
    public net.teogemini.nova.entity.ai.tree.NovaTreeMemory trees() { return treeMemory; }
    public BlockPos lastChoppedRoot;
    public Item expectedSapling;
    private static final RawAnimation[] WORK_ANIMATIONS = {
            RawAnimation.begin().thenLoop("animation.nova.idle"),
            RawAnimation.begin().thenLoop("animation.nova.chop"),
            RawAnimation.begin().thenLoop("animation.nova.mine"),
            RawAnimation.begin().thenLoop("animation.nova.farm"),
            RawAnimation.begin().thenLoop("animation.nova.dig"),
            RawAnimation.begin().thenLoop("animation.nova.plant"),
            RawAnimation.begin().thenLoop("animation.nova.chest")
    };
    private static final RawAnimation CARRY_IDLE =
            RawAnimation.begin().thenLoop("animation.nova.carry_idle");
    private static final RawAnimation CARRY_WALK =
            RawAnimation.begin().thenLoop("animation.nova.carry_walk");

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.nova.idle");

    private static final RawAnimation WALK =
            RawAnimation.begin().thenLoop("animation.nova.walk");
    private static final RawAnimation RUN =
            RawAnimation.begin().thenLoop("animation.nova.run");
    private static final RawAnimation SIT =
            RawAnimation.begin().thenLoop("animation.nova.sit");
    private static final RawAnimation WAVE =
            RawAnimation.begin().thenPlay("animation.nova.wave");
    private static final RawAnimation CELEBRATE =
            RawAnimation.begin().thenPlay("animation.nova.celebrate");

    public static final int ACTION_NONE = 0;
    public static final int ACTION_WAVE = 1;
    public static final int ACTION_CELEBRATE = 2;

    private static final EntityDataAccessor<Integer> NOVA_ACTION =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NOVA_RUNNING =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.BOOLEAN);
    
        public static final int MODE_FOLLOW = 0;
    public static final int MODE_FREE = 1;
    public static final int MODE_WAIT = 2;

    private static final EntityDataAccessor<Integer> NOVA_MOVE_MODE =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> NOVA_SIT_REQUESTED =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> NOVA_WORK_ENABLED =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NOVA_DORMANT =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> NOVA_DORMANT_POSE =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.INT);

    private int novaLookTicks;
    private float novaLookYaw;
    private float novaLookPitch;
    private int actionTicks;

    private boolean novaLampOn;
    private double novaLampHeight = 17.07 / 16.0;
    private int novaLampPoseTick = -100;

    private static final EntityDataAccessor<Integer> NOVA_FACE =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.INT);

    private static final int[] IDLE_FACES = {
            8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23
    };
    private String novaLastGreetingSession = "";
    private boolean novaOwnerWasAway;
    private boolean novaGreetingPending;
    private long novaNextGreetingTime;
    public long novaNextTreeCollectTime;

    private int novaFearTicks;
    private int novaRequestedFace;
    private int novaRequestedTicks;
    private int novaIdleFace;
    private int novaIdleTicks;
    private int novaFaceCooldown = 100;
    private int novaPreviewFace = -1;
        
    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);

    public NovaEntity(EntityType<? extends NovaEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setLeftHanded(false);
        getNavigation().setCanOpenDoors(true);
        getNavigation().setCanFloat(false);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, 8);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DANGER_FIRE, -1);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DAMAGE_FIRE, -1);
            this.lookControl = new LookControl(this) {
            @Override
            protected boolean resetXRotOnTick() {
                return true;
            }
        };
    }

    public NovaWorkData work() { return novaWork; }
    public SimpleContainer getInventory() { return novaWork.inventory; }
    public BlockPos getLinkedChest() {
        GlobalPos link = novaWork.linkedChest();
        return link != null && link.dimension().equals(level().dimension()) ? link.pos() : null;
    }
    public void setLinkedChest(BlockPos pos) {
        novaWork.link(pos == null ? null : GlobalPos.of(level().dimension(), pos.immutable()));
    }
    public boolean isSwordNova() {
        return getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS)
                || (getMainHandItem().isEmpty() && getOffhandItem().is(net.minecraft.tags.ItemTags.SWORDS));
    }
    public boolean isWithinWorkspace(BlockPos pos) {
        if (isSwordNova()) {
            var owner = getOwner();
            BlockPos center = owner != null ? owner.blockPosition() : blockPosition();
            return center.distSqr(pos) <= 576.0;
        }
        BlockPos chest = getLinkedChest();
        return chest != null && chest.distSqr(pos) <= 256.0
                && level().hasChunkAt(pos) && level().getWorldBorder().isWithinBounds(pos);
    }
    public boolean canNovaWork() {
        if (isSwordNova()) {
            return level() instanceof ServerLevel && isAlive() && !isNoAi() && isTame()
                    && novaWork.energy() > 0 && !novaWork.inventoryOpen()
                    && !isOrderedToSit() && !isPerformingAction() && !isRallying();
        }
        BlockPos chest = getLinkedChest();
        return level() instanceof ServerLevel server && isAlive() && !isNoAi() && isTame()
                && novaWork.enabled() && novaWork.energy() > 0 && !novaWork.inventoryOpen()
                && companion().workAllowed()
                && getNovaMoveMode() == MODE_FREE
                && !isOrderedToSit() && !isPerformingAction() && !isRallying()
                && !isCarryingChest() && !isFetchingChest() && chest != null && level().hasChunkAt(chest)
                && level().getBlockState(chest).is(net.teogemini.nova.registry.ModBlocks.NOVA_CHEST)
                && server.getGameRules().get(GameRules.MOB_GRIEFING);
    }
    public void addExperience(int amount) {
        if (!level().isClientSide() && novaWork.addExperience(amount)) {
            sounds().play(Cue.LEVEL_UP);
            showNovaFace(5, 60);
            companion().gesture("proud");
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.0, getZ(), 25, 0.4, 0.5, 0.4, 0.15);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.1, getZ(), 10, 0.35, 0.4, 0.35, 0.05);
            }
        }
    }
    public int getWorkAction() { return entityData.get(NOVA_WORK); }
    public void playWorkAnimation(int action) {
        if (!level().isClientSide()) {
            if (action!=WORK_NONE) companion().cancelGesture();
            entityData.set(NOVA_WORK, Mth.clamp(action, 0, 6));
        }
    }
    public void clearWorkAnimation() { playWorkAnimation(WORK_NONE); }
    public boolean isCarryingChest() { return entityData.get(NOVA_CARRYING); }
    public void setNovaCarrying(boolean carrying) {
        if (!level().isClientSide()) {
            if (carrying) pathClearing().cancel();
            entityData.set(NOVA_CARRYING, carrying);
        }
    }
    private BlockPos fetchingChestTarget;
    public boolean isFetchingChest() { return fetchingChestTarget != null; }
    public BlockPos getFetchingChestTarget() { return fetchingChestTarget; }
    public void startFetchingChest(BlockPos pos) {
        if (pos == null) return;
        this.fetchingChestTarget = pos.immutable();
        setInSittingPose(false);
        setNovaSitRequested(false);
        clearWorkAnimation();
    }
    public void stopFetchingChest() {
        this.fetchingChestTarget = null;
        getNavigation().stop();
    }
    public boolean isRallying() { return novaWork.rallyTarget != null; }
    public void startRally(BlockPos target, BlockPos face) {
        if (level().isClientSide()) return;
        companion().ownerCommand();
        if (isPerformingAction()) finishNovaAction();
        setInSittingPose(false);
        setNovaRunning(false);
        novaWork.rallyTarget = target.immutable();
        novaWork.rallyFace = face.immutable();
        clearWorkAnimation();
        getNavigation().stop();
    }
    public void stopRally() {
        novaWork.rallyTarget = null;
        novaWork.rallyFace = null;
        if (!level().isClientSide()) novaWork.restoreRallyName(this);
        getNavigation().stop();
    }
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        pathClearing().cancel();
        super.dropCustomDeathLoot(level, source, recentlyHit);
        NovaWorkSupport.dropCargo(this, level);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return Cue.HURT.event;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return Cue.DEATH.event;
    }

    @Override
    protected float getSoundVolume() { return 0.55F; }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // Keep NOVA's original footstep. All layers share its server-side rate limit.
        if (!onGround() || isInWater() || !sounds().play(Cue.STEP)) return;

        float volume = NovaSoundController.MECHANICAL_VOLUME;
        playSound(net.minecraft.sounds.SoundEvents.IRON_GOLEM_STEP, 0.12F * volume, 1.20F);

        // Grass rustles on grass; other surfaces use their own quiet footstep.
        var surface = state.getSoundType();
        playSound(surface.getStepSound(), surface.getVolume() * 0.10F * volume,
                surface.getPitch());
    }

    @Override
    public void swing(InteractionHand hand) {
        super.swing(hand);
        if (isSwordNova()) sounds().play(Cue.ATTACK);
        else if (getWorkAction() != WORK_NONE) sounds().play(switch (getWorkAction()) {
            case WORK_CHOP -> Cue.WORK_CHOP;
            case WORK_MINE -> Cue.WORK_MINE;
            case WORK_DIG -> Cue.WORK_DIG;
            case WORK_FARM -> Cue.WORK_FARM;
            case WORK_PLANT -> Cue.WORK_PLANT;
            default -> Cue.WORK;
        });
    }

    @Override
    public net.minecraft.world.entity.HumanoidArm getMainArm() {
        return net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.world.entity.SpawnGroupData data) {
        var result = super.finalizeSpawn(level, difficulty, reason, data);
        setLeftHanded(false);
        sounds().spawned();
        return result;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(NOVA_COMPANION, NovaCompanion.NORMAL);
            builder.define(NOVA_ACTION, ACTION_NONE);
            builder.define(NOVA_RUNNING, false);
            builder.define(NOVA_FACE, 0);
            builder.define(NOVA_WORK, WORK_NONE);
            builder.define(NOVA_CARRYING, false);
            builder.define(NOVA_MOVE_MODE, MODE_FOLLOW);
            builder.define(NOVA_SIT_REQUESTED, false);
            builder.define(NOVA_WORK_ENABLED, false);
            builder.define(NOVA_DORMANT, false);
            builder.define(NOVA_DORMANT_POSE, 0);
    }

    public String getNovaDisplayName() {
        return hasCustomName() ? getCustomName().getString() : "NOVA";
    }

    public boolean isNovaWorkEnabled() {
        return entityData.get(NOVA_WORK_ENABLED);
    }

    public boolean isDormant() {
        return entityData.get(NOVA_DORMANT);
    }

    public void setDormant(boolean dormant) {
        entityData.set(NOVA_DORMANT, dormant);
        if (dormant) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        }
    }

    public int getDormantPose() {
        return entityData.get(NOVA_DORMANT_POSE);
    }

    public void setDormantPose(int pose) {
        entityData.set(NOVA_DORMANT_POSE, Mth.clamp(pose, 0, 4));
    }

    @Override
    public boolean isPushable() {
        return !isDormant() && super.isPushable();
    }

    public void setNovaWorkEnabled(boolean enabled) {
        entityData.set(NOVA_WORK_ENABLED, enabled);
        novaWork.setEnabled(enabled);
    }
    
    public int getNovaFace() {
        return entityData.get(NOVA_FACE);
    }

    // Gọi từ phía server: chọn ID 0-39 trong số tick mong muốn.
    public void showNovaFace(int face, int durationTicks) {
        if (level().isClientSide()) {
            return;
        }

        novaRequestedFace = Math.max(0, Math.min(face, 39));
        novaRequestedTicks = Math.max(1, durationTicks);

        if (hurtTime == 0 && novaFearTicks == 0) {
            entityData.set(NOVA_FACE, novaRequestedFace);
        }
    }

    public int cyclePreviewFace(int delta) {
        if (novaPreviewFace < 0) {
            novaPreviewFace = (delta >= 0) ? 0 : 39;
        } else {
            novaPreviewFace = (novaPreviewFace + delta + 40) % 40;
        }
        entityData.set(NOVA_FACE, novaPreviewFace);
        return novaPreviewFace;
    }

    public void resetPreviewFace() {
        novaPreviewFace = -1;
        novaRequestedTicks = 0;
    }

    private void tickNovaFace() {
        if (level().isClientSide()) {
            return;
        }

        if (isDormant()) {
            int dormantFace = switch (getDormantPose()) {
                case 0 -> 39;
                case 1 -> 8;
                case 2 -> 14;
                case 3 -> 2;
                case 4 -> 24;
                default -> 39;
            };
            entityData.set(NOVA_FACE, dormantFace);
            return;
        }

        if (hurtTime > 0) {
            novaFearTicks = 40;
        }

        // Sợ hãi được ưu tiên khi vừa nhận sát thương.
        if (novaFearTicks > 0) {
            --novaFearTicks;
            novaRequestedTicks = 0;
            novaIdleTicks = 0;
            entityData.set(NOVA_FACE, 32);
            return;
        }

        // Biểu cảm cố định do người chơi chọn.
        if (novaPreviewFace >= 0) {
            entityData.set(NOVA_FACE, novaPreviewFace);
            return;
        }

        // Biểu cảm được yêu cầu bằng code hoặc chế độ xem thử.
        if (novaRequestedTicks > 0) {
            --novaRequestedTicks;
            entityData.set(NOVA_FACE, novaRequestedFace);
            return;
        }

        int companionFace = companion().contextFace();
        if (companionFace >= 0) {
            novaIdleTicks=0;
            entityData.set(NOVA_FACE, companionFace);
            return;
        }
        int contextualFace = -1;

        if (getNovaAction() == ACTION_CELEBRATE) {
            contextualFace = 9;
        } else if (getNovaAction() == ACTION_WAVE) {
            contextualFace = 11;
        } else if (getHealth() < getMaxHealth() * 0.35F) {
            contextualFace = 27;
        } else if (isInSittingPose()) {
            contextualFace = 7;
        } else if (isNovaRunning()) {
            contextualFace = 18;
        } else if (!onGround() || isInWater() || !getNavigation().isDone()) {
            contextualFace = 0;
        }

        if (contextualFace >= 0) {
            novaIdleTicks = 0;
            entityData.set(NOVA_FACE, contextualFace);
            return;
        }

        // Giữ biểu cảm ngẫu nhiên cho hết thời gian đã chọn.
        if (novaIdleTicks > 0) {
            --novaIdleTicks;
            entityData.set(NOVA_FACE, novaIdleFace);
            return;
        }

        // Lúc đứng rảnh: đôi khi đổi mặt, hiếm hơn thì hiện loading.
        if (--novaFaceCooldown <= 0) {
            novaIdleFace = companion().memories().personality().idleFace;

            novaIdleTicks = novaIdleFace == 39 ? 40 : 60;
            novaFaceCooldown = 160 + random.nextInt(241);

            entityData.set(NOVA_FACE, novaIdleFace);
            return;
        }

        entityData.set(NOVA_FACE, 0);
    }

        public int getNovaMoveMode() {
        return entityData.get(NOVA_MOVE_MODE);
    }

    public boolean isNovaSitRequested() {
        return entityData.get(NOVA_SIT_REQUESTED);
    }

    public boolean canNovaRoam() {
        return (!isTame() || getNovaMoveMode() == MODE_FREE || isSwordNova())
                && !pathClearing().busy() && !isFetchingChest()
                && !isOrderedToSit() && !isPerformingAction() && !isRallying()
                && !novaWork.inventoryOpen() && getCompanionState()==NovaCompanion.NORMAL;
    }

    public void setNovaMoveMode(int mode) {
        if (level().isClientSide() || mode < MODE_FOLLOW || mode > MODE_WAIT) {
            return;
        }

        entityData.set(NOVA_MOVE_MODE, mode);
        setNovaSitRequested(false);
    }

    public void setNovaSitRequested(boolean sitting) {
        if (level().isClientSide()) {
            return;
        }

        if (isPerformingAction()) {
            finishNovaAction();
        }

        stopRally();
        companion().ownerCommand();
        clearWorkAnimation();
        setOrderedToSit(sitting);
        entityData.set(NOVA_SIT_REQUESTED, sitting);
        setInSittingPose(sitting && onGround() && !isInWater());
        getNavigation().stop();
        setTarget(null);
        setNovaRunning(false);
    }

    @Override
    public int getMaxHeadYRot() {
        return 90;
    }

    @Override
    public int getMaxHeadXRot() {
        // Tốc độ đổi góc ngước/cúi khi bộ điều khiển nhìn sử dụng giá trị này.
        return 6;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                double dx = getX() - xo;
                double dz = getZ() - zo;

                if (dx * dx + dz * dz > 0.000001) {
                    super.clientTick();
                } else {
                    // Đứng yên: cho đầu quay độc lập, thân giữ nguyên hướng.
                    setYHeadRot(Mth.rotateIfNecessary(
                            yHeadRot, yBodyRot, 90.0F));
                }
            }
        };
    }

    private void tickNovaLook() {
        if (level().isClientSide() || isNoAi() || !isAlive()
                || isPerformingAction() || getWorkAction() != WORK_NONE || isRallying()) {
            return;
        }

        if (companion().controlsLook()) return;
        var owner = getOwner();

        if (owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == level() && distanceToSqr(owner) < 64.0
                && hasLineOfSight(owner)) {
            getLookControl().setLookAt(owner, 6.0F, 6.0F);
            return;
        }

        if (--novaLookTicks <= 0) {
            novaLookTicks = 40 + random.nextInt(61);
            novaLookYaw = random.nextFloat() * 180.0F - 90.0F;
            novaLookPitch = random.nextFloat() * 120.0F - 60.0F;
        }

        double yaw = Math.toRadians(yBodyRot + novaLookYaw);
        double pitch = Math.toRadians(novaLookPitch);
        double horizontal = 4.0 * Math.cos(pitch);

        getLookControl().setLookAt(
                getX() - Math.sin(yaw) * horizontal,
                getEyeY() - Math.sin(pitch) * 4.0,
                getZ() + Math.cos(yaw) * horizontal,
                6.0F, 6.0F);
    }

    public int getNovaAction() {
        return entityData.get(NOVA_ACTION);
    }

    public boolean isPerformingAction() {
        return getNovaAction() != ACTION_NONE;
    }

    public boolean isNovaRunning() {
        return entityData.get(NOVA_RUNNING);
    }
    
        public boolean isNovaLampOn() {
        return novaLampOn;
    }

    public void setNovaLampOn(boolean on) {
        novaLampOn = on;
    }

    public void updateNovaLampHeight(double height) {
        if (Double.isFinite(height) && height > 0 && height < 4) {
            novaLampHeight = height;
            novaLampPoseTick = tickCount;
        }
    }

    public double getNovaLampHeight() {
        if (tickCount - novaLampPoseTick <= 3) {
            return novaLampHeight;
        }

        return 17.07 / 16.0 - (isInSittingPose() ? 1.7 / 16.0 : 0);
    }
    public void setNovaRunning(boolean running) {
        entityData.set(NOVA_RUNNING, running);
    }
        private void tickNovaGreeting() {
        if (level().isClientSide() || !isTame() || isCarryingChest()
                || companion().controlsLook() || companion().panicking()
                || getWorkAction() != WORK_NONE || isRallying()) {
            return;
        }

        if (!(getOwner() instanceof ServerPlayer owner)
                || owner.level() != level() || !owner.isAlive()) {
            novaOwnerWasAway = true;
            return;
        }

        String session = NovaOwnerSessions.get(owner);
        if (session.isEmpty()) {
            return;
        }

        if (!session.equals(novaLastGreetingSession)) {
            novaGreetingPending = true;
        }

        double distanceSquared = distanceToSqr(owner);

        if (distanceSquared >= 16.0 * 16.0) {
            novaOwnerWasAway = true;
        }

        if (getNovaAction() == ACTION_WAVE) {
            faceNovaOwner(owner);
            return;
        }

        if (!novaGreetingPending && !novaOwnerWasAway) {
            return;
        }

        if (distanceSquared > 6.0 * 6.0 || tickCount < 20 || owner.tickCount < 20
                || owner.isSpectator() || !hasLineOfSight(owner)
                || isPerformingAction() || !onGround() || isInWater()
                || hurtTime > 0 || novaFearTicks > 0) {
            return;
        }

        if (!novaGreetingPending && level().getGameTime() < novaNextGreetingTime) {
            return;
        }

        faceNovaOwner(owner);
        startNovaAction(ACTION_WAVE);
    }

    private void faceNovaOwner(ServerPlayer owner) {
        double dx = owner.getX() - getX();
        double dz = owner.getZ() - getZ();

        if (dx * dx + dz * dz < 0.01) {
            return;
        }

        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float yaw = Mth.approachDegrees(getYRot(), targetYaw, 8.0F);

        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        getLookControl().setLookAt(owner, 30.0F, 30.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        novaWork.save(output.child("NovaWork"));
        novaSelfRepair.save(output.child("NovaSelfRepair"));
        output.putInt("NovaBorrowedToolSlot", toolBelt().borrowedSlot());
        trees().save(output.child("NovaTrees"));
        companion().memories().save(output.child("NovaCompanion"));
        output.storeNullable("NovaLastRoot", BlockPos.CODEC, lastChoppedRoot);
        output.store("NovaSapling", ItemStack.OPTIONAL_CODEC, expectedSapling == null
                ? ItemStack.EMPTY : new ItemStack(expectedSapling));
        output.putInt("NovaMoveMode", getNovaMoveMode());
        output.putString("NovaGreetingSession", novaLastGreetingSession);
        output.putBoolean("NovaOwnerWasAway", novaOwnerWasAway);
        output.putBoolean("NovaGreetingPending", novaGreetingPending);
        output.putLong("NovaNextGreetingTime", novaNextGreetingTime);
        output.putLong("NovaNextTreeCollect", novaNextTreeCollectTime);
        output.putInt("NovaPreviewFace", novaPreviewFace);
        output.putBoolean("NovaDormant", isDormant());
        output.putInt("NovaDormantPose", getDormantPose());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setLeftHanded(false);
        novaWork.load(input.childOrEmpty("NovaWork"));
        novaSelfRepair.load(input.childOrEmpty("NovaSelfRepair"));
        toolBelt().restoreAfterLoad(input.getIntOr("NovaBorrowedToolSlot", 0));
        trees().load(input.childOrEmpty("NovaTrees"));
        companion().memories().load(input.childOrEmpty("NovaCompanion"));
        novaWork.restoreRallyName(this);
        setNovaCarrying(!novaWork.carriedChest().isEmpty());
        lastChoppedRoot = input.read("NovaLastRoot", BlockPos.CODEC).orElse(null);
        ItemStack sapling = input.read("NovaSapling", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        expectedSapling = sapling.isEmpty() ? null : sapling.getItem();
        int defaultMode = isOrderedToSit() ? MODE_WAIT : MODE_FOLLOW;

        entityData.set(NOVA_MOVE_MODE,
                Mth.clamp(input.getIntOr("NovaMoveMode", defaultMode),
                        MODE_FOLLOW, MODE_WAIT));

        entityData.set(NOVA_SIT_REQUESTED, isOrderedToSit());
        novaLastGreetingSession = input.getStringOr("NovaGreetingSession", "");
        novaOwnerWasAway = input.getBooleanOr("NovaOwnerWasAway", false);
        setDormant(input.getBooleanOr("NovaDormant", false));
        setDormantPose(input.getIntOr("NovaDormantPose", 0));
        novaGreetingPending = input.getBooleanOr("NovaGreetingPending", false);
        novaNextGreetingTime = input.getLongOr("NovaNextGreetingTime", 0L);
        novaNextTreeCollectTime = input.getLongOr("NovaNextTreeCollect", 0L);
        novaPreviewFace = input.getIntOr("NovaPreviewFace", -1);
        entityData.set(NOVA_WORK_ENABLED, novaWork.enabled());
    }

    public void startNovaAction(int action) {
        if (level().isClientSide() || isPerformingAction() || isCarryingChest()
                || companion().panicking() || !onGround() || isInWater()) {
            return;
        }

        companion().cancelGesture();
        setCompanionState(NovaCompanion.NORMAL);
        clearWorkAnimation();
        stopRally();
        entityData.set(NOVA_ACTION, action);
        actionTicks = action == ACTION_WAVE ? 100 : 60;
        setNovaRunning(false);
        setInSittingPose(false);
        getNavigation().stop();
        setTarget(null);

        pathClearing().cancel();
        triggerAnim("movement", action == ACTION_WAVE ? "wave" : "celebrate");
        sounds().play(action == ACTION_WAVE ? Cue.GREET : Cue.CELEBRATE);
        if (action == ACTION_WAVE && getOwner() instanceof ServerPlayer owner) {
            novaLastGreetingSession = NovaOwnerSessions.get(owner);
            novaGreetingPending = false;
            novaOwnerWasAway = false;
            novaNextGreetingTime = level().getGameTime() + 600;
        }
    }

    public void startApologizeAction() {
        if (level().isClientSide() || isPerformingAction() || isCarryingChest()
                || companion().panicking() || !onGround() || isInWater()) {
            return;
        }

        companion().cancelGesture();
        setCompanionState(NovaCompanion.NORMAL);
        clearWorkAnimation();
        stopRally();
        showNovaFace(27, 80);
        companion().gesture("nod");
    }

    private void finishNovaAction() {
        actionTicks = 0;
        entityData.set(NOVA_ACTION, ACTION_NONE);
        stopTriggeredAnim("movement", null);}

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            if (isPerformingAction()) {
                // Hủy động tác khi bị đánh, rơi hoặc xuống nước.
                if (--actionTicks <= 0 || hurtTime > 0 || !onGround() || isInWater()) {
                finishNovaAction();

                }
            }

            entityData.set(NOVA_SIT_REQUESTED, isOrderedToSit());

            // isOrderedToSit thuộc AI; tư thế này được đồng bộ sang máy khách.
            setInSittingPose((isOrderedToSit() || getCompanionState()==NovaCompanion.RESTING)
                && !companion().panicking() && !isPerformingAction() && !isRallying()
                && onGround() && !isInWater());
        }

        if (!level().isClientSide() && isAlive() && level() instanceof ServerLevel serverLevel) {
            if (getHealth() / getMaxHealth() <= 0.35F && tickCount % 16 == 0) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.9, getZ(), 3, 0.15, 0.2, 0.15, 0.02);
                if (tickCount % 32 == 0) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 2, 0.15, 0.2, 0.15, 0.05);
                }
            }
        }
        if (isDormant()) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            setYHeadRot(getYRot());
            setYBodyRot(getYRot());
            tickNovaFace();
            super.tick();
            return;
        }

        if (!level().isClientSide() && novaWork.energy() <= 0 && !isDormant()) {
            enterDormantState();
            return;
        }

        pathClearing().maintain();
        companion().tick();
        tickNovaGreeting();
        super.tick();
        if (!level().isClientSide() && novaWork.tickEnergy(canNovaWork() && !pathClearing().busy() && getWorkAction() != WORK_NONE && getWorkAction() != WORK_CHOP)) {
            clearWorkAnimation();
            getNavigation().stop();
        }
        if (tickCount % 10 == 0 && canNovaWork()) NovaWorkSupport.collectNearby(this);
        if (!level().isClientSide() && tickCount % 10 == 0 && isSwordNova() && !novaWork.inventoryOpen()) {
            NovaWorkSupport.ensureOffhandSword(this);
        }
        novaSelfRepair.tick(this);
        sounds().tick();
        tickNovaFace();
        tickNovaLook();
            
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new NovaSafetyGoal(this));
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new NovaWaitGoal(this));
        goalSelector.addGoal(2, new NovaFetchChestGoal(this));
        goalSelector.addGoal(2, new NovaRallyGoal(this));
        goalSelector.addGoal(3, new NovaSwordCombatGoal(this, 1.25));
        goalSelector.addGoal(3, new NovaFollowOwnerGoal(this));
        goalSelector.addGoal(4, new NovaClearPathGoal(this));
        goalSelector.addGoal(4, new NovaStorageGoal(this));
        goalSelector.addGoal(5, new net.teogemini.nova.entity.ai.tree.NovaTreeReplantGoal(this));
        goalSelector.addGoal(5, new NovaOffhandUtilityGoal(this));
        goalSelector.addGoal(6, new NovaMineOreGoal(this, 1.2));
        goalSelector.addGoal(6, new net.teogemini.nova.entity.ai.tree.NovaTreeCollectGoal(this, 1.2));
        goalSelector.addGoal(7, new NovaChopTreeGoal(this, 1.2));
        goalSelector.addGoal(8, new NovaFarmingGoal(this, 1.2));
        goalSelector.addGoal(9, new NovaShovelDigGoal(this, 1.2));
        goalSelector.addGoal(10, new NovaRoutineGoal(this));
        goalSelector.addGoal(10, new NovaSocialGoal(this));
        goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 0.75) {
            @Override public boolean canUse() { return canNovaRoam() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canNovaRoam() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(12, new net.teogemini.nova.entity.ai.NovaLookAtOwnerGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (isDormant()) {
            return interactDormant(player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);
        InteractionResult workResult = NovaWorkSupport.interact(this, player, hand);
        if (workResult != InteractionResult.PASS) return workResult;

        // Đặt tên bằng Thẻ Tên (Name Tag)
        if (stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME)) {
            if (!level().isClientSide()) {
                Component customName = stack.get(DataComponents.CUSTOM_NAME);
                setCustomName(customName);
                setCustomNameVisible(true);
                setPersistenceRequired();
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.displayClientMessage(
                        Component.literal(getNovaDisplayName() + ": Đã nhận tên mới!"),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Shift + tay không: chào. Shift + redstone: ăn mừng.
        if (isTame() && isOwnedBy(player) && player.isShiftKeyDown()
                && (stack.isEmpty() || stack.is(Items.REDSTONE))) {
            if (!level().isClientSide()) {
                if (!onGround() || isInWater() || hurtTime > 0 || novaFearTicks > 0) {
                    player.displayClientMessage(
                            Component.literal(getNovaDisplayName() + ": Đợi mình ổn định lại nhé."), true);
                } else if (isPerformingAction()) {
                    player.displayClientMessage(
                            Component.literal(getNovaDisplayName() + ": Mình đang làm động tác rồi."), true);
                } else {
                    startNovaAction(stack.isEmpty() ? ACTION_WAVE : ACTION_CELEBRATE);
                }
            }

            return InteractionResult.SUCCESS;
        }
        // Shift + mảnh thạch anh tím: thử lần lượt 40 khuôn mặt.
        if (isTame() && isOwnedBy(player) && player.isShiftKeyDown()
                && stack.is(Items.AMETHYST_SHARD)) {
            if (!level().isClientSide()) {
                novaPreviewFace = (novaPreviewFace + 1) % 40;
                showNovaFace(novaPreviewFace, 80);

                player.displayClientMessage(
                        Component.literal(getNovaDisplayName() + ": Biểu cảm " + novaPreviewFace + "/39"),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }

        // NOVA chưa có chủ: dùng redstone hoặc khối redstone để kết nối.
        if (!isTame() && (stack.is(Items.REDSTONE) || stack.is(Items.REDSTONE_BLOCK))) {
            if (!level().isClientSide()) {
                tame(player);
                sounds().play(Cue.TAME);
                companion().memories().meet(player.getName().getString());
                novaGreetingPending = true;
                setOrderedToSit(false);
                getNavigation().stop();

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                spawnTamingParticles(true);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.8, getZ(), 16, 0.35, 0.45, 0.35, 0.08);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 10, 0.3, 0.35, 0.3, 0.02);
                }
                showNovaFace(0, 80);

                player.displayClientMessage(
                        Component.literal(
                                getNovaDisplayName() + ": Đã kết nối! Mình sẽ đi theo bạn."
                        ),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }

        // Nhắc nhở người chơi khi cố cho robot ăn đồ ăn hữu cơ thường
        if (isTame() && stack.has(DataComponents.FOOD)) {
            if (!level().isClientSide()) {
                showNovaFace(6, 50);
                sounds().play(Cue.CURIOUS);
                companion().gesture("nod");
                player.displayClientMessage(
                        Component.literal(getNovaDisplayName() + ": Mình là robot, mình chỉ dùng Redstone để sạc pin và Phôi kim loại (Sắt/Đồng) để sửa vỏ thôi nhé!"),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Tay không + chuột phải: mở túi đồ NOVA (Shift vẫn giữ động tác chào).
        if (isTame() && stack.isEmpty()) {
            if (!isOwnedBy(player)) {
                if (!level().isClientSide()) {
                    player.displayClientMessage(
                            Component.literal(
                                    getNovaDisplayName() + ": Chỉ chủ nhân mới mở được túi đồ."
                            ),
                            true
                    );
                }

                return InteractionResult.FAIL;
            }

            net.teogemini.nova.screen.NovaMenus.open(player, this);
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(
            ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public ItemStack getPickResult() {
        return ItemStack.EMPTY;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<NovaEntity>("movement", 6, state -> {
            if (isDormant()) {
                return state.setAndContinue(RawAnimation.begin());
            }
            if (isCarryingChest()) {
                return state.setAndContinue(state.isMoving() ? CARRY_WALK : CARRY_IDLE);
            }
            if (getWorkAction() != WORK_NONE) {
                return state.setAndContinue(WORK_ANIMATIONS[getWorkAction()]);
            }
            if (isInSittingPose()) {
                return state.setAndContinue(SIT);
            }

            if (!onGround() || isInWater()) {
                return state.setAndContinue(IDLE);
            }

            if (state.isMoving()) {
                return state.setAndContinue(isNovaRunning() ? RUN : WALK);
            }

            return state.setAndContinue(IDLE);
        }).triggerableAnim("wave", WAVE)
          .triggerableAnim("celebrate", CELEBRATE));
        NovaCompanionAnimations.register(this, controllers);
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public void enterDormantState() {
        setDormant(true);
        setDormantPose(0);
        clearWorkAnimation();
        getNavigation().stop();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.8, getZ(), 20, 0.3, 0.4, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.6, getZ(), 10, 0.25, 0.3, 0.25, 0.05);
        }
        sounds().play(Cue.SLEEP);
        if (getOwner() instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("[" + getNovaDisplayName() + "] Đã cạn sạch năng lượng (0%) và tự động chuyển sang trạng thái Chưa Kích Hoạt! Hãy dùng Khối Đá Đỏ để kích hoạt lại.")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
        }
    }

    public void dropDormantItem(Player player) {
        if (level() instanceof ServerLevel serverLevel) {
            ItemStack item = new ItemStack(net.teogemini.nova.registry.ModItems.NOVA_DORMANT);
            net.minecraft.world.level.storage.TagValueOutput output =
                    net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                            net.minecraft.util.ProblemReporter.DISCARDING, serverLevel.registryAccess());
            saveWithoutId(output);
            net.minecraft.nbt.CompoundTag tag = output.buildResult();

            item.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            if (hasCustomName()) {
                item.set(DataComponents.CUSTOM_NAME, getCustomName());
            }

            if (player != null && player.addItem(item)) {
                serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
            } else {
                spawnAtLocation(serverLevel, item);
            }
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 10, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private InteractionResult interactDormant(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        // 1. KÍCH HOẠT BẰNG KHỐI ĐÁ ĐỎ
        if (held.is(Items.REDSTONE_BLOCK)) {
            if (!level().isClientSide()) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                work().setEnergy(NovaWorkData.MAX_ENERGY);
                heal(getMaxHealth());
                setDormant(false);
                if (!isTame()) {
                    tame(player);
                }
                showNovaFace(0, 60);
                companion().gesture("nod");
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.6, getZ(), 35, 0.4, 0.5, 0.4, 0.1);
                    serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.2, getZ(), 12, 0.3, 0.3, 0.3, 0.05);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.8, getZ(), 16, 0.4, 0.4, 0.4, 0.05);
                }
                sounds().play(Cue.BOOT);
                player.displayClientMessage(
                        Component.literal("[" + getNovaDisplayName() + "] Đã nạp đầy năng lượng từ Khối Đá Đỏ và khởi động thành công!")
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // 2. SHIFT + TAY KHÔNG: THU HỒI
        if (player.isShiftKeyDown() && held.isEmpty() && (isOwnedBy(player) || !isTame() || player.getAbilities().instabuild)) {
            if (!level().isClientSide()) {
                dropDormantItem(player);
                discard();
                player.displayClientMessage(
                        Component.literal("[" + getNovaDisplayName() + "] Đã thu hồi vào túi đồ.")
                                .withStyle(ChatFormatting.YELLOW),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // 3. NHẤP PHẢI ĐỔI 5 TƯ THẾ
        if (!level().isClientSide()) {
            int nextPose = (getDormantPose() + 1) % 5;
            setDormantPose(nextPose);
            tickNovaFace();
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F + nextPose * 0.1F);

            String poseName = switch (nextPose) {
                case 0 -> "Tắt nguồn ngủ đông (1/5)";
                case 1 -> "Đứng nghiêm canh gác (2/5)";
                case 2 -> "Ngồi gập gối chờ lệnh (3/5)";
                case 3 -> "Giơ tay chào (4/5)";
                case 4 -> "Chiến binh thủ thế (5/5)";
                default -> "Chưa kích hoạt";
            };
            player.displayClientMessage(
                    Component.literal("[" + getNovaDisplayName() + " - Chưa Kích Hoạt] Tư thế: " + poseName)
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

}
