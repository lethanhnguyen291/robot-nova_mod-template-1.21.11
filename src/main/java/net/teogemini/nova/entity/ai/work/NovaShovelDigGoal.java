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
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Blocks;
import net.teogemini.nova.entity.NovaEntity;

public class NovaShovelDigGoal extends Goal {
    private int novaTaskTicks;
    @Override public boolean requiresUpdateEveryTick() { return true; }

    private final NovaEntity nova;
    private final double speed;
    private BlockPos targetDigPos;
    
    // Điểm Neo (Tâm của khu vực 5x5)
    private BlockPos anchorPos; 
    
    private int actionTick;
    private int searchDelay;

    public NovaShovelDigGoal(NovaEntity nova, double speed) {
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
        this.searchDelay = 30; // Quét 1.5 giây 1 lần

        // ĐIỀU KIỆN: Phải cầm XẺNG (ShovelItem) mới làm việc!
        if (!(this.nova.getMainHandItem().is(ItemTags.SHOVELS))) {
            this.anchorPos = null; // Mất xẻng là quên Điểm Neo luôn
            return false;
        }

        // THIẾT LẬP TÂM KHU VỰC 5x5: Khi vừa cầm xẻng, đứng đâu thì lấy đó làm Điểm Neo
        if (this.anchorPos == null) {
            this.anchorPos = this.nova.blockPosition();
        }

        this.targetDigPos = findNearestDigBlock();
        return this.targetDigPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!nova.canNovaWork()) return false;
        return this.targetDigPos != null 
            && this.nova.getMainHandItem().is(ItemTags.SHOVELS);
    }

    @Override
    public void start() {
        novaTaskTicks = 0;
        this.actionTick = 0;
    }

    @Override
    public void tick() {
        if (!nova.canNovaWork() || ++novaTaskTicks > 400) { stop(); return; }
        nova.clearWorkAnimation();
        if (this.targetDigPos == null) return;

        // Bảo hiểm: Nhỡ người chơi đập mất khối đất rồi
        if (!isDiggable(this.targetDigPos)) {
            this.targetDigPos = null;
            return;
        }

        // Nhìn chằm chằm vào khối đất
        this.nova.getLookControl().setLookAt(
            this.targetDigPos.getX() + 0.5, 
            this.targetDigPos.getY(), 
            this.targetDigPos.getZ() + 0.5, 
            10.0F, 
            this.nova.getMaxHeadXRot()
        );

        double distance = this.nova.blockPosition().distSqr(this.targetDigPos);
        
        // Khoảng cách <= 5.0 là vươn tay tới
        if (distance <= 5.0) {
            this.nova.getNavigation().stop();
            NovaWorkSupport.faceTarget(nova, targetDigPos);
            nova.playWorkAnimation(4);
            
            // Xúc đất: Cứ 5 ticks vung tay 1 lần
            if (this.actionTick % 5 == 0) { 
                this.nova.swing(InteractionHand.MAIN_HAND);
            }
            
            this.actionTick++;
            
            // Thực hiện hành động sau 15 ticks
            if (this.actionTick >= 15) {
                if (isDiggable(this.targetDigPos)) {
                    BlockState novaOriginalBlock = nova.level().getBlockState(this.targetDigPos);
                if (!NovaWorkSupport.breakBlock(nova, this.targetDigPos)) { stop(); return; }
                nova.addExperience(1);
                }  
                this.targetDigPos = null; // Xong việc, tìm ô khác!
                this.actionTick = 0; 
            }
        } else {
            // Chạy ra chỗ khối đất
            this.nova.getNavigation().moveTo(
                this.targetDigPos.getX() + 0.5, 
                this.targetDigPos.getY(), 
                this.targetDigPos.getZ() + 0.5, 
                this.speed
            );
        }
    }

    // QUÉT RADAR TÌM ĐẤT TRONG KHU VỰC 5x5 XUNG QUANH ĐIỂM NEO
    private BlockPos findNearestDigBlock() {
        if (this.anchorPos == null) return null;

        Optional<BlockPos> closestBlock = BlockPos.findClosestMatch(
            this.anchorPos, // Tìm quanh Điểm Neo thay vì quanh vị trí hiện tại của Nova
            2,  // Bán kính ngang 2 block (Tạo thành lưới 5x5)
            2,  // Quét sâu xuống mặt đất 2 block để đào bằng phẳng
            this::isDiggable
        );
        return closestBlock.orElse(null);
    }

    // ĐIỀU KIỆN 1: LÀ ĐẤT, CÁT, SỎI
    private boolean isDiggable(BlockPos pos) {
        if (!nova.isWithinWorkspace(pos)) return false;
        BlockState state = this.nova.level().getBlockState(pos);
        return state.is(Blocks.DIRT) 
            || state.is(Blocks.GRASS_BLOCK) 
            || state.is(Blocks.SAND) 
            || state.is(Blocks.GRAVEL)
            || state.is(Blocks.DIRT_PATH)
            || state.is(Blocks.COARSE_DIRT);
    }

    @Override public void stop() {
        targetDigPos = null;
        nova.clearWorkAnimation();
        nova.getNavigation().stop();
    }
}
