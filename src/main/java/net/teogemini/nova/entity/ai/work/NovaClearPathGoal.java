package net.teogemini.nova.entity.ai.work;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.teogemini.nova.entity.NovaEntity;

/** Work rescue has priority over jobs, but yields to danger, waiting, rally and follow. */
public final class NovaClearPathGoal extends Goal {
    private final NovaEntity nova;
    public NovaClearPathGoal(NovaEntity nova) {
        this.nova = nova;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() { return nova.pathClearing().busy() && nova.pathClearing().allowed(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { nova.pathClearing().start(); }
    @Override public void tick() { nova.pathClearing().tick(); }
    @Override public void stop() { nova.pathClearing().cancel(); }
}
