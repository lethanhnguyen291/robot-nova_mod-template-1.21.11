package net.teogemini.nova.entity.ai.work;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaExperienceManager;

public final class NovaOffhandUtilityGoal extends Goal {
    private final NovaEntity nova;
    private BlockPos target;
    private ItemStack supply = ItemStack.EMPTY;
    private int cooldown, ticks, actionTicks;

    public NovaOffhandUtilityGoal(NovaEntity nova) {
        this.nova = nova;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!nova.canNovaWork() || --cooldown > 0) return false;
        cooldown = 20;
        supply = nova.getOffhandItem().copy();
        if (supply.isEmpty()) return false;
        target = BlockPos.findClosestMatch(nova.blockPosition(), 4, 2, this::valid)
                .map(BlockPos::immutable).orElse(null);
        return target != null;
    }
    @Override public boolean canContinueToUse() {
        return nova.canNovaWork() && target != null && ticks < 200
                && ItemStack.isSameItemSameComponents(supply, nova.getOffhandItem()) && valid(target);
    }
    @Override public void start() { ticks = 0; actionTicks = 0; }

    private BlockState plantedState() {
        if (supply.is(Items.WHEAT_SEEDS)) return Blocks.WHEAT.defaultBlockState();
        if (supply.is(Items.BEETROOT_SEEDS)) return Blocks.BEETROOTS.defaultBlockState();
        if (supply.is(Items.CARROT)) return Blocks.CARROTS.defaultBlockState();
        if (supply.is(Items.POTATO)) return Blocks.POTATOES.defaultBlockState();
        if ((supply.is(ItemTags.SAPLINGS) || supply.is(Items.TORCH))
                && supply.getItem() instanceof BlockItem block) return block.getBlock().defaultBlockState();
        return null;
    }

    private boolean valid(BlockPos pos) {
        if (!nova.isWithinWorkspace(pos) || !nova.level().getBlockState(pos).isAir()) return false;
        BlockState state = plantedState();
        if (state == null || !state.canSurvive(nova.level(), pos)) return false;
        if (supply.is(ItemTags.SAPLINGS)) {
            return pos.equals(nova.lastChoppedRoot)
                    && (nova.expectedSapling == null || supply.is(nova.expectedSapling));
        }
        if (supply.is(Items.TORCH)) return nova.level().getBrightness(LightLayer.BLOCK, pos) < 7
                && nova.level().getBlockState(pos.below()).isFaceSturdy(nova.level(), pos.below(), Direction.UP);
        return nova.level().getBlockState(pos.below()).is(Blocks.FARMLAND);
    }

    @Override public void tick() {
        ticks++;
        if (!canContinueToUse()) { stop(); return; }
        nova.getLookControl().setLookAt(target.getX() + .5, target.getY() + .5, target.getZ() + .5);
        if (nova.getEyePosition().distanceToSqr(Vec3.atCenterOf(target)) > 9) {
            nova.clearWorkAnimation();
            if (ticks % 10 == 1) nova.getNavigation().moveTo(target.getX() + .5, target.getY(), target.getZ() + .5, 1.0);
            return;
        }
        nova.getNavigation().stop();
        if (!net.teogemini.nova.entity.NovaWorkSupport.canReach(nova, target)) { stop(); return; }
        net.teogemini.nova.entity.NovaWorkSupport.faceTarget(nova, target);
        nova.playWorkAnimation(NovaEntity.WORK_PLANT);
        if (++actionTicks < 16) return;
        BlockState state = plantedState();
        if (state != null && valid(target) && nova.level().setBlockAndUpdate(target, state)) {
            nova.getOffhandItem().shrink(1);
            nova.swing(InteractionHand.OFF_HAND);
            NovaExperienceManager.awardFarmerExp(nova, false);
            if (target.equals(nova.lastChoppedRoot)) {
                nova.lastChoppedRoot = null;
                nova.expectedSapling = null;
            }
        }
        stop();
    }
    @Override public void stop() {
        target = null;
        nova.getNavigation().stop();
        nova.clearWorkAnimation();
    }
}
