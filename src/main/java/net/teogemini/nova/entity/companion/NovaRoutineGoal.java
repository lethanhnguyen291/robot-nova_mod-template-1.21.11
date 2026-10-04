package net.teogemini.nova.entity.companion;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;

/** Safe return, sleep and shelter. It never changes the owner's follow/wait/sit choice. */
public final class NovaRoutineGoal extends Goal {
    private final NovaEntity nova;
    private BlockPos destination;
    private boolean rain;
    private int travelTicks;
    public NovaRoutineGoal(NovaEntity nova) {
        this.nova=nova; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        var c=nova.companion();
        if (!c.idleAllowed() || !c.routineReady() || nova.getCompanionState()!=NovaCompanion.NORMAL
                || nova.getWorkAction()!=NovaEntity.WORK_NONE || nova.getNavigation().isInProgress()) return false;
        rain=c.raining() && nova.getNovaMoveMode()==NovaEntity.MODE_FREE;
        if (!rain && !c.wantsHome()) return false;
        c.postponeRoutine();
        destination=null;
        if (rain) {
            // Sample at most 24 positions and evaluate at most four routes per search.
            int routes=0;
            for (int i=0;i<24 && routes<4;i++) {
                BlockPos pos=nova.blockPosition().offset(nova.getRandom().nextInt(17)-8,
                        nova.getRandom().nextInt(3)-1,nova.getRandom().nextInt(17)-8);
                if (!NovaCompanionPaths.safe(nova,pos) || nova.level().canSeeSky(pos)) continue;
                routes++;
                if (NovaCompanionPaths.path(nova,pos)!=null) { destination=pos; break; }
            }
            if (destination==null) {
                c.gesture("rain");
                c.notifyOwner(NovaCompanion.Notice.RAIN,"Mình chưa tìm được mái che gần đây.");
            }
        } else {
            BlockPos home=nova.getLinkedChest();
            if (home==null || !nova.level().hasChunkAt(home) || nova.distanceToSqr(Vec3.atCenterOf(home))>32*32
                    || !(nova.level().getBlockEntity(home) instanceof ChestBlockEntity)) return false;
            var owner=nova.getOwner();
            boolean ownerNear=owner!=null && owner.distanceToSqr(Vec3.atCenterOf(home))<9;
            if (!ownerNear && ChestBlockEntity.getOpenCount(nova.level(),home)==0
                    && NovaCompanionPaths.path(nova,home.above())!=null) destination=home.above();
            else destination=NovaCompanionPaths.beside(nova,home,false);
        }
        if (destination==null) {
            c.notifyOwner(NovaCompanion.Notice.PATH,"Mình chưa tìm được chỗ nghỉ có đường đi an toàn.");
            return false;
        }
        return true;
    }
    @Override public void start() {
        travelTicks=0; nova.clearWorkAnimation();
        nova.companion().state(rain ? NovaCompanion.RAIN : NovaCompanion.RETURNING);
        move();
    }
    private void move() {
        var path=NovaCompanionPaths.path(nova,destination);
        if (path==null) { nova.companion().stuck(destination); return; }
        nova.getNavigation().moveTo(path,.85);
    }
    @Override public boolean canContinueToUse() {
        var c=nova.companion();
        if (destination==null || !c.idleAllowed() || !nova.level().hasChunkAt(destination)
                || c.panicking() || nova.getCompanionState()==NovaCompanion.NORMAL
                || nova.getCompanionState()==NovaCompanion.HELP) return false;
        if (rain) return nova.getNovaMoveMode()==NovaEntity.MODE_FREE && nova.level().isRaining();
        if (!c.wantsHome()) return false;
        BlockPos home=nova.getLinkedChest();
        if (home==null || !nova.level().hasChunkAt(home)
                || !(nova.level().getBlockEntity(home) instanceof ChestBlockEntity)) return false;
        if (destination.equals(home.above())) {
            var owner=nova.getOwner();
            if (ChestBlockEntity.getOpenCount(nova.level(),home)>0
                    || owner!=null && owner.distanceToSqr(Vec3.atCenterOf(home))<6.25) return false;
        }
        return true;
    }
    @Override public void tick() {
        if (nova.distanceToSqr(Vec3.atBottomCenterOf(destination))<1.4) {
            nova.getNavigation().stop();
            nova.companion().state(rain ? NovaCompanion.RAIN : NovaCompanion.RESTING);
            if (!rain) nova.setInSittingPose(true);
            else if (nova.tickCount%100==0) nova.companion().gesture("inspect");
            return;
        }
        if (++travelTicks>400) { nova.companion().stuck(destination); return; }
        if (travelTicks%40==0) move();
        if (rain && nova.tickCount%80==0) nova.companion().gesture("rain");
    }
    @Override public void stop() {
        boolean onChest=destination!=null && nova.getLinkedChest()!=null
                && destination.equals(nova.getLinkedChest().above()) && nova.distanceToSqr(Vec3.atBottomCenterOf(destination))<2;
        nova.getNavigation().stop(); nova.setInSittingPose(false);
        if (!nova.companion().panicking() && nova.getCompanionState()!=NovaCompanion.HELP)
            nova.companion().state(NovaCompanion.NORMAL);
        nova.companion().postponeRoutine();
        if (onChest && !nova.isOrderedToSit() && !nova.companion().panicking()) {
            var beside=NovaCompanionPaths.beside(nova,nova.getLinkedChest(),false);
            if (beside!=null) nova.getNavigation().moveTo(beside.getX()+.5,beside.getY(),beside.getZ()+.5,.8);
        }
        destination=null;
    }
}
