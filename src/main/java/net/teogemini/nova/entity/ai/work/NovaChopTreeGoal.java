package net.teogemini.nova.entity.ai.work;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.entity.ai.tree.*;
import net.teogemini.nova.entity.companion.NovaCompanion;

/** Select from the shared index, reserve a whole tree, then fell only that snapshot. */
public class NovaChopTreeGoal extends Goal {
    private final NovaEntity nova;
    private final double speed;
    private NovaTreePlan plan;
    private BlockPos stand,tryingRoot;
    private Path route;
    private long nextTry;
    private int standAttempt,walkTicks,noProgress,replans,actionTicks,logIndex;
    private double previousDistance;
    private boolean finished;
    public NovaChopTreeGoal(NovaEntity nova,double speed) {
        this.nova=nova;this.speed=speed;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public boolean canUse() {
        if(!nova.canNovaWork() || nova.work().energy()<=100 || !nova.getMainHandItem().is(ItemTags.AXES)
                || !nova.companion().usableTool(nova.getMainHandItem()) || nova.trees().full())return false;
        long now=nova.level().getGameTime();if(now<nextTry)return false;
        nextTry=now+10+Math.floorMod(nova.getId(),11);
        NovaTreeService.request(nova);
        var memory=nova.trees();var level=(ServerLevel)nova.level();
        if(memory.active()!=null && (!memory.activeRoot().dimension().equals(level.dimension())
                || !NovaTreeService.area(nova,memory.active().root())))memory.cancel();
        var options=memory.active()!=null ? List.of(memory.active()) : NovaTreeService.candidates(nova);
        for(NovaTreePlan candidate:options) {
            if(memory.protectedTree(level.dimension(),candidate))continue;
            if(candidate.logs().stream().anyMatch(p->!NovaTreeService.inside(nova,p)))continue;
            var root=NovaTreeService.read(level,candidate.root());
            if(root==null)continue;
            if(memory.active()==null && !root.is(candidate.species().log)) {NovaTreeService.forget(level,candidate);continue;}
            if(nova.companion().retry.blocked(candidate.root(),now) || !NovaTreeService.claim(nova,candidate))continue;
            if(!candidate.root().equals(tryingRoot)){tryingRoot=candidate.root();standAttempt=0;}
            var stands=NovaTreeService.stands(nova,candidate.root());
            if(standAttempt>=stands.size()) {
                NovaTreeService.release(nova,candidate);nova.companion().retry.fail(candidate.root(),now);
                standAttempt=0;continue;
            }
            var target=stands.get(standAttempt);
            var result=NovaTreeService.route(nova,target);
            if(!result.attempted()){NovaTreeService.release(nova,candidate);return false;}
            if(result.path()==null){standAttempt++;NovaTreeService.release(nova,candidate);return false;}
            plan=candidate;stand=target;route=result.path();return true;
        }
        return false;
    }
    @Override public void start() {
        finished=false;walkTicks=noProgress=replans=actionTicks=logIndex=0;previousDistance=Double.MAX_VALUE;
        nova.trees().begin(GlobalPos.of(nova.level().dimension(),plan.root()),plan);
        nova.clearWorkAnimation();nova.getNavigation().moveTo(route,speed);
    }
    @Override public boolean canContinueToUse() {
        return !finished && plan!=null && nova.canNovaWork() && nova.work().energy()>100
                && nova.getMainHandItem().is(ItemTags.AXES) && nova.companion().usableTool(nova.getMainHandItem())
                && !nova.trees().protectedTree(nova.level().dimension(),plan)
                && NovaTreeService.inside(nova,plan.root());
    }
    @Override public void tick() {
        if(!canContinueToUse() || !NovaTreeService.claim(nova,plan)){finished=true;return;}
        double distance=nova.distanceToSqr(Vec3.atBottomCenterOf(stand));
        if(distance>1.5) {
            nova.clearWorkAnimation();walkTicks++;
            if(walkTicks%20==0) {
                noProgress=distance<previousDistance-.25 ? 0 : noProgress+20;previousDistance=distance;
                if(noProgress>=80 || nova.getNavigation().isDone()) {
                    var alternatives=NovaTreeService.stands(nova,plan.root());
                    var target=alternatives.get((standAttempt+replans+1)%alternatives.size());
                    var result=NovaTreeService.route(nova,target);
                    if(result.attempted()) {
                        replans++;noProgress=0;
                        if(result.path()!=null){stand=target;nova.getNavigation().moveTo(result.path(),speed);}
                    }
                }
            }
            if(walkTicks>600 || replans>=3){nova.companion().stuck(plan.root());finished=true;}
            return;
        }
        nova.getNavigation().stop();
        var rootState=NovaTreeService.read((ServerLevel)nova.level(),plan.root());
        if(rootState==null){finished=true;return;}
        if(actionTicks==0 && rootState.is(plan.species().log) && !NovaWorkSupport.canReach(nova,plan.root())) {
            // A reachable standing spot may still have an obstacle between the axe and trunk.
            // Try the next side before placing the whole tree on the retry cooldown.
            standAttempt++;finished=true;return;
        }
        NovaWorkSupport.faceTarget(nova,plan.root());nova.playWorkAnimation(NovaEntity.WORK_CHOP);
        nova.getLookControl().setLookAt(plan.root().getX()+.5,plan.root().getY()+.5,plan.root().getZ()+.5);
        if(++actionTicks%10==0)nova.swing(InteractionHand.MAIN_HAND);
        if(actionTicks<40 || actionTicks%20!=0)return;
        // Skipping stale snapshot entries is bounded, and never awards drops or experience.
        int checked=0;
        while(logIndex<plan.logs().size() && checked++<8) {
            var result=NovaTreeHarvest.fell(nova,plan,plan.logs().get(logIndex));
            if(result==NovaTreeHarvest.Result.STOP){finished=true;nextTry=nova.level().getGameTime()+100;return;}
            logIndex++;
            if(result==NovaTreeHarvest.Result.BROKEN)break;
        }
        if(logIndex>=plan.logs().size()) {
            nova.trees().complete();NovaTreeService.forget((ServerLevel)nova.level(),plan);
            nova.companion().gesture("proud");finished=true;
        }
    }
    @Override public void stop() {
        NovaTreeService.release(nova,plan);plan=null;route=null;
        nova.clearWorkAnimation();nova.getNavigation().stop();
    }
}
