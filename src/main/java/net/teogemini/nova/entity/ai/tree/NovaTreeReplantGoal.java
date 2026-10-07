package net.teogemini.nova.entity.ai.tree;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaExperienceManager;
import net.teogemini.nova.entity.NovaWorkSupport;
import net.teogemini.nova.entity.companion.NovaCompanion;

/** Persistent planting jobs; a 2x2 tree is never intentionally replanted as a single sapling. */
public final class NovaTreeReplantGoal extends Goal {
    private final NovaEntity nova;
    private NovaTreeMemory.Planting job;
    private Path path;
    private BlockPos stand;
    private long nextTry;
    private int ticks,action,standIndex;
    private boolean done;
    public NovaTreeReplantGoal(NovaEntity nova) { this.nova=nova;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    private int supply(NovaTreeMemory.Planting job) {
        int count=nova.getOffhandItem().is(job.species().sapling) ? nova.getOffhandItem().getCount() : 0;
        for(var stack:nova.getInventory().getItems()) if(stack.is(job.species().sapling))count+=stack.getCount();
        return count;
    }
    private int missing(NovaTreeMemory.Planting job) {
        if(!(job.species().sapling instanceof BlockItem item))return -1;
        var planted=item.getBlock().defaultBlockState();int count=0;
        for(var pos:job.positions()) {
            var current=NovaTreeService.read((ServerLevel)nova.level(),pos);
            if(current==null || !NovaTreeService.inside(nova,pos))return -1;
            if(current.is(item.getBlock()))continue;
            if(!current.isAir() || !planted.canSurvive(nova.level(),pos))return -1;
            count++;
        }
        return count;
    }
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public boolean canUse() {
        if(!nova.canNovaWork() || nova.level().getGameTime()<nextTry)return false;
        nextTry=nova.level().getGameTime()+40+Math.floorMod(nova.getId(),17);
        for(var pending:nova.trees().planting()) {
            if(!pending.root().dimension().equals(nova.level().dimension())
                    || nova.companion().retry.blocked(pending.root().pos(),nova.level().getGameTime()))continue;
            int missing=missing(pending);
            if(missing==0){nova.trees().planted(pending);continue;}
            if(missing<0) {
                nova.companion().notifyOwner(NovaCompanion.Notice.PATH,"Mình còn chỗ cần trồng lại nhưng nền hoặc khoảng trống chưa phù hợp.");
                continue;
            }
            if(supply(pending)<missing) {
                nova.companion().notifyOwner(NovaCompanion.Notice.TOOL,"Mình đã nhớ chỗ trồng lại. Cần đủ "+missing+" cây giống đúng loại trong ba lô hoặc tay phụ.");
                continue;
            }
            var options=NovaTreeService.stands(nova,pending.root().pos());
            BlockPos target=options.get(standIndex%options.size());
            var result=NovaTreeService.route(nova,target);
            if(!result.attempted())return false;
            if(result.path()==null){standIndex++;if(standIndex%options.size()==0)nova.companion().retry.fail(pending.root().pos(),nova.level().getGameTime());return false;}
            job=pending;path=result.path();stand=target;return true;
        }
        return false;
    }
    @Override public void start(){ticks=action=0;done=false;nova.clearWorkAnimation();nova.getNavigation().moveTo(path,1);}
    @Override public boolean canContinueToUse(){return !done && job!=null && nova.canNovaWork() && ticks<400 && missing(job)>=0;}
    private void consume() {
        if(nova.getOffhandItem().is(job.species().sapling)){nova.getOffhandItem().shrink(1);return;}
        for(int i=0;i<nova.getInventory().getContainerSize();i++) if(nova.getInventory().getItem(i).is(job.species().sapling)) {
            nova.getInventory().removeItem(i,1);return;
        }
    }
    @Override public void tick() {
        ticks++;
        if(!canContinueToUse()){done=true;return;}
        if(nova.distanceToSqr(Vec3.atBottomCenterOf(stand))>1.5) {
            if(nova.getNavigation().isDone() || ticks>=380){nova.companion().retry.fail(job.root().pos(),nova.level().getGameTime());done=true;}
            return;
        }
        nova.getNavigation().stop();
        int needed=missing(job);
        if(needed==0){nova.trees().planted(job);done=true;return;}
        if(needed<0 || supply(job)<needed){done=true;return;}
        for(var pos:job.positions()) if(nova.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>12.25
                || !NovaWorkSupport.canReach(nova,pos)){done=true;standIndex++;return;}
        NovaWorkSupport.faceTarget(nova,job.root().pos());nova.playWorkAnimation(NovaEntity.WORK_PLANT);
        if(++action<20)return;
        var block=((BlockItem)job.species().sapling).getBlock();
        for(var pos:job.positions()) {
            if(nova.level().getBlockState(pos).is(block))continue;
            // Charge only successfully placed saplings, even if a later placement is refused.
            if(!nova.level().getBlockState(pos).isAir() || !block.defaultBlockState().canSurvive(nova.level(),pos))break;
            if(!nova.level().setBlockAndUpdate(pos,block.defaultBlockState()))break;
            consume();nova.companion().planted();NovaExperienceManager.awardFarmerExp(nova,false);
        }
        nova.swing(InteractionHand.OFF_HAND);
        if(missing(job)==0)nova.trees().planted(job);
        done=true;
    }
    @Override public void stop(){job=null;path=null;nova.clearWorkAnimation();nova.getNavigation().stop();}
}
