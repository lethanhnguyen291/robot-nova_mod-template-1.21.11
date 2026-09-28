package net.teogemini.nova.entity.ai.work;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaExperienceManager;
import net.teogemini.nova.entity.NovaWorkSupport;

public class NovaChopTreeGoal extends Goal {
    
    // BỘ NHỚ TOÀN CẦU (Dùng chung cho mọi Nova)
    public static final Map<NovaEntity, BlockPos> CLAIMED_TREES = new WeakHashMap<>();

    private final NovaEntity nova;
    private final double speed;
    private BlockPos targetLogPos;
    private int choppingTick;
    private int searchDelay;
    private int stuckTimer; 

    public NovaChopTreeGoal(NovaEntity nova, double speed) {
        this.nova = nova;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (!nova.canNovaWork()) return false;
        if (this.searchDelay > 0) {
            this.searchDelay--;
            return false;
        }
        // Thêm độ nảy random (40-80 ticks) để các Nova không quét vào đúng 1 tích tắc
        this.searchDelay = 40 + this.nova.getRandom().nextInt(40); 

        // Phải cầm Rìu mới đi chặt
        if (!this.nova.getMainHandItem().is(ItemTags.AXES)) {
            return false;
        }

        this.targetLogPos = findNearestLog();
        if (this.targetLogPos != null) {
            CLAIMED_TREES.put(this.nova, this.targetLogPos);
            return true;
        }
        
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetLogPos != null 
            && this.nova.getMainHandItem().is(ItemTags.AXES)
            && this.nova.canNovaWork();
    }

    @Override
    public void start() {
        this.choppingTick = 0;
        this.stuckTimer = 0;
    }

    @Override
    public void stop() {
        CLAIMED_TREES.remove(this.nova);
        this.targetLogPos = null;
    }

    @Override
    public void tick() {
        if (this.targetLogPos == null) return;

        // Nếu khúc gỗ bị đập mất bởi người chơi hoặc con khác, bỏ mục tiêu
        if (!isLog(this.nova.level().getBlockState(this.targetLogPos).getBlock())) {
            this.targetLogPos = null;
            return;
        }

        // Tọa độ tâm của khối gỗ để tính toán khoảng cách ngang
        double targetX = this.targetLogPos.getX() + 0.5;
        double targetZ = this.targetLogPos.getZ() + 0.5;
        
        double horizontalDistSq = this.nova.distanceToSqr(targetX, this.nova.getY(), targetZ);

        // Khóa ánh nhìn vào khúc gỗ
        this.nova.getLookControl().setLookAt(targetX, this.targetLogPos.getY() + 0.5, targetZ, 30.0F, 30.0F);

        // ÉP GÓC ĐỨNG SÁT GỐC (horizontalDistSq <= 3.0)
        if (horizontalDistSq > 3.0) {
            this.nova.getNavigation().moveTo(targetX, this.targetLogPos.getY(), targetZ, this.speed);
            
            // HỆ THỐNG CHỐNG KẸT
            this.stuckTimer++;
            if (this.stuckTimer > 150) { 
                this.targetLogPos = null; 
            }
        } else {
            // ĐÃ ĐỨNG SÁT GỐC CÂY -> DỪNG LẠI VÀ BẮT ĐẦU CHẶT
            this.nova.getNavigation().stop();
            this.stuckTimer = 0; 
            NovaWorkSupport.faceTarget(nova, targetLogPos);
            nova.playWorkAnimation(1); // 1 = chop animation
            
            if (this.choppingTick % 10 == 0) { 
                this.nova.swing(InteractionHand.MAIN_HAND);
            }
            
            this.choppingTick++;
            
            if (this.choppingTick >= 40) {
                // Kiểm tra gốc cây
                BlockState belowState = this.nova.level().getBlockState(this.targetLogPos.below());
                boolean isRoot = belowState.is(Blocks.DIRT) || belowState.is(Blocks.GRASS_BLOCK) || belowState.is(Blocks.PODZOL);

                if (isRoot) {
                    this.nova.lastChoppedRoot = this.targetLogPos;
                    Block choppedBlock = this.nova.level().getBlockState(this.targetLogPos).getBlock();
                    this.nova.expectedSapling = getSaplingForLog(choppedBlock);
                }

                // Vung rìu đập gỗ
                NovaWorkSupport.breakBlock(nova, this.targetLogPos);
                NovaExperienceManager.awardLumberjackExp(this.nova); 
                this.choppingTick = 0; 
                
                // DÒ TÌM NHÁNH CÂY TIẾP THEO
                this.targetLogPos = findNextLogToChop(this.targetLogPos);
                
                if (this.targetLogPos != null) {
                    CLAIMED_TREES.put(this.nova, this.targetLogPos);
                }
            }
        }
    }

