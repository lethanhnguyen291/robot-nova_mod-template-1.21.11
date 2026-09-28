package net.teogemini.nova.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.teogemini.nova.entity.NovaEntity;

public class NovaFollowOwnerGoal extends FollowOwnerGoal {
    private final NovaEntity nova;

    public NovaFollowOwnerGoal(NovaEntity nova) {
        super(nova, 1.0, 5.0F, 2.0F);
        this.nova = nova;
    }

    @Override
    public boolean canUse() {
                return nova.getNovaMoveMode() == NovaEntity.MODE_FOLLOW
                && !nova.isOrderedToSit()
                && !nova.isPerformingAction() && !nova.isRallying()
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
                return nova.getNovaMoveMode() == NovaEntity.MODE_FOLLOW
                && !nova.isOrderedToSit()
                && !nova.isPerformingAction() && !nova.isRallying()
                && super.canContinueToUse();
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity owner = nova.getOwner();
        if (owner == null || nova.getNavigation().isDone()) {
            nova.setNovaRunning(false);
            return;
        }

        double distanceSquared = nova.distanceToSqr(owner);

        // Dùng hai ngưỡng để tránh đổi đi/chạy liên tục.
        boolean running = nova.isNovaRunning()
                ? distanceSquared > 4.5 * 4.5
                : distanceSquared > 7.0 * 7.0;

        nova.setNovaRunning(running);
        nova.getNavigation().setSpeedModifier(running ? 1.45 : 1.0);
    }

    @Override
    public void stop() {
        super.stop();
        nova.setNovaRunning(false);
    }
}