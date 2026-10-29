package net.teogemini.nova.entity.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaChestHandler;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;

public class NovaFetchChestGoal extends Goal {
    private final NovaEntity nova;
    private int ticks;

    public NovaFetchChestGoal(NovaEntity nova) {
        this.nova = nova;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public boolean canUse() {
        return nova.isAlive() && nova.isTame() && !nova.isCarryingChest() && nova.isFetchingChest();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && ticks < 600;
    }

    @Override
    public void start() {
        ticks = 0;
        BlockPos target = nova.getFetchingChestTarget();
        if (target != null) {
            nova.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.25);
        }
    }

    @Override
    public void tick() {
        ticks++;
        BlockPos target = nova.getFetchingChestTarget();
        if (target == null || !(nova.level() instanceof ServerLevel level)) {
            nova.stopFetchingChest();
            return;
        }

        nova.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        double distSqr = nova.distanceToSqr(Vec3.atCenterOf(target));
        if (distSqr <= 9.0) {
            nova.getNavigation().stop();
            if (NovaChestHandler.pickUp(nova, level, target)) {
                if (nova.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
                    NovaWorkSupport.message(nova, player, "Đã nhấc rương xong! Mình sẽ đi theo bạn.");
                }
            } else {
                if (nova.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
                    NovaWorkSupport.message(nova, player, "Không thể nhấc rương (rương có thể đang mở hoặc là rương đôi).");
                }
            }
            nova.stopFetchingChest();
            return;
        }

        if (ticks % 20 == 0) {
            nova.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.25);
        }
    }

    @Override
    public void stop() {
        ticks = 0;
        nova.getNavigation().stop();
    }
}
