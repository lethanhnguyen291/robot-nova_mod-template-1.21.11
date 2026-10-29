package net.teogemini.nova.entity.companion;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
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
    private boolean fetchingTool;
    private boolean fetchingTorch;
    public NovaStorageGoal(NovaEntity nova) {
        this.nova=nova; setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (!nova.canStartNovaJob() || nova.isSwordNova() || nova.level().getGameTime()<nextSearch) return false;
        nextSearch=nova.level().getGameTime()+60+Math.floorMod(nova.getId(),20);
        if (nova.getPrimaryTool().is(ItemTags.HOES) && nova.getOffhandItem().isEmpty()) {
            NovaWorkSupport.ensureOffhandSeed(nova);
        }
        int filled=0;
        boolean hasDepositable=false;
        for (int i=0;i<nova.getInventory().getContainerSize();i++) {
            ItemStack item = nova.getInventory().getItem(i);
            if (!item.isEmpty()) {
                filled++;
                if (!shouldKeepInBackpack(item)) hasDepositable=true;
            }
        }
        depositing=filled>=nova.getInventory().getContainerSize()-2 && hasDepositable;

        ItemStack held = nova.getMainHandItem();
        fetchingTool = NovaWorkSupport.isTool(held)
                && !nova.companion().usableTool(held)
                && !NovaWorkSupport.hasUsableToolOnBody(nova, held);

        fetchingTorch = NovaWorkSupport.needsTorches(nova);

        boolean farmingSeeds = nova.getPrimaryTool().is(ItemTags.HOES) && nova.getOffhandItem().isEmpty();

        if (!depositing && !fetchingTool && !fetchingTorch && !farmingSeeds) return false;

        target=depositing ? NovaChestAccess.depositTarget(nova) : nova.getLinkedChest();
        if (target==null) {
            if (depositing) nova.companion().storageBlocked();
            return false;
        }
        if (nova.companion().retry.blocked(target,nova.level().getGameTime())) return false;
        Container chest=NovaChestAccess.container(nova,target);
        if (chest==null || NovaChestAccess.busy(nova,target)) return false;
        if (!depositing && !fetchingTool && !fetchingTorch && farmingSeeds && seed(chest)<0) return false;
        standAt=NovaCompanionPaths.beside(nova,target,false);
        if (standAt==null) { nova.companion().stuck(target); return false; }
        return true;
    }
    @Override public void start() { nova.pathClearing().track(target); ticks=0; inspectTicks=0; opened=null; move(); }
    private void move() {
        var path=NovaCompanionPaths.path(nova,standAt);
        if (path==null) { nova.companion().stuck(target); return; }
        nova.getNavigation().moveTo(path,1);
    }
    @Override public boolean canContinueToUse() {
        return nova.canStartNovaJob() && target!=null && ticks<500
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
        if (++inspectTicks<60) return;
        int moved=0;
        if (depositing) {
            if (nova.getPrimaryTool().is(ItemTags.HOES)) {
                NovaWorkSupport.ensureOffhandSeed(nova);
            } else if (nova.getPrimaryTool().is(ItemTags.PICKAXES)) {
                NovaWorkSupport.ensureOffhandTorch(nova);
            } else if (nova.getPrimaryTool().is(ItemTags.AXES)) {
                NovaWorkSupport.autoSelectOffhandSapling(nova);
            }
            for (int i=0;i<nova.getInventory().getContainerSize();i++) {
                ItemStack stack = nova.getInventory().getItem(i);
                if (shouldKeepInBackpack(stack)) continue;
                moved+=NovaChestAccess.transfer(chest,stack);
            }
            nova.getInventory().setChanged();
            nova.companion().delivered(moved);
        }

        // Lấy công cụ mới từ rương nếu công cụ trên tay sắp hết độ bền
        ItemStack held = nova.getMainHandItem();
        if (fetchingTool || (NovaWorkSupport.isTool(held) && !nova.companion().usableTool(held))) {
            int toolSlot = -1;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (!stack.isEmpty() && NovaWorkSupport.isSameToolType(held, stack) && nova.companion().usableTool(stack)) {
                    toolSlot = i;
                    break;
                }
            }
            if (toolSlot >= 0) {
                ItemStack newTool = chest.removeItem(toolSlot, 1);
                if (!held.isEmpty()) {
                    NovaChestAccess.transfer(chest, held);
                }
                nova.setItemSlot(EquipmentSlot.MAINHAND, newTool);
                nova.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                chest.setChanged();
                moved++;
                nova.companion().gesture("nod");
            } else {
                String toolName = NovaWorkSupport.getToolTypeName(held);
                nova.companion().notifyOwner(NovaCompanion.Notice.TOOL,
                        "Công cụ " + toolName + " của mình đã hết độ bền và trong rương cũng không còn, bạn bổ sung vào rương giúp mình nhé.");
                nova.companion().retry.fail(target, nova.level().getGameTime() + 600);
            }
        }

        // Lấy đuốc từ rương nếu hết đuốc
        if (fetchingTorch || (nova.getMainHandItem().is(ItemTags.PICKAXES) && !NovaWorkSupport.hasTorchesOnBody(nova))) {
            int torchSlot = -1;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (!stack.isEmpty() && stack.is(Items.TORCH)) {
                    torchSlot = i;
                    break;
                }
            }
            if (torchSlot >= 0) {
                ItemStack torches = chest.removeItem(torchSlot, 64);
                if (nova.getOffhandItem().isEmpty() || nova.getOffhandItem().is(Items.TORCH)) {
                    nova.setItemSlot(EquipmentSlot.OFFHAND, torches);
                    nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                } else {
                    nova.getInventory().addItem(torches);
                    NovaWorkSupport.ensureOffhandTorch(nova);
                }
                chest.setChanged();
                moved++;
                nova.companion().gesture("nod");
            } else {
                nova.companion().notifyOwner(NovaCompanion.Notice.TORCH,
                        "Mình đã hết đuốc và trong rương cũng không còn đuốc, bạn bổ sung đuốc vào rương giúp mình nhé.");
                nova.companion().retry.fail(target, nova.level().getGameTime() + 600);
            }
        }

        // Lấy hạt giống cho nông dân
        if (nova.getOffhandItem().isEmpty()) {
            int index=seed(chest);
            if (index>=0) {
                var supply=chest.removeItem(index,32); moved+=supply.getCount();
                nova.setItemSlot(EquipmentSlot.OFFHAND,supply); nova.setGuaranteedDrop(EquipmentSlot.OFFHAND);
                chest.setChanged();
            }
        }
        if (moved>0) nova.companion().retry.success(target);
        else nova.companion().retry.fail(target,nova.level().getGameTime());
        stop();
    }
    private boolean shouldKeepInBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack primary = nova.getPrimaryTool();
        // Giữ lại công cụ dự phòng còn dùng tốt trong ba lô
        if (NovaWorkSupport.isTool(stack) && nova.companion().usableTool(stack)) {
            return true;
        }
        // Thợ mỏ giữ đuốc trong ba lô để cắm dưới hầm
        if (primary.is(ItemTags.PICKAXES) && stack.is(Items.TORCH)) {
            return true;
        }
        // Tiều phu giữ mầm cây nếu còn vị trí cây cần trồng lại
        if (primary.is(ItemTags.AXES) && stack.is(ItemTags.SAPLINGS) && !nova.trees().planting().isEmpty()) {
            return true;
        }
        return false;
    }

    private static int seed(Container chest) {
        for (int i=0;i<chest.getContainerSize();i++) {
            var stack=chest.getItem(i);
            if (stack.is(Items.WHEAT_SEEDS)||stack.is(Items.BEETROOT_SEEDS)||stack.is(Items.CARROT)||stack.is(Items.POTATO)) return i;
        }
        return -1;
    }
    @Override public void stop() { nova.pathClearing().track(null);
        if (opened!=null) { opened.stopOpen(nova); opened=null; }
        nova.companion().closeChest();
        target=null; standAt=null; nova.clearWorkAnimation(); nova.getNavigation().stop();
        fetchingTool = false; fetchingTorch = false;
        nextSearch=nova.level().getGameTime()+80;
    }
}
