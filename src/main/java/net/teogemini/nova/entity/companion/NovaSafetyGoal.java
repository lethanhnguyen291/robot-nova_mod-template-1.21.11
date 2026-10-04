package net.teogemini.nova.entity.companion;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.teogemini.nova.entity.NovaEntity;

public final class NovaSafetyGoal extends Goal {
    private final NovaEntity nova;
    private int delay;
    public NovaSafetyGoal(NovaEntity nova) {
        this.nova=nova; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() { return nova.isAlive() && !nova.isNoAi() && nova.companion().panicking(); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { delay=0; nova.setInSittingPose(false); nova.setNovaRunning(true); }
    @Override public void tick() {
        nova.showNovaFace(32,20);
        if (--delay>0) return;
        delay=40;
        for (int i=0;i<3;i++) {
            var away=DefaultRandomPos.getPosAway(nova,8,2,nova.companion().danger());
            if (away==null) continue;
            var path=NovaCompanionPaths.path(nova,BlockPos.containing(away));
            if (path!=null) { nova.getNavigation().moveTo(path,1.3); return; }
        }
        nova.companion().notifyOwner(NovaCompanion.Notice.PATH,"Mình sợ quá, chỗ này không có đường chạy an toàn!");
    }
    @Override public void stop() { nova.getNavigation().stop(); nova.setNovaRunning(false); }
}
