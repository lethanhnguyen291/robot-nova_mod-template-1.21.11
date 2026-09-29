package net.teogemini.nova.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.teogemini.nova.entity.NovaEntity;

public class NovaLookAtOwnerGoal extends Goal {
    private final NovaEntity nova;
    private LivingEntity target;

    public NovaLookAtOwnerGoal(NovaEntity nova) {
        this.nova = nova;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Không can thiệp nếu đang thực hiện động tác đặc biệt hoặc animation làm việc
        if (nova.isPerformingAction() || nova.getWorkAction() != NovaEntity.WORK_NONE) {
            return false;
        }

        // Ưu tiên nhìn theo chủ nhân (kể cả khi chủ nhân bay lên trời cao đến 32 block)
        LivingEntity owner = nova.getOwner();
        if (owner != null && owner.isAlive()) {
            if (nova.distanceToSqr(owner) <= 32.0 * 32.0) {
                this.target = owner;
                return true;
            }
        }

        // Nếu chưa thuần hóa hoặc chủ đi xa, tìm người chơi gần nhất trong 16 block
        Player player = nova.level().getNearestPlayer(nova, 16.0);
        if (player != null && player.isAlive()) {
            this.target = player;
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }
        if (nova.isPerformingAction() || nova.getWorkAction() != NovaEntity.WORK_NONE) {
            return false;
        }
        return nova.distanceToSqr(this.target) <= 32.0 * 32.0;
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target != null) {
            // Nhìn thẳng vào tầm mắt (EyeY) của người chơi với góc ngước/cúi lên tới 85 độ
            this.nova.getLookControl().setLookAt(
                this.target.getX(),
                this.target.getEyeY(),
                this.target.getZ(),
                30.0F,
                85.0F
            );
        }
    }
}