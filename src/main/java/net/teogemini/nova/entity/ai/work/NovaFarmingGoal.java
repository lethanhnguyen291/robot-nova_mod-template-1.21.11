package net.teogemini.nova.entity.ai.work;

import java.util.EnumSet;
import net.teogemini.nova.entity.NovaExperienceManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.ItemTags;
import net.teogemini.nova.entity.NovaWorkSupport;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.CropBlock;
import net.teogemini.nova.entity.NovaEntity;

public class NovaFarmingGoal extends Goal {
    private int novaTaskTicks;
    @Override public boolean requiresUpdateEveryTick() { return true; }

    private final NovaEntity nova;
    private final double speed;
    private BlockPos targetFarmPos;
    private int actionTick;
    private int searchDelay;

    public NovaFarmingGoal(NovaEntity nova, double speed) {
        this.nova = nova;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!nova.canStartNovaJob()) return false;
        if (!nova.companion().usableTool(nova.getPrimaryTool())) {
            NovaWorkSupport.swapToolFromBody(nova);
        }
        if (!nova.companion().usableTool(nova.getPrimaryTool())) return false;
        if (this.searchDelay > 0) {
            this.searchDelay--;
            return false;
        }
        this.searchDelay = 30; // Quét 1.5 giây 1 lần

        // ĐIỀU KIỆN: Phải cầm CUỐC (HoeItem) mới ra đồng!
        if (!(this.nova.getPrimaryTool().is(ItemTags.HOES))) {
            return false;
        }

        this.targetFarmPos = findNearestFarmBlock();
        return this.targetFarmPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!nova.canStartNovaJob()) return false;
        if (!nova.companion().usableTool(nova.getPrimaryTool())) {
            NovaWorkSupport.swapToolFromBody(nova);
        }
        if (!nova.companion().usableTool(nova.getPrimaryTool())) return false;
        return this.targetFarmPos != null 
            && this.nova.getPrimaryTool().is(ItemTags.HOES);
    }

    @Override
    public void start() {
        novaTaskTicks = 0;
        this.actionTick = 0;
    }

    @Override
    public void tick() {
        if (!nova.canStartNovaJob()) { stop(); return; }
        if (++novaTaskTicks > 200) { nova.companion().stuck(targetFarmPos); stop(); return; }
        nova.clearWorkAnimation();
        if (this.targetFarmPos == null) return;

        // Bảo hiểm: Nhỡ ruộng bị dẫm hỏng hoặc lúa bị thu hoạch mất
        if (!isMatureCrop(this.targetFarmPos)) {
            this.targetFarmPos = null;
            return;
        }

        // Nhìn chằm chằm vào gốc lúa
        this.nova.getLookControl().setLookAt(
                this.targetFarmPos.getX() + 0.5,
                this.targetFarmPos.getY(),
                this.targetFarmPos.getZ() + 0.5,
                10.0F,
                this.nova.getMaxHeadXRot()
        );

        if (nova.getEyePosition().distanceToSqr(Vec3.atCenterOf(targetFarmPos)) <= 12.25
                && !NovaWorkSupport.canReach(nova, targetFarmPos)) {
            nova.companion().stuck(targetFarmPos); stop(); return;
        }
        double distance = this.nova.blockPosition().distSqr(this.targetFarmPos);
        
        // Khoảng cách <= 4.0 là đủ để vươn tay cuốc đất
        if (distance <= 4.0) {
            this.nova.getNavigation().stop();
            NovaWorkSupport.faceTarget(nova, targetFarmPos);
            nova.playWorkAnimation(3);
            
            // Cứ 5 ticks vung tay 1 lần
            if (this.actionTick % 5 == 0) { 
                this.nova.swing(InteractionHand.MAIN_HAND);
            }
            
            this.actionTick++;
            
            // Thực hiện hành động sau 15 ticks
            if (this.actionTick >= 15) {
                if (isMatureCrop(this.targetFarmPos)) {
                    BlockState novaOriginalBlock = nova.level().getBlockState(this.targetFarmPos);
                if (!NovaWorkSupport.breakBlock(nova, this.targetFarmPos)) { stop(); return; }
                NovaExperienceManager.awardFarmerExp(nova, true);
                }  
                this.targetFarmPos = null;
                this.actionTick = 0; 
            }
        } else {
            // Chạy ra ruộng
            if (novaTaskTicks % 20 == 1) NovaWorkSupport.moveToWork(nova, targetFarmPos, speed);
        }
    }

    // QUÉT RADAR TÌM RUỘNG LÚA (Bán kính 16 block)
    private BlockPos findNearestFarmBlock() {
        Optional<BlockPos> closestBlock = BlockPos.findClosestMatch(
            this.nova.blockPosition(), 
            16,
            3,
            this::isMatureCrop
        );
        return closestBlock.orElse(null);
    }

    // ĐIỀU KIỆN 1: TÌM LÚA ĐÃ CHÍN
    private boolean isMatureCrop(BlockPos pos) {
        if (!nova.isWithinWorkspace(pos) || nova.companion().retry.blocked(pos,nova.level().getGameTime())) return false;
        BlockState state = this.nova.level().getBlockState(pos);
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state); 
        }
        return false;
    }

    @Override public void stop() { nova.pathClearing().track(null);
        targetFarmPos = null;
        nova.clearWorkAnimation();
        nova.getNavigation().stop();
    }
}
