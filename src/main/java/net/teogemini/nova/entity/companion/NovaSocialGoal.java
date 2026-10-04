package net.teogemini.nova.entity.companion;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;

/** Short, optional moments. Work and explicit commands have higher Goal priorities. */
public final class NovaSocialGoal extends Goal {
    private final NovaEntity nova;
    private int ticks, kind;
    private BlockPos target;
    private NovaEntity friend;
    private ItemEntity flower;
    public NovaSocialGoal(NovaEntity nova) {
        this.nova=nova; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        var c=nova.companion(); var owner=nova.getOwner();
        if (!c.idleAllowed() || !c.socialReady() || nova.getCompanionState()!=NovaCompanion.NORMAL
                || nova.getWorkAction()!=NovaEntity.WORK_NONE || nova.getNavigation().isInProgress()
                || owner==null || !owner.isAlive() || owner.isSpectator() || nova.distanceToSqr(owner)>36
                || !nova.hasLineOfSight(owner) || owner.getDeltaMovement().horizontalDistanceSqr()>.005
                || nova.work().energy()<=100) return false;
        c.postponeSocial();
        target=null; friend=null; flower=null;
        kind=c.completionPending() ? 0 : nova.getRandom().nextInt(5)+1;
        if (kind==1) {
            // Stand beside the owner, rather than stepping into their line of travel.
            double yaw=Math.toRadians(owner.getYRot());
            target=BlockPos.containing(owner.position().add(Math.cos(yaw)*1.8,0,Math.sin(yaw)*1.8));
            if (NovaCompanionPaths.path(nova,target)==null) return false;
        } else if (kind==2) {
            var robots=nova.level().getEntitiesOfClass(NovaEntity.class,nova.getBoundingBox().inflate(4),
                    other -> other!=nova && other.isAlive() && other.isTame()
                            && java.util.Objects.equals(other.getOwnerReference(),nova.getOwnerReference())
                            && other.getWorkAction()==NovaEntity.WORK_NONE && nova.hasLineOfSight(other));
            if (!robots.isEmpty()) friend=robots.getFirst();
        } else if (kind==3) {
            for (var allowed:c.memories().buttons()) {
                if (!allowed.dimension().equals(nova.level().dimension()) || !nova.level().hasChunkAt(allowed.pos())
                        || nova.distanceToSqr(Vec3.atCenterOf(allowed.pos()))>9) continue;
                if (nova.level().getBlockState(allowed.pos()).getBlock() instanceof ButtonBlock) { target=allowed.pos(); break; }
            }
            // Unapproved switches are only observed; never activated.
            if (target==null) kind=5;
        } else if (kind==4 && c.memories().bond()>=20 && nova.getRandom().nextInt(4)==0) {
            var drops=nova.level().getEntitiesOfClass(ItemEntity.class,nova.getBoundingBox().inflate(2),
                    item -> item.isAlive() && !item.hasPickUpDelay()
                            && item.getItem().is(net.minecraft.tags.ItemTags.FLOWERS)
                            && giftable(item) && nova.hasLineOfSight(item));
            if (!drops.isEmpty()) flower=drops.getFirst();
        }
        return true;
    }
    @Override public void start() {
        ticks=0; nova.companion().state(NovaCompanion.SOCIAL);
        if (kind==1) {
            var path=NovaCompanionPaths.path(nova,target);
            if (path!=null) nova.getNavigation().moveTo(path,.65);
        } else nova.getNavigation().stop();
        nova.companion().gesture(kind==0 ? "proud" : "inspect");
        if (kind==0) { nova.companion().reportCompletion(); nova.showNovaFace(5,60); }
    }
    @Override public boolean canContinueToUse() {
        var owner=nova.getOwner();
        return ticks<100 && nova.companion().idleAllowed() && nova.getCompanionState()==NovaCompanion.SOCIAL
                && owner!=null && owner.isAlive() && !owner.isSpectator() && nova.distanceToSqr(owner)<49
                && owner.getDeltaMovement().horizontalDistanceSqr()<.015;
    }
    @Override public void tick() {
        ticks++; var owner=nova.getOwner(); if (owner==null) return;
        if (kind==1 && ticks>30 && nova.getNavigation().isDone()) {
            Vec3 look=owner.getEyePosition().add(owner.getLookAngle().scale(4));
            nova.getLookControl().setLookAt(look.x,look.y,look.z,6,6);
            if (owner.isCrouching() || nova.companion().memories().personality()==NovaPersonality.CALM)
                nova.setInSittingPose(true);
        } else if (friend!=null && friend.isAlive()) {
            nova.getLookControl().setLookAt(friend,6,6);
            if (ticks==20) { nova.companion().gesture("nod"); friend.showNovaFace(0,40); }
        } else if (target!=null && kind==3) {
            nova.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,6,6);
            if (ticks==40 && nova.companion().memories().buttons().contains(
                    net.minecraft.core.GlobalPos.of(nova.level().dimension(),target))
                    && nova.level() instanceof net.minecraft.server.level.ServerLevel level
                    && level.mayInteract(owner,target)
                    && net.teogemini.nova.entity.NovaWorkSupport.canReach(nova,target)) {
                var state=level.getBlockState(target);
                if (state.getBlock() instanceof ButtonBlock button && !state.getValue(ButtonBlock.POWERED)) {
                    nova.companion().gesture("point"); button.press(state,level,target,null);
                }
            }
        } else {
            nova.getLookControl().setLookAt(owner,6,6);
            if (ticks==40 && flower!=null && flower.isAlive() && !flower.hasPickUpDelay()
                    && giftable(flower) && nova.distanceToSqr(flower)<4 && nova.distanceToSqr(owner)<9) {
                // Move a single existing dropped flower. No creation or removal from chests.
                var gift=flower.getItem().copyWithCount(1);
                ItemEntity drop=new ItemEntity(nova.level(),owner.getX(),owner.getY()+.2,owner.getZ(),gift);
                drop.setTarget(owner.getUUID()); drop.setThrower(nova); drop.setPickUpDelay(20);
                if (!nova.level().addFreshEntity(drop)) { flower=null; return; }
                flower.getItem().shrink(1);
                if (flower.getItem().isEmpty()) flower.discard();
                nova.companion().memories().remember("Tặng chủ nhân một bông hoa mình tìm thấy.");
                nova.companion().gesture("proud"); flower=null;
            }
        }
    }
    private boolean giftable(ItemEntity item) {
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                net.minecraft.util.ProblemReporter.DISCARDING,nova.level().registryAccess());
        item.saveWithoutId(output);
        var saved=output.buildResult();
        return !saved.contains("Owner") && !saved.contains("Thrower");
    }
    @Override public void stop() {
        nova.getNavigation().stop(); nova.setInSittingPose(false);
        if (nova.getCompanionState()==NovaCompanion.SOCIAL) nova.companion().state(NovaCompanion.NORMAL);
        nova.companion().postponeSocial();
        target=null; friend=null; flower=null;
    }
}
