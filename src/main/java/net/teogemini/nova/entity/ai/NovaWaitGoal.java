package net.teogemini.nova.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.teogemini.nova.entity.NovaEntity;

public class NovaWaitGoal extends Goal {
    private final NovaEntity nova;

    public NovaWaitGoal(NovaEntity nova) {
        this.nova = nova;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
               return nova.isTame() && !nova.isRallying()
                && (nova.isOrderedToSit() || nova.isPerformingAction()
                    || nova.getNovaMoveMode() == NovaEntity.MODE_WAIT);
    }

    @Override
    public boolean canContinueToUse() {
                return nova.isTame() && !nova.isRallying()
                && (nova.isOrderedToSit()
                    || nova.isPerformingAction()
                    || nova.getNovaMoveMode() == NovaEntity.MODE_WAIT);
    }

    @Override
    public void start() {
        nova.getNavigation().stop();
    }
}