    // QUÉT TÌM GỐC CÂY
    private BlockPos findNearestLog() {
        Optional<BlockPos> closestLog = BlockPos.findClosestMatch(
            this.nova.blockPosition(), 
            32, 
            16, 
            pos -> {
                if (!isLog(this.nova.level().getBlockState(pos).getBlock())) return false;
                
                BlockPos base = pos;
                while (isLog(this.nova.level().getBlockState(base.below()).getBlock())) {
                    base = base.below();
                }
                
                // Kiểm tra xem đã có Nova nào nhận cây này chưa
                for (Map.Entry<NovaEntity, BlockPos> entry : CLAIMED_TREES.entrySet()) {
                    NovaEntity other = entry.getKey();
                    BlockPos claimedPos = entry.getValue();
                    
                    if (other != this.nova && other.isAlive() && claimedPos != null) {
                        if (claimedPos.closerThan(base, 2.0)) {
                            return false;
                        }
                    }
                }

                // Kiểm tra chiều cao cây (Loại bỏ các cây quá 10 block)
                int height = 0;
                BlockPos checkHeightPos = base;
                while (isLog(this.nova.level().getBlockState(checkHeightPos).getBlock())) {
                    height++;
                    if (height > 10) return false; 
                    checkHeightPos = checkHeightPos.above();
                }
                
                return true; 
            }
        );
        
        if (closestLog.isPresent()) {
            BlockPos basePos = closestLog.get();
            while (isLog(this.nova.level().getBlockState(basePos.below()).getBlock())) {
                basePos = basePos.below();
            }
            return basePos;
        }
        
        return null;
    }

    // HÀM TÌM NHÁNH CÂY
    private BlockPos findNextLogToChop(BlockPos currentPos) {
        if (isLog(this.nova.level().getBlockState(currentPos.above()).getBlock())) {
            return currentPos.above();
        }
        for (BlockPos pos : BlockPos.betweenClosed(currentPos.offset(-1, 0, -1), currentPos.offset(1, 1, 1))) {
            if (isLog(this.nova.level().getBlockState(pos).getBlock())) {
                return pos;
            }
        }
        return null; 
    }

    private boolean isLog(Block block) {
        return block.defaultBlockState().is(BlockTags.LOGS);
    }

    // HÀM CHUYỂN ĐỔI GỖ -> MẦM CÂY
    private Item getSaplingForLog(Block logBlock) {
        if (logBlock == Blocks.OAK_LOG) return Items.OAK_SAPLING;
        if (logBlock == Blocks.SPRUCE_LOG) return Items.SPRUCE_SAPLING;
        if (logBlock == Blocks.BIRCH_LOG) return Items.BIRCH_SAPLING;
        if (logBlock == Blocks.JUNGLE_LOG) return Items.JUNGLE_SAPLING;
        if (logBlock == Blocks.ACACIA_LOG) return Items.ACACIA_SAPLING;
        if (logBlock == Blocks.DARK_OAK_LOG) return Items.DARK_OAK_SAPLING;
        if (logBlock == Blocks.MANGROVE_LOG) return Items.MANGROVE_PROPAGULE;
        if (logBlock == Blocks.CHERRY_LOG) return Items.CHERRY_SAPLING;
        return Items.OAK_SAPLING;
    }
}