package net.teogemini.nova.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.teogemini.nova.entity.ai.NovaFollowOwnerGoal;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.gamerules.GameRules;
import net.teogemini.nova.entity.ai.NovaRallyGoal;
import net.teogemini.nova.entity.ai.work.*;

public class NovaEntity extends TamableAnimal implements GeoEntity {
    public static final int WORK_NONE = 0, WORK_CHOP = 1, WORK_MINE = 2,
            WORK_FARM = 3, WORK_DIG = 4, WORK_PLANT = 5, WORK_CHEST = 6;
    private static final EntityDataAccessor<Integer> NOVA_WORK =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NOVA_CARRYING =
            SynchedEntityData.defineId(NovaEntity.class, EntityDataSerializers.BOOLEAN);
    private final NovaWorkData novaWork = new NovaWorkData();
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
    public boolean isWithinWorkspace(BlockPos pos) {
        BlockPos chest = getLinkedChest();
        return chest != null && chest.distSqr(pos) <= 256.0
                && level().hasChunkAt(pos) && level().getWorldBorder().isWithinBounds(pos);
    }
    public boolean canNovaWork() {
        BlockPos chest = getLinkedChest();
        return level() instanceof ServerLevel server && isAlive() && !isNoAi() && isTame()
                && novaWork.enabled() && novaWork.energy() > 0 && !novaWork.inventoryOpen()
                && getNovaMoveMode() == MODE_FREE
                && !isOrderedToSit() && !isPerformingAction() && !isRallying()
                && !isCarryingChest() && chest != null && level().hasChunkAt(chest)
                && level().getBlockState(chest).is(net.teogemini.nova.registry.ModBlocks.NOVA_CHEST)
                && server.getGameRules().get(GameRules.MOB_GRIEFING);
    }
    public void addExperience(int amount) {
        if (!level().isClientSide() && novaWork.addExperience(amount)) {
            playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.5F, 1.3F);
        }
    }
    public int getWorkAction() { return entityData.get(NOVA_WORK); }
    public void playWorkAnimation(int action) {
        if (!level().isClientSide()) entityData.set(NOVA_WORK, Mth.clamp(action, 0, 6));
    }
    public void clearWorkAnimation() { playWorkAnimation(WORK_NONE); }
    public boolean isCarryingChest() { return entityData.get(NOVA_CARRYING); }
    public void setNovaCarrying(boolean carrying) {
        if (!level().isClientSide()) entityData.set(NOVA_CARRYING, carrying);
    }
    public boolean isRallying() { return novaWork.rallyTarget != null; }
    public void startRally(BlockPos target, BlockPos face) {
        if (level().isClientSide()) return;
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
        super.dropCustomDeathLoot(level, source, recentlyHit);
        NovaWorkSupport.dropCargo(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(NOVA_ACTION, ACTION_NONE);
            builder.define(NOVA_RUNNING, false);
            builder.define(NOVA_FACE, 0);
            builder.define(NOVA_WORK, WORK_NONE);
            builder.define(NOVA_CARRYING, false);
            builder.define(NOVA_MOVE_MODE, MODE_FOLLOW);
            builder.define(NOVA_SIT_REQUESTED, false);
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

    private void tickNovaFace() {
        if (level().isClientSide()) {
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

        // Biểu cảm được yêu cầu bằng code hoặc chế độ xem thử.
        if (novaRequestedTicks > 0) {
            --novaRequestedTicks;
            entityData.set(NOVA_FACE, novaRequestedFace);
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
            novaIdleFace = random.nextInt(8) == 0
                    ? 39
                    : IDLE_FACES[random.nextInt(IDLE_FACES.length)];

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
        return (!isTame() || getNovaMoveMode() == MODE_FREE)
                && !isOrderedToSit() && !isPerformingAction() && !isRallying();
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
        output.storeNullable("NovaLastRoot", BlockPos.CODEC, lastChoppedRoot);
        output.store("NovaSapling", ItemStack.OPTIONAL_CODEC, expectedSapling == null
                ? ItemStack.EMPTY : new ItemStack(expectedSapling));
        output.putInt("NovaMoveMode", getNovaMoveMode());
        output.putString("NovaGreetingSession", novaLastGreetingSession);
        output.putBoolean("NovaOwnerWasAway", novaOwnerWasAway);
        output.putBoolean("NovaGreetingPending", novaGreetingPending);
        output.putLong("NovaNextGreetingTime", novaNextGreetingTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        novaWork.load(input.childOrEmpty("NovaWork"));
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
        novaGreetingPending = input.getBooleanOr("NovaGreetingPending", false);
        novaNextGreetingTime = input.getLongOr("NovaNextGreetingTime", 0L);
    }

    private void startNovaAction(int action) {
        if (level().isClientSide() || isPerformingAction() || isCarryingChest()
                || !onGround() || isInWater()) {
            return;
        }

        clearWorkAnimation();
        stopRally();
        entityData.set(NOVA_ACTION, action);
        actionTicks = action == ACTION_WAVE ? 100 : 60;
        setNovaRunning(false);
        setInSittingPose(false);
        getNavigation().stop();
        setTarget(null);

        triggerAnim("movement", action == ACTION_WAVE ? "wave" : "celebrate");
        if (action == ACTION_WAVE && getOwner() instanceof ServerPlayer owner) {
            novaLastGreetingSession = NovaOwnerSessions.get(owner);
            novaGreetingPending = false;
            novaOwnerWasAway = false;
            novaNextGreetingTime = level().getGameTime() + 600;
        }

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
            setInSittingPose(isOrderedToSit() && !isPerformingAction() && !isRallying()
                && onGround() && !isInWater());
        }

        tickNovaGreeting();
        super.tick();
        if (!level().isClientSide() && novaWork.tickEnergy(canNovaWork() && getWorkAction() != WORK_NONE)) {
            clearWorkAnimation();
            getNavigation().stop();
        }
        if (tickCount % 10 == 0 && canNovaWork()) NovaWorkSupport.collectNearby(this);
        tickNovaFace();
        tickNovaLook();
            
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new NovaWaitGoal(this));
        goalSelector.addGoal(2, new NovaRallyGoal(this));
        goalSelector.addGoal(3, new NovaFollowOwnerGoal(this));
        goalSelector.addGoal(4, new NovaChestLogisticsGoal(this));
        goalSelector.addGoal(5, new NovaOffhandUtilityGoal(this));
        goalSelector.addGoal(6, new NovaMineOreGoal(this, 1.2));
        goalSelector.addGoal(7, new NovaChopTreeGoal(this, 1.2));
        goalSelector.addGoal(8, new NovaFarmingGoal(this, 1.2));
        goalSelector.addGoal(9, new NovaShovelDigGoal(this, 1.2));
        goalSelector.addGoal(11, new WaterAvoidingRandomStrollGoal(this, 0.75) {
            @Override public boolean canUse() { return canNovaRoam() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canNovaRoam() && super.canContinueToUse(); }
        });
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        InteractionResult workResult = NovaWorkSupport.interact(this, player, hand);
        if (workResult != InteractionResult.PASS) return workResult;
                // Shift + tay không: chào. Shift + redstone: ăn mừng.
        if (isTame() && isOwnedBy(player) && player.isShiftKeyDown()
                && (stack.isEmpty() || stack.is(Items.REDSTONE))) {
            if (!level().isClientSide()) {
                if (!onGround() || isInWater() || hurtTime > 0 || novaFearTicks > 0) {
                    player.displayClientMessage(
                            Component.literal("NOVA: Đợi mình ổn định lại nhé."), true);
                } else if (isPerformingAction()) {
                    player.displayClientMessage(
                            Component.literal("NOVA: Mình đang làm động tác rồi."), true);
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
                        Component.literal("NOVA: Biểu cảm " + novaPreviewFace + "/39"),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }

        // NOVA chưa có chủ: dùng redstone để kết nối.
        if (!isTame() && stack.is(Items.REDSTONE)) {
            if (!level().isClientSide()) {
                tame(player);
                novaGreetingPending = true;
                setOrderedToSit(false);
                getNavigation().stop();

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                player.displayClientMessage(
                        Component.literal(
                                "NOVA: Đã kết nối! Mình sẽ đi theo bạn."
                        ),
                        true
                );
            }

            return InteractionResult.SUCCESS;
        }

        // Chủ nhân dùng tay không để đổi chế độ.
        if (isTame() && stack.isEmpty()) {
            if (!isOwnedBy(player)) {
                if (!level().isClientSide()) {
                    player.displayClientMessage(
                            Component.literal(
                                    "NOVA: Chỉ chủ nhân mới đổi được chế độ."
                            ),
                            true
                    );
                }

                return InteractionResult.FAIL;
            }

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
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<NovaEntity>("movement", 6, state -> {
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
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}