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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.teogemini.nova.entity.NovaEntity;

public class NovaMineOreGoal extends Goal {
    private int novaTaskTicks;
    @Override public boolean requiresUpdateEveryTick() { return true; }

    private final NovaEntity nova;
    private final double speed;
    private BlockPos targetOrePos;      // Mục tiêu chót (Cục Quặng)
    private BlockPos currentMiningPos;  // Mục tiêu hiện tại (Cục Đá/Đất đang cản đường)
    private int miningTick;
    private int searchDelay;

    public NovaMineOreGoal(NovaEntity nova, double speed) {
        this.nova = nova;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!nova.canNovaWork()) return false;
        if (this.searchDelay > 0) {
            this.searchDelay--;
            return false;
        }
        this.searchDelay = 40; // Quét radar 2 giây 1 lần

        if (!this.nova.getMainHandItem().is(ItemTags.PICKAXES)) {
            return false;
        }

        this.targetOrePos = findNearestOre();
        return this.targetOrePos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!nova.canNovaWork()) return false;
        return this.targetOrePos != null 
            && this.nova.getMainHandItem().is(ItemTags.PICKAXES);
    }

    @Override
    public void start() {
        novaTaskTicks = 0;
        this.miningTick = 0;
        this.currentMiningPos = null;
    }

    @Override
    public void tick() {
        if (!nova.canNovaWork() || ++novaTaskTicks > 400) { stop(); return; }
        nova.clearWorkAnimation();
        if (this.targetOrePos == null) return;

        // BẢO HIỂM: Nhỡ người chơi đập mất cục quặng trước khi Nova tới thì hủy mục tiêu
        if (!isOre(this.nova.level().getBlockState(this.targetOrePos).getBlock())) {
            this.targetOrePos = null;
            return;
        }

        // 1. BẮN TIA LAZE DÒ ĐƯỜNG
        Vec3 startRay = this.nova.getEyePosition(); 
        Vec3 endRay = Vec3.atCenterOf(this.targetOrePos); 
        
        BlockHitResult hit = this.nova.level().clip(new ClipContext(
            startRay, endRay, 
            ClipContext.Block.COLLIDER, 
            ClipContext.Fluid.NONE, 
            this.nova
        ));

        // 2. XÁC ĐỊNH MỤC TIÊU ĐẬP
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hit.getBlockPos();
            Block hitBlock = this.nova.level().getBlockState(hitPos).getBlock();

            if (isOre(hitBlock) || isObstacle(hitBlock)) {
                this.currentMiningPos = hitPos;
            } else {
                this.targetOrePos = null; // Đụng trúng khối không thể phá thì bỏ qua
                return;
            }
        } else {
            this.currentMiningPos = this.targetOrePos;
        }

        if (this.currentMiningPos == null) return;

        // 3. THỰC THI HÀNH ĐỘNG ĐẬP
        this.nova.getLookControl().setLookAt(
            this.currentMiningPos.getX() + 0.5, 
            this.currentMiningPos.getY() + 0.5, 
            this.currentMiningPos.getZ() + 0.5, 
            10.0F, 
            this.nova.getMaxHeadXRot()
        );

        double distance = this.nova.blockPosition().distSqr(this.currentMiningPos);
        
        // Đã áp sát (Cách khoảng 2 block)
        if (distance <= 4.0) {
            this.nova.getNavigation().stop();
            NovaWorkSupport.faceTarget(nova, currentMiningPos);
            nova.playWorkAnimation(2);
            
            if (this.miningTick % 10 == 0) { 
                this.nova.swing(InteractionHand.MAIN_HAND);
            }
            
            this.miningTick++;
            
            // Đập vỡ khối sau 20 ticks (1 giây)
            if (this.miningTick >= 20) {
                BlockState novaOriginalBlock = nova.level().getBlockState(this.currentMiningPos);
                if (!NovaWorkSupport.breakBlock(nova, this.currentMiningPos)) { stop(); return; }
                NovaExperienceManager.awardMiningExp(nova, novaOriginalBlock); // Đập vỡ rớt đồ
                this.miningTick = 0; // Reset lực
                
                // NẾU VỪA ĐẬP XONG CỤC QUẶNG -> QUÉT MẠCH (VEIN MINING)
                if (this.currentMiningPos.equals(this.targetOrePos)) {
                    BlockPos adjacentOre = findAdjacentOre(this.targetOrePos);
                    
                    if (adjacentOre != null) {
                        this.targetOrePos = adjacentOre; // Nối mạch! Nhắm đập cục tiếp theo luôn
                    } else {
                        this.targetOrePos = null; // Cả mạch đã cạn kiệt, xóa mục tiêu để quét radar lại
                    }
                }
                
                // Xóa khối đang đập để tick sau nó bắn tia laze kiểm tra lại
                this.currentMiningPos = null; 
            }
        } else {
            // Chạy lại gần
            this.nova.getNavigation().moveTo(
                this.currentMiningPos.getX() + 0.5, 
                this.currentMiningPos.getY(), 
                this.currentMiningPos.getZ() + 0.5, 
                this.speed
            );
        }
    }

    // TÌM MẠCH QUẶNG LIỀN KỀ (Bán kính 3 block quanh cục vừa đập)
    private BlockPos findAdjacentOre(BlockPos center) {
        // Quét cục bộ xung quanh cục quặng vừa đào
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Bỏ qua vị trí trung tâm (đã đập)
                    
                    BlockPos checkPos = center.offset(x, y, z);
                    if (!nova.isWithinWorkspace(checkPos)) continue;
                    Block block = this.nova.level().getBlockState(checkPos).getBlock();
                    
                    if (isOre(block)) {
                        return checkPos; // Có quặng nối đuôi! Trả về tọa độ cục này ngay
                    }
                }
            }
        }
        return null; // Không thấy cục nào dính chung mạch
    }

    // QUÉT RADAR DIỆN RỘNG CHỐNG LAG (Bán kính 64 block)
    private BlockPos findNearestOre() {
        Optional<BlockPos> closestOre = BlockPos.findClosestMatch(
            this.nova.blockPosition(), 
            16, 
            9, 
            pos -> nova.isWithinWorkspace(pos) && isOre(this.nova.level().getBlockState(pos).getBlock())
        );
        return closestOre.orElse(null);
    }

    // QUẶNG MỤC TIÊU
    private boolean isOre(Block block) {
        return block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE || 
               block == Blocks.COAL_ORE || block == Blocks.IRON_ORE || 
               block == Blocks.GOLD_ORE || block == Blocks.DIAMOND_ORE ||
               block == Blocks.LAPIS_ORE || block == Blocks.REDSTONE_ORE;
    }

    // ĐÁ CẢN ĐƯỜNG (Mở đường hầm)
    private boolean isObstacle(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.DEEPSLATE ||
               block == Blocks.GRAVEL || block == Blocks.ANDESITE || block == Blocks.DIORITE ||
               block == Blocks.GRANITE || block == Blocks.TUFF || block == Blocks.COBBLESTONE;
    }

    @Override public void stop() {
        targetOrePos = null;
        nova.clearWorkAnimation();
        nova.getNavigation().stop();
    }
}
