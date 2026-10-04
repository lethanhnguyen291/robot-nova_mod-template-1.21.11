package net.teogemini.nova.entity.companion;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;

/** Replaces the registered logistics Goal; the old class remains for compatibility helpers. */
public final class NovaStorageGoal extends Goal {
    private final NovaEntity nova;
    private BlockPos target, standAt;
    private Container opened;
    private long nextSearch;
    private int ticks, inspectTicks;
    private boolean depositing;
    public NovaStorageGoal(NovaEntity nova) {
        this.nova=nova; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (!nova.canNovaWork() || nova.level().getGameTime()<nextSearch) return false;
        nextSearch=nova.level().getGameTime()+80+Math.floorMod(nova.getId(),20);
        int filled=0;
        for (int i=0;i<nova.getInventory().getContainerSize();i++) if (!nova.getInventory().getItem(i).isEmpty()) filled++;
        depositing=filled>=nova.getInventory().getContainerSize()-2;
        target=depositing ? NovaChestAccess.depositTarget(nova) : nova.getLinkedChest();
        if (target==null) {
            if (depositing) nova.companion().storageBlocked();
            return false;
        }
        if (nova.companion().retry.blocked(target,nova.level().getGameTime())) return false;
        Container chest=NovaChestAccess.container(nova,target);
        if (chest==null || NovaChestAccess.busy(nova,target)) return false;
        if (!depositing && (!nova.getMainHandItem().is(ItemTags.HOES) || !nova.getOffhandItem().isEmpty() || seed(chest)<0)) return false;
        standAt=NovaCompanionPaths.beside(nova,target,false);
        if (standAt==null) { nova.companion().retry.fail(target,nova.level().getGameTime()); return false; }
        return true;
    }
    @Override public void start() { ticks=0; inspectTicks=0; opened=null; move(); }
    private void move() {
        var path=NovaCompanionPaths.path(nova,standAt);
        if (path==null) { nova.companion().stuck(target); return; }
        nova.getNavigation().moveTo(path,1);
    }
    @Override public boolean canContinueToUse() {
        return nova.canNovaWork() && target!=null && ticks<500
                && NovaChestAccess.container(nova,target)!=null && !NovaChestAccess.busy(nova,target);
    }
    @Override public void tick() {
        if (!canContinueToUse()) { stop(); return; }
        ticks++;
        nova.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5);
        if (nova.distanceToSqr(Vec3.atCenterOf(target))>6.25 || !NovaWorkSupport.canReach(nova,target)) {
            if (opened!=null) { stop(); return; }
            if (ticks>350) { nova.companion().stuck(target); stop(); return; }
            if (ticks%40==0) move();
            return;
        }
        nova.getNavigation().stop();
        Container chest=NovaChestAccess.container(nova,target);
        if (chest==null || NovaChestAccess.busy(nova,target)) { stop(); return; }
        if (opened==null) {
            opened=chest; nova.companion().openChest(target); opened.startOpen(nova);
        }
        NovaWorkSupport.faceTarget(nova,target);
        nova.playWorkAnimation(NovaEntity.WORK_CHEST);
        if (++inspectTicks<100) return;
        int moved=0;
        if (depositing) {
            for (int i=0;i<nova.getInventory().getContainerSize();i++)
                moved+=NovaChestAccess.transfer(chest,nova.getInventory().getItem(i));
            nova.getInventory().setChanged();
            nova.companion().delivered(moved);
        } else if (nova.getOffhandItem().isEmpty()) {
            int index=seed(chest);
            if (index>=0) {
                var supply=chest.removeItem(index,32); moved=supply.getCount();
                nova.setItemSlot(EquipmentSlot.OFFHAND,supply); nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                chest.setChanged();
            }
        }
        if (moved>0) nova.companion().retry.success(target);
        else nova.companion().retry.fail(target,nova.level().getGameTime());
        stop();
    }
    private static int seed(Container chest) {
        for (int i=0;i<chest.getContainerSize();i++) {
            var stack=chest.getItem(i);
            if (stack.is(Items.WHEAT_SEEDS)||stack.is(Items.BEETROOT_SEEDS)||stack.is(Items.CARROT)||stack.is(Items.POTATO)) return i;
        }
        return -1;
    }
    @Override public void stop() {
        if (opened!=null) { opened.stopOpen(nova); opened=null; }
        nova.companion().closeChest();
        target=null; standAt=null; nova.clearWorkAnimation(); nova.getNavigation().stop();
        nextSearch=nova.level().getGameTime()+80;
    }
}
